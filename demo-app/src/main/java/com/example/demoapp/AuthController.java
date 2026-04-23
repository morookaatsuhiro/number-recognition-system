
package com.example.demoapp;

        import jakarta.servlet.http.HttpSession;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.*;

        import javax.imageio.ImageIO;
        import java.awt.*;
        import java.awt.image.BufferedImage;
        import java.util.*;
        import java.util.Base64;
        import java.io.ByteArrayOutputStream;
        import java.util.Optional;
        import java.util.Random;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private String currentCaptcha; // 用于存储当前验证码

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();

        // 验证用户名格式
        if (!user.getUsername().matches("^[A-Za-z0-9]{8,12}$")) {
            response.put("error", "用户名应为8-12位的数字和大小写字母");
            return ResponseEntity.badRequest().body(response);
        }
        // 验证密码格式
        if (!user.getPassword().matches("^[A-Za-z0-9_]{6,16}$")) {
            response.put("error", "密码应为6-16位数字、字母或下划线");
            return ResponseEntity.badRequest().body(response);
        }

        // 检查用户名是否已存在
        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            response.put("error", "用户名已存在，请选择其他用户名");
            return ResponseEntity.badRequest().body(response);
        }

        // 保存新用户
        userRepository.save(user);
        response.put("success", "注册成功！");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody User user, @RequestParam String captchaInput, HttpSession session) {
        Map<String, String> response = new HashMap<>();




        // 从 session 中获取验证码
        String storedCaptcha = (String) session.getAttribute("captcha");

        // 打印日志验证captcha
        System.out.println("******************************************");
        System.out.println("Received captchaInput: " + captchaInput);
        System.out.println("Current captcha from session: " + currentCaptcha);
        System.out.println("Session ID (validation): " + session.getId());
        System.out.println("******************************************");




        if (storedCaptcha == null || !captchaInput.equalsIgnoreCase(storedCaptcha)) {
            response.put("error", "验证码错误，请重试！");
            return ResponseEntity.status(401).body(response);
        }

        // 清除验证码以防重复使用
        session.removeAttribute("captcha");

        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent() && existingUser.get().getPassword().equals(user.getPassword())) {
            response.put("success", "登录成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "用户名或密码错误！");
            return ResponseEntity.status(401).body(response);
        }
    }


}

