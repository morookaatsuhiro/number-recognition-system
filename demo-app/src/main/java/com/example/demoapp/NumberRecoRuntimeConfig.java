package com.example.demoapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class NumberRecoRuntimeConfig {
    private static final String DEFAULT_PYTHON_EXECUTABLE = "C:/Users/67529/number-reco/python.exe";
    private static final String DEFAULT_MODEL_DIR = "model";
    private static final String DEFAULT_IMAGE_DIR = "img";
    private static final String DEFAULT_TRAINING_IMAGE_PATH = "default/default.png";
    private static final String DEFAULT_TRAINING_PLOT_PATH = "model_image/accuracy_plot.png";
    private static final String DEFAULT_CORS_ALLOWED_ORIGINS = String.join(",",
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://192.168.43.252:5173",
            "https://number-recognition-system.vercel.app",
            "https://*.vercel.app"
    );

    private NumberRecoRuntimeConfig() {
    }

    public static String getPythonExecutable() {
        return getEnv("NUMBER_RECO_PYTHON_EXECUTABLE", DEFAULT_PYTHON_EXECUTABLE);
    }

    public static Path getModelDir() {
        return Paths.get(getEnv("NUMBER_RECO_MODEL_DIR", DEFAULT_MODEL_DIR)).normalize();
    }

    public static Path getImageDir() {
        return Paths.get(getEnv("NUMBER_RECO_IMAGE_DIR", DEFAULT_IMAGE_DIR)).normalize();
    }

    public static Path resolveModelPath(String modelName) {
        return getModelDir().resolve(modelName).normalize();
    }

    public static Path resolveImagePath(String imagePath) {
        Path candidate = Paths.get(imagePath);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }
        return getImageDir().resolve(imagePath).normalize();
    }

    public static Path getDefaultTrainingImagePath() {
        return resolveMaybeRelativePath(
                getEnv("NUMBER_RECO_TRAINING_DEFAULT_IMAGE_PATH", DEFAULT_TRAINING_IMAGE_PATH),
                getImageDir()
        );
    }

    public static Path getTrainingPlotPath() {
        return resolveMaybeRelativePath(
                getEnv("NUMBER_RECO_TRAINING_PLOT_PATH", DEFAULT_TRAINING_PLOT_PATH),
                getImageDir()
        );
    }

    public static String toPublicImageUrl(Path imagePath) {
        Path root = getImageDir().toAbsolutePath().normalize();
        Path target = imagePath.toAbsolutePath().normalize();
        try {
            Path relative = root.relativize(target);
            return "/" + relative.toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return "/" + target.getFileName().toString().replace('\\', '/');
        }
    }

    public static List<String> getCorsAllowedOrigins() {
        return Arrays.stream(getEnv("NUMBER_RECO_CORS_ALLOWED_ORIGINS", DEFAULT_CORS_ALLOWED_ORIGINS).split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    public static void ensureBaseDirectories() throws IOException {
        Files.createDirectories(getModelDir());
        Files.createDirectories(getImageDir());

        Path trainingImageParent = getDefaultTrainingImagePath().getParent();
        if (trainingImageParent != null) {
            Files.createDirectories(trainingImageParent);
        }

        Path trainingPlotParent = getTrainingPlotPath().getParent();
        if (trainingPlotParent != null) {
            Files.createDirectories(trainingPlotParent);
        }
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static Path resolveMaybeRelativePath(String pathValue, Path baseDir) {
        Path candidate = Paths.get(pathValue);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }
        return baseDir.resolve(pathValue).normalize();
    }
}
