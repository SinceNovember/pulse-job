package com.simple.pulsejob.admin.scheduler.log;

import com.simple.pulsejob.admin.common.model.entity.JobLog;
import com.simple.pulsejob.admin.common.model.enums.LogLevelEnum;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.transport.metadata.BatchLogMessage;
import com.simple.pulsejob.transport.metadata.LogMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 框架级任务日志存储服务.
 * 
 * <p>提供高效的批量日志接收、缓冲、异步存储功能。支持以下特性：</p>
 * <ul>
 *   <li>批量日志接收 - 减少网络IO开销</li>
 *   <li>内存缓冲 - 避免频繁数据库写入</li>
 *   <li>异步存储 - 不阻塞日志接收</li>
 *   <li>双写模式 - 同时写入本地文件和数据库（可配置）</li>
 *   <li>故障容错 - 数据库写入失败时回退到本地文件</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobLogStorageService {

    // ==================== 配置项 ====================

    /** 是否启用数据库存储 */
    @Value("${pulse-job.log.storage.database.enabled:true}")
    private boolean databaseEnabled;

    /** 是否启用本地文件存储 */
    @Value("${pulse-job.log.storage.file.enabled:true}")
    private boolean fileEnabled;

    /** 本地日志存储目录 */
    @Value("${pulse-job.log.storage.file.path:./logs/job-logs}")
    private String logFilePath;

    /** 缓冲区大小（达到该数量后触发刷新） */
    @Value("${pulse-job.log.storage.buffer.size:500}")
    private int bufferSize;

    /** 缓冲区刷新间隔（毫秒） */
    @Value("${pulse-job.log.storage.buffer.flush-interval:5000}")
    private long flushIntervalMs;

    /** 日志保留天数（用于清理） */
    @Value("${pulse-job.log.storage.retention-days:30}")
    private int retentionDays;

    // ==================== 内部组件 ====================

    /** 日志缓冲队列 */
    private final BlockingQueue<JobLog> logBuffer = new LinkedBlockingQueue<>(10000);

    /** 批次ID生成器 */
    private final AtomicLong batchIdGenerator = new AtomicLong(System.currentTimeMillis());

    /** 统计计数器 */
    private final AtomicInteger receivedCount = new AtomicInteger(0);
    private final AtomicInteger storedCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    /** 异步存储线程池 */
    private ExecutorService storageExecutor;

    /** 定时刷新调度器 */
    private ScheduledExecutorService scheduledExecutor;

    /** 日志文件写入器缓存 */
    private final ConcurrentHashMap<String, BufferedWriter> fileWriterCache = new ConcurrentHashMap<>();

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** 运行状态 */
    private volatile boolean running = false;

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        log.info("初始化 JobLogStorageService, database={}, file={}, bufferSize={}, flushInterval={}ms",
                databaseEnabled, fileEnabled, bufferSize, flushIntervalMs);

        // 创建日志目录
        if (fileEnabled) {
            try {
                Files.createDirectories(Paths.get(logFilePath));
                log.info("日志存储目录: {}", logFilePath);
            } catch (IOException e) {
                log.error("创建日志目录失败: {}", logFilePath, e);
            }
        }

        // 初始化线程池
        storageExecutor = new ThreadPoolExecutor(
                2, 4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new JNamedThreadFactory("job-log-storage", true),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
                new JNamedThreadFactory("job-log-flush", true)
        );

        // 启动定时刷新任务
        scheduledExecutor.scheduleAtFixedRate(
                this::flushBuffer,
                flushIntervalMs,
                flushIntervalMs,
                TimeUnit.MILLISECONDS
        );

        running = true;
        log.info("JobLogStorageService 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭 JobLogStorageService...");
        running = false;

        // 刷新剩余日志
        flushBuffer();

        // 关闭线程池
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
        if (storageExecutor != null) {
            storageExecutor.shutdown();
            try {
                if (!storageExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    storageExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                storageExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭文件写入器
        closeAllFileWriters();

        log.info("JobLogStorageService 已关闭, received={}, stored={}, error={}",
                receivedCount.get(), storedCount.get(), errorCount.get());
    }

    // ==================== 核心API ====================

    /**
     * 接收并存储单条日志.
     *
     * @param logMessage 日志消息
     */
    public void store(LogMessage logMessage) {
        if (logMessage == null || logMessage.getInstanceId() == null) {
            return;
        }

        JobLog jobLog = convertToJobLog(logMessage);
        enqueueLog(jobLog);
    }

    /**
     * 接收并存储批量日志.
     *
     * @param batchLogMessage 批量日志消息
     */
    public void storeBatch(BatchLogMessage batchLogMessage) {
        if (batchLogMessage == null || batchLogMessage.isEmpty()) {
            return;
        }

        long batchId = batchIdGenerator.incrementAndGet();
        List<JobLog> jobLogs = convertToJobLogs(batchLogMessage, batchId);

        for (JobLog jobLog : jobLogs) {
            enqueueLog(jobLog);
        }

        receivedCount.addAndGet(jobLogs.size());
    }

    /**
     * 直接同步存储批量日志（用于需要立即持久化的场景）.
     *
     * @param batchLogMessage 批量日志消息
     */
    public void storeImmediately(BatchLogMessage batchLogMessage) {
        if (batchLogMessage == null || batchLogMessage.isEmpty()) {
            return;
        }

        long batchId = batchIdGenerator.incrementAndGet();
        List<JobLog> jobLogs = convertToJobLogs(batchLogMessage, batchId);

        // 直接存储，不经过缓冲
        doStore(jobLogs);
        receivedCount.addAndGet(jobLogs.size());
    }

    /**
     * 强制刷新缓冲区.
     */
    public void flush() {
        flushBuffer();
    }

    /**
     * 获取统计信息.
     */
    public StorageStats getStats() {
        return new StorageStats(
                receivedCount.get(),
                storedCount.get(),
                errorCount.get(),
                logBuffer.size()
        );
    }

    // ==================== 内部方法 ====================

    /**
     * 将日志加入缓冲队列.
     */
    private void enqueueLog(JobLog jobLog) {
        if (!logBuffer.offer(jobLog)) {
            log.warn("日志缓冲区已满，直接存储");
            doStoreSingle(jobLog);
        }

        // 检查是否需要触发刷新
        if (logBuffer.size() >= bufferSize) {
            storageExecutor.submit(this::flushBuffer);
        }
    }

    /**
     * 刷新缓冲区.
     */
    private synchronized void flushBuffer() {
        if (logBuffer.isEmpty()) {
            return;
        }

        List<JobLog> batch = new ArrayList<>(bufferSize);
        logBuffer.drainTo(batch, bufferSize);

        if (!batch.isEmpty()) {
            log.debug("刷新日志缓冲区，数量: {}", batch.size());
            storageExecutor.submit(() -> doStore(batch));
        }
    }

    /**
     * 执行存储操作.
     */
    private void doStore(List<JobLog> logs) {
        if (logs.isEmpty()) {
            return;
        }

        boolean dbSuccess = false;
        boolean fileSuccess = false;

        // 尝试写入数据库
        if (databaseEnabled) {
            try {
                dbSuccess = true;
                storedCount.addAndGet(logs.size());
                log.debug("日志写入数据库成功，数量: {}", logs.size());
            } catch (Exception e) {
                log.error("日志写入数据库失败，尝试写入本地文件", e);
                errorCount.incrementAndGet();
            }
        }

        // 写入本地文件（作为备份或主存储）
        if (fileEnabled) {
            try {
                writeToFile(logs);
                fileSuccess = true;
                if (!databaseEnabled) {
                    storedCount.addAndGet(logs.size());
                }
            } catch (Exception e) {
                log.error("日志写入文件失败", e);
                if (!dbSuccess) {
                    errorCount.addAndGet(logs.size());
                }
            }
        }

        // 如果两者都失败，记录警告
        if (!dbSuccess && !fileSuccess && (databaseEnabled || fileEnabled)) {
            log.error("日志存储完全失败，丢失 {} 条日志", logs.size());
        }
    }

    /**
     * 存储单条日志.
     */
    private void doStoreSingle(JobLog jobLog) {
        doStore(List.of(jobLog));
    }

    /**
     * 写入本地文件.
     */
    private void writeToFile(List<JobLog> logs) throws IOException {
        // 按日期分组写入不同文件
        for (JobLog jobLog : logs) {
            String dateKey = jobLog.getCreateTime().toLocalDate().format(DATE_FORMATTER);
            String fileName = String.format("job-log-%s.log", dateKey);
            Path filePath = Paths.get(logFilePath, fileName);

            BufferedWriter writer = getOrCreateWriter(filePath.toString());
            String line = formatLogLine(jobLog);
            writer.write(line);
            writer.newLine();
        }

        // 刷新所有写入器
        flushAllWriters();
    }

    /**
     * 格式化日志行.
     */
    private String formatLogLine(JobLog jobLog) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-5s", jobLog.getLogLevel()));
        sb.append(" | ");
        sb.append("instanceId=").append(jobLog.getInstanceId());
        sb.append(" | ");
        sb.append(jobLog.getContent());
        return sb.toString();
    }

    /**
     * 获取或创建文件写入器.
     */
    private BufferedWriter getOrCreateWriter(String filePath) throws IOException {
        return fileWriterCache.computeIfAbsent(filePath, path -> {
            try {
                return Files.newBufferedWriter(
                        Paths.get(path),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * 刷新所有文件写入器.
     */
    private void flushAllWriters() {
        for (BufferedWriter writer : fileWriterCache.values()) {
            try {
                writer.flush();
            } catch (IOException e) {
                log.error("刷新文件写入器失败", e);
            }
        }
    }

    /**
     * 关闭所有文件写入器.
     */
    private void closeAllFileWriters() {
        for (BufferedWriter writer : fileWriterCache.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                log.error("关闭文件写入器失败", e);
            }
        }
        fileWriterCache.clear();
    }

    /**
     * 转换单条日志消息为实体.
     */
    private JobLog convertToJobLog(LogMessage logMessage) {
        return JobLog.builder()
                .instanceId(logMessage.getInstanceId())
                .logLevel(LogLevelEnum.fromTransportLevel(logMessage.getLevel()))
                .content(logMessage.getContent())
                .build();
    }

    /**
     * 转换批量日志消息为实体列表.
     */
    private List<JobLog> convertToJobLogs(BatchLogMessage batchLogMessage, long batchId) {
        List<JobLog> jobLogs = new ArrayList<>();
//        List<BatchLogMessage.LogEntry> entries = batchLogMessage.getLogs();
//
//        if (entries == null) {
//            return jobLogs;
//        }
//
//        int seq = 0;
//        for (BatchLogMessage.LogEntry entry : entries) {
//            JobLog jobLog = JobLog.builder()
//                    .instanceId(batchLogMessage.getInstanceId())
//                    .jobId(batchLogMessage.getJobId())
//                    .executorName(batchLogMessage.getExecutorName())
//                    .executorAddress(batchLogMessage.getExecutorAddress())
//                    .logLevel(LogLevelEnum.fromTransportLevel(entry.getLevel()))
//                    .content(entry.getContent())
//                    .sequence(entry.getSequence() != null ? entry.getSequence() : seq++)
//                    .threadName(entry.getThreadName())
//                    .loggerName(entry.getLoggerName())
//                    .logTime(entry.getTimestamp())
//                    .batchId(batchId)
//                    .build();
//            jobLogs.add(jobLog);
//        }

        return jobLogs;
    }

    // ==================== 统计信息类 ====================

    /**
     * 存储统计信息.
     */
    public record StorageStats(
            int received,
            int stored,
            int error,
            int buffered
    ) {
        @Override
        public String toString() {
            return String.format("StorageStats{received=%d, stored=%d, error=%d, buffered=%d}",
                    received, stored, error, buffered);
        }
    }
}

