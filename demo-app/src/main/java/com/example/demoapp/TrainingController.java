package com.example.demoapp;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/training")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://192.168.43.252:5173"})
public class TrainingController {
    private static final String CURRENT_MODEL_SESSION_KEY = "currentModel";

    @GetMapping("/modelInfo")
    public String getModelInfo(@RequestParam String modelName) {
        TrainingConfig config = trainingConfigRepository.findBySaveModel(modelName);
        if (config != null) {
            return "模型名称: " + config.getSaveModel() + "\n"
                    + "Batch Size: " + config.getBatchSize() + "\n"
                    + "Learning Rate: " + config.getLearningRate() + "\n"
                    + "Momentum: " + config.getMomentum() + "\n"
                    + "Epochs: " + config.getEpochs() + "\n"
                    + "Stride: " + config.getStride() + "\n"
                    + "Padding: " + config.getPadding() + "\n"
                    + "Kernel Size: " + config.getKernelSize() + "\n"
                    + "Out Channel: " + config.getOutChannel() + "\n"
                    + "In Channel: " + config.getInChannel() + "\n"
                    + "Training Result: " + config.getTrainingResult();
        } else {
            return "未找到指定的模型信息。";
        }
    }

    @PostMapping("/train")
    public ResponseEntity<String> trainModel(@RequestBody Map<String, String> params, HttpSession session) {
        try {
            // 从请求参数中提取各个模型训练参数
            int batchSize = Integer.parseInt(params.get("batchSize"));
            float learningRate = Float.parseFloat(params.get("learningRate"));
            float momentum = Float.parseFloat(params.get("momentum"));
            int epochs = Integer.parseInt(params.get("epochs"));
            int stride = Integer.parseInt(params.get("stride"));
            int padding = Integer.parseInt(params.get("padding"));
            int kernelSize = Integer.parseInt(params.get("kernelSize"));
            int outChannel = Integer.parseInt(params.get("outChannel"));
            int inChannel = Integer.parseInt(params.get("inChannel"));
            String saveModel = params.get("saveModel");
            String flag = params.get("flag");  // 新增 flag 参数处理





            // 为每次请求创建独立的 Main，避免并发请求互相覆盖参数
            Main main = new Main();
            main.setBatch_size(batchSize);
            main.setLearning_rate((int)(learningRate));
            main.setMomentum((int)(momentum));
            main.setEPOCH(epochs);
            main.setConvstride(stride);
            main.setConv2dpadding(padding);
            main.setConv2dkernel_size(kernelSize);
            main.setConv2doutchannels(outChannel);
            main.setConv2dinchannels(inChannel);
            main.setU_model_name(resolveCurrentModel(session));
            main.setS_model_name(saveModel);
            main.setFlag(flag);  // 传递 flag 参数

            // 启动训练并获取返回结果
            String result = main.runTraining();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("训练失败: " + e.getMessage());
        }
    }


    @PostMapping("/setModel")
    public ResponseEntity<String> setModelName(@RequestBody Map<String, String> params, HttpSession session) {
        try {
            String modelName = params.get("modelName");
            session.setAttribute(CURRENT_MODEL_SESSION_KEY, modelName);
            return ResponseEntity.ok("模型名称已设置为: " + modelName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("设置模型失败: " + e.getMessage());
        }
    }


    @Autowired
    private TrainingConfigRepository trainingConfigRepository;

    @Autowired
    private TrainingJobService trainingJobService;



    @PostMapping("/updateTrainingResultByModelName")
    public String updateTrainingResultByModelName(@RequestParam String saveModel, @RequestParam String trainingResult) {
        // 手动解码 trainingResult 参数
        String decodedTrainingResult = URLDecoder.decode(trainingResult, StandardCharsets.UTF_8);


        TrainingConfig config = trainingConfigRepository.findBySaveModel(saveModel);
        if (config != null) {
            config.setTrainingResult(decodedTrainingResult);
            trainingConfigRepository.save(config);
            return "训练结果已成功保存到数据库";
        } else {
            return "保存失败：未找到对应的模型记录";
        }
    }


    //--------------------------------------------------------------------------------------------

    @PostMapping("/saveConfig")
    public String saveTrainingConfig(@RequestBody TrainingConfig config) {
        try {
            trainingConfigRepository.save(config);
            return "训练配置已成功保存到数据库";
        } catch (DataIntegrityViolationException e) {
            return "保存失败：模型名称已存在，请使用其他名称";
        }
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startTraining(@RequestBody Map<String, String> params, HttpSession session) {
        return ResponseEntity.ok(trainingJobService.startJob(params, resolveCurrentModel(session), session.getId()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTrainingStatus(@RequestParam String jobId, HttpSession session) {
        return ResponseEntity.ok(trainingJobService.getJobStatus(jobId, session.getId()));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelTraining(@RequestParam String jobId, HttpSession session) {
        return ResponseEntity.ok(trainingJobService.cancelJob(jobId, session.getId()));
    }




    @GetMapping("/model")
    public ResponseEntity<List<String>> getModelList() {
        try {
            // 设置模型存储路径
//            Path modelDirectory = Paths.get("C:/Users/81809/IdeaProjects/tomcat-demo/tomcat-demo/tomcat-demo/demo-app/model");
            Path modelDirectory = Paths.get("model/");

            Files.createDirectories(modelDirectory);

            // 列出目录中的文件
            List<String> modelList = Files.list(modelDirectory)
                    .filter(Files::isRegularFile) // 仅包含文件
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(modelList);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @GetMapping("/getCurrentModel")
    public ResponseEntity<String> getCurrentModel(HttpSession session) {
        return ResponseEntity.ok(resolveCurrentModel(session));
    }

    private String resolveCurrentModel(HttpSession session) {
        Object currentModel = session.getAttribute(CURRENT_MODEL_SESSION_KEY);
        if (currentModel instanceof String modelName && !modelName.isBlank()) {
            return modelName;
        }
        return Main.DEFAULT_MODEL_NAME;
    }

}
