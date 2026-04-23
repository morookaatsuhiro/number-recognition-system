package com.example.demoapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;

public abstract class JavaToPy {
    private static final String PYTHON_SCRIPT_PATH = Paths.get(
            "src", "main", "java", "com", "example", "demoapp", "cnn.py"
    ).toString();

    public static String getFile(String batch_size, String learning_rate, String momentum, String EPOCH,
                                 String convstride, String conv2dpadding, String pool2dKernelSize,
                                 String conv2dKernelSize, String conv2dOutChannels, String conv2dInChannels,
                                 String imagePath, String useModelName, String saveModelName, String flag)
            throws IOException, InterruptedException {
        Process process = startProcess(
                batch_size, learning_rate, momentum, EPOCH, convstride, conv2dpadding,
                pool2dKernelSize, conv2dKernelSize, conv2dOutChannels, conv2dInChannels,
                imagePath, useModelName, saveModelName, flag
        );

        String output = getProcessOutput(process.getInputStream());
        String errorOutput = getProcessOutput(process.getErrorStream());

        if (!errorOutput.isEmpty()) {
            System.err.println("Python Error: " + errorOutput);
        }

        process.waitFor();
        return output;
    }

    public static Process startProcess(String batch_size, String learning_rate, String momentum, String EPOCH,
                                       String convstride, String conv2dpadding, String pool2dKernelSize,
                                       String conv2dKernelSize, String conv2dOutChannels, String conv2dInChannels,
                                       String imagePath, String useModelName, String saveModelName, String flag)
            throws IOException {
        System.out.println("batch_size: " + batch_size);
        System.out.println("learning_rate: " + learning_rate);
        System.out.println("momentum: " + momentum);
        System.out.println("EPOCH: " + EPOCH);
        System.out.println("convstride: " + convstride);
        System.out.println("conv2dpadding: " + conv2dpadding);
        System.out.println("Pool2dkernel_size: " + pool2dKernelSize);
        System.out.println("conv2dkernel_size: " + conv2dKernelSize);
        System.out.println("conv2doutchannels: " + conv2dOutChannels);
        System.out.println("conv2dinchannels: " + conv2dInChannels);
        System.out.println("image_path: " + imagePath);
        System.out.println("u_model_name: " + useModelName);
        System.out.println("s_model_name: " + saveModelName);
        System.out.println("flag: " + flag);

        NumberRecoRuntimeConfig.ensureBaseDirectories();
        Path trainingPlotPath = NumberRecoRuntimeConfig.getTrainingPlotPath();
        if (trainingPlotPath.getParent() != null) {
            Files.createDirectories(trainingPlotPath.getParent());
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                NumberRecoRuntimeConfig.getPythonExecutable(),
                PYTHON_SCRIPT_PATH,
                batch_size, learning_rate, momentum, EPOCH, convstride, conv2dpadding,
                pool2dKernelSize, conv2dKernelSize, conv2dOutChannels, conv2dInChannels,
                imagePath, useModelName, saveModelName, flag
        );

        processBuilder.directory(new File("."));
        Map<String, String> environment = processBuilder.environment();
        environment.put("NUMBER_RECO_MODEL_DIR", NumberRecoRuntimeConfig.getModelDir().toAbsolutePath().normalize().toString());
        environment.put("NUMBER_RECO_IMAGE_DIR", NumberRecoRuntimeConfig.getImageDir().toAbsolutePath().normalize().toString());
        environment.put("NUMBER_RECO_TRAINING_PLOT_PATH", trainingPlotPath.toAbsolutePath().normalize().toString());

        return processBuilder.start();
    }

    private static String getProcessOutput(InputStream input) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader bf = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bf.readLine()) != null) {
                if (!line.startsWith("TRAIN_")) {
                    output.append(line).append("\n");
                }
            }
        }
        return output.toString();
    }

    public static void readProcessOutputLines(InputStream input, Consumer<String> lineConsumer) throws IOException {
        try (BufferedReader bf = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bf.readLine()) != null) {
                lineConsumer.accept(line);
            }
        }
    }
}
