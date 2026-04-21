package com.example.demoapp;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
public class SomeController {
    private static final String TARGET_FOLDER = "img/";

//    @CrossOrigin(origins = "http://localhost:5173")
    @CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://192.168.43.252:5173"})
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile imageFile, HttpSession session) throws InterruptedException {


        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body("未接收到文件");
        }

        try {
            String originalFileName = imageFile.getOriginalFilename();
            String extension = getFileExtension(originalFileName);
            String storedFileName = UUID.randomUUID() + extension;
            Path destinationFilePath = Paths.get(TARGET_FOLDER, storedFileName);

            // 确保目录存在
            Files.createDirectories(destinationFilePath.getParent());

            // 保存文件
            imageFile.transferTo(destinationFilePath);

            Main mn = new Main(storedFileName);
            mn.setU_model_name(resolveCurrentModel(session));
            String analysisResult = mn.runApp();


            // 返回文件的访问URL或者其他相关信息
//            return ResponseEntity.ok("文件上传成功，保存路径：" + destinationFilePath.toString());

            //return ResponseEntity.ok(analysisResult);  // 返回识别结果到前端

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", "/" + storedFileName);
            response.put("resultText", analysisResult);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body("文件保存失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件保存失败：" + e.getMessage());
        }

    }

    private String resolveCurrentModel(HttpSession session) {
        Object currentModel = session.getAttribute("currentModel");
        if (currentModel instanceof String modelName && !modelName.isBlank()) {
            return modelName;
        }
        return Main.DEFAULT_MODEL_NAME;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

}
