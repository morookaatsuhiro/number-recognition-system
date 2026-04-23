package com.example.demoapp;

import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class TrainingJobService {
    private final ExecutorService jobExecutor = Executors.newCachedThreadPool();
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private final ConcurrentMap<String, TrainingJob> jobs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> activeJobBySession = new ConcurrentHashMap<>();

    public Map<String, Object> startJob(Map<String, String> params, String currentModel, String sessionId) {
        String activeJobId = activeJobBySession.get(sessionId);
        if (activeJobId != null) {
            TrainingJob activeJob = jobs.get(activeJobId);
            if (activeJob != null && activeJob.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "当前已有训练任务正在运行，请先等待完成或取消。");
            }
            activeJobBySession.remove(sessionId, activeJobId);
        }

        String saveModel = params.get("saveModel");
        TrainingJob job = new TrainingJob(
                UUID.randomUUID().toString(),
                sessionId,
                saveModel,
                parseInt(params.get("epochs"), "epochs")
        );

        jobs.put(job.jobId, job);
        activeJobBySession.put(sessionId, job.jobId);
        jobExecutor.submit(() -> runTraining(job, params, currentModel));
        return job.snapshot();
    }

    public Map<String, Object> getJobStatus(String jobId, String sessionId) {
        return getOwnedJob(jobId, sessionId).snapshot();
    }

    public Map<String, Object> cancelJob(String jobId, String sessionId) {
        TrainingJob job = getOwnedJob(jobId, sessionId);

        synchronized (job) {
            if (!job.isActive()) {
                return job.snapshot();
            }
            job.cancellationRequested = true;
            job.status = "CANCELLING";
            job.message = "正在取消训练...";
        }

        Process process = job.process;
        if (process != null) {
            process.destroy();
            jobExecutor.submit(() -> forceStopIfNeeded(process));
        }

        return job.snapshot();
    }

    private void runTraining(TrainingJob job, Map<String, String> params, String currentModel) {
        Future<?> stdoutFuture = null;
        Future<?> stderrFuture = null;

        try {
            NumberRecoRuntimeConfig.ensureBaseDirectories();
            Process process = JavaToPy.startProcess(
                    params.get("batchSize"),
                    params.get("learningRate"),
                    params.get("momentum"),
                    params.get("epochs"),
                    params.get("stride"),
                    params.get("padding"),
                    "2",
                    params.get("kernelSize"),
                    params.get("outChannel"),
                    params.get("inChannel"),
                    NumberRecoRuntimeConfig.getDefaultTrainingImagePath().toAbsolutePath().normalize().toString(),
                    currentModel,
                    job.saveModel,
                    params.get("flag")
            );

            synchronized (job) {
                job.process = process;
                job.status = "RUNNING";
                job.message = "正在准备训练数据...";
            }

            stdoutFuture = ioExecutor.submit(() -> {
                try {
                    JavaToPy.readProcessOutputLines(process.getInputStream(), line -> handleStdout(job, line));
                } catch (IOException e) {
                    appendError(job, "读取训练输出失败: " + e.getMessage());
                }
            });

            stderrFuture = ioExecutor.submit(() -> {
                try {
                    JavaToPy.readProcessOutputLines(process.getErrorStream(), line -> appendError(job, line));
                } catch (IOException e) {
                    appendError(job, "读取训练错误输出失败: " + e.getMessage());
                }
            });

            int exitCode = process.waitFor();
            waitForStreamReaders(stdoutFuture, stderrFuture);

            synchronized (job) {
                if (job.cancellationRequested) {
                    job.status = "CANCELLED";
                    job.message = "训练已取消，模型未保存";
                    cleanupCanceledArtifacts(job.saveModel);
                } else if (exitCode == 0) {
                    job.status = "COMPLETED";
                    job.progressPercent = 100.0;
                    job.message = "训练完成";
                } else {
                    job.status = "FAILED";
                    job.message = "训练失败，进程退出码: " + exitCode;
                }
                job.finishedAt = Instant.now().toString();
            }
        } catch (Exception e) {
            synchronized (job) {
                if (job.cancellationRequested) {
                    job.status = "CANCELLED";
                    job.message = "训练已取消，模型未保存";
                    cleanupCanceledArtifacts(job.saveModel);
                } else {
                    job.status = "FAILED";
                    job.message = "训练失败: " + e.getMessage();
                    job.errorOutput.append(e.getMessage()).append('\n');
                }
                job.finishedAt = Instant.now().toString();
            }
        } finally {
            synchronized (job) {
                job.process = null;
            }
            activeJobBySession.remove(job.sessionId, job.jobId);
        }
    }

    private void handleStdout(TrainingJob job, String line) {
        if (line.startsWith("TRAIN_PROGRESS|")) {
            Map<String, String> values = parseStructuredLine(line);
            synchronized (job) {
                job.currentEpoch = parseInt(values.get("epoch"), "epoch");
                job.totalEpochs = parseInt(values.get("epochs"), "epochs");
                job.currentBatch = parseInt(values.get("batch"), "batch");
                job.totalBatches = parseInt(values.get("batches"), "batches");
                job.progressPercent = parseDouble(values.get("percent"), "percent");
                job.message = String.format("第 %d/%d 轮，第 %d/%d 批", job.currentEpoch, job.totalEpochs, job.currentBatch, job.totalBatches);
            }
            return;
        }

        if (line.startsWith("TRAIN_EPOCH_SUMMARY|")) {
            Map<String, String> values = parseStructuredLine(line);
            synchronized (job) {
                int epoch = parseInt(values.get("epoch"), "epoch");
                int epochs = parseInt(values.get("epochs"), "epochs");
                String testAcc = values.getOrDefault("testAcc", "0.00");
                job.message = String.format("第 %d/%d 轮完成，测试准确率 %s%%", epoch, epochs, testAcc);
                job.currentEpoch = epoch;
                job.totalEpochs = epochs;
            }
            return;
        }

        if (line.startsWith("TRAIN_SAVING|")) {
            synchronized (job) {
                job.progressPercent = parseDouble(parseStructuredLine(line).get("percent"), "percent");
                job.message = "正在保存模型和训练图像...";
            }
            return;
        }

        if (line.startsWith("TRAIN_DONE|")) {
            synchronized (job) {
                job.progressPercent = 100.0;
                job.message = "训练完成";
            }
            return;
        }

        synchronized (job) {
            job.output.append(line).append('\n');
        }
    }

    private void appendError(TrainingJob job, String line) {
        synchronized (job) {
            job.errorOutput.append(line).append('\n');
        }
    }

    private void waitForStreamReaders(Future<?> stdoutFuture, Future<?> stderrFuture) {
        try {
            if (stdoutFuture != null) {
                stdoutFuture.get(10, TimeUnit.SECONDS);
            }
            if (stderrFuture != null) {
                stderrFuture.get(10, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }
    }

    private void forceStopIfNeeded(Process process) {
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private void cleanupCanceledArtifacts(String saveModel) {
        try {
            if (StringUtils.hasText(saveModel)) {
                Files.deleteIfExists(NumberRecoRuntimeConfig.resolveModelPath(saveModel));
                Files.deleteIfExists(NumberRecoRuntimeConfig.getTrainingPlotPath());
            }
        } catch (IOException ignored) {
        }
    }

    private TrainingJob getOwnedJob(String jobId, String sessionId) {
        TrainingJob job = jobs.get(jobId);
        if (job == null || !job.sessionId.equals(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到对应的训练任务");
        }
        return job;
    }

    private Map<String, String> parseStructuredLine(String line) {
        Map<String, String> values = new LinkedHashMap<>();
        String[] segments = line.split("\\|");
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            int index = segment.indexOf('=');
            if (index > 0 && index < segment.length() - 1) {
                values.put(segment.substring(0, index), segment.substring(index + 1));
            }
        }
        return values;
    }

    private int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数 " + field + " 非法");
        }
    }

    private double parseDouble(String value, String field) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数 " + field + " 非法");
        }
    }

    @PreDestroy
    public void shutdown() {
        jobExecutor.shutdownNow();
        ioExecutor.shutdownNow();
    }

    private static final class TrainingJob {
        private final String jobId;
        private final String sessionId;
        private final String saveModel;
        private int totalEpochs;
        private String status = "PENDING";
        private String message = "训练任务已创建";
        private double progressPercent = 0.0;
        private int currentEpoch = 0;
        private int currentBatch = 0;
        private int totalBatches = 0;
        private final String startedAt = Instant.now().toString();
        private String finishedAt;
        private final StringBuilder output = new StringBuilder();
        private final StringBuilder errorOutput = new StringBuilder();
        private volatile boolean cancellationRequested = false;
        private volatile Process process;

        private TrainingJob(String jobId, String sessionId, String saveModel, int totalEpochs) {
            this.jobId = jobId;
            this.sessionId = sessionId;
            this.saveModel = saveModel;
            this.totalEpochs = totalEpochs;
        }

        private boolean isActive() {
            return "PENDING".equals(status) || "RUNNING".equals(status) || "CANCELLING".equals(status);
        }

        private Map<String, Object> snapshot() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("jobId", jobId);
            result.put("status", status);
            result.put("message", message);
            result.put("progressPercent", progressPercent);
            result.put("currentEpoch", currentEpoch);
            result.put("totalEpochs", totalEpochs);
            result.put("currentBatch", currentBatch);
            result.put("totalBatches", totalBatches);
            result.put("output", output.toString());
            result.put("errorOutput", errorOutput.toString());
            result.put("cancellationRequested", cancellationRequested);
            result.put("saveModel", saveModel);
            result.put("startedAt", startedAt);
            result.put("finishedAt", finishedAt);
            return result;
        }
    }
}
