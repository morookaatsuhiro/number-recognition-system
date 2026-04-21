package com.example.demoapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.Base64;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class CaptchaController {

    @GetMapping("/captcha")
    public ResponseEntity<String> generateCaptcha(HttpSession session) {
        String captcha = generateRandomCaptcha();
        session.setAttribute("captcha", captcha); // 将验证码存储在 session 中

        System.out.println("--------------------------------");
        System.out.println("Generated captcha stored in session: " + captcha);
        System.out.println("Session ID (generation): " + session.getId());
        System.out.println("--------------------------------");

        BufferedImage image = createCaptchaImage(captcha);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            String base64Captcha = Base64.getEncoder().encodeToString(baos.toByteArray());
            return ResponseEntity.ok("data:image/png;base64," + base64Captcha);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error generating captcha");
        }
    }

    private String generateRandomCaptcha() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            captcha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return captcha.toString();
    }

    private BufferedImage createCaptchaImage(String captcha) {
        int width = 100, height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(Color.BLACK);
        g2d.drawString(captcha, 10, 25);
        g2d.dispose();
        return image;
    }
}
