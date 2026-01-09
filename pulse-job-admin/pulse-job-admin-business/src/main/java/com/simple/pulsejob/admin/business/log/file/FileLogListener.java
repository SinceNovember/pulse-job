package com.simple.pulsejob.admin.business.log.file;

import com.simple.pulsejob.admin.scheduler.log.JobLogListener;
import com.simple.pulsejob.transport.metadata.LogMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件日志写入监听器.
 *
 * <p>存储策略：每次执行一个独立文件（类似 XXL-Job）</p>
 *
 * <h3>目录结构</h3>
 * <pre>
 * job-logs/
 * └── 2026-01-09/                  # 按日期（便于清理）
 *     └── job-123/                 # 按 jobId
 *         ├── 1001.log             # instanceId.log
 *         ├── 1002.log
 *         └── 1003.log
 * </pre>
 *
 * <h3>优势</h3>
 * <ul>
 *   <li>查询 O(1)：直接读取对应文件</li>
 *   <li>完全隔离：每次执行独立文件</li>
 *   <li>易于清理：按日期目录删除</li>
 * </ul>
 *
 * <h3>配置项</h3>
 * <pre>
 * pulse-job:
 *   log:
 *     listener:
 *       file:
 *         enabled: true                # 启用文件存储
 *         path: ./logs/job-logs        # 存储根目录
 *         retention-days: 7            # 日志保留天数
 * </pre>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pulse-job.log.listener.file.enabled", havingValue = "true", matchIfMissing = true)
public class FileLogListener implements JobLogListener {

    @Value("${pulse-job.log.listener.file.path:D:/logs/job-logs}")
    private String logBasePath;

    private final ConcurrentHashMap<String, BufferedWriter> writerCache = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(logBasePath));
            log.info("文件日志存储根目录: {}", logBasePath);
        } catch (IOException e) {
            log.error("创建日志目录失败: {}", logBasePath, e);
        }
    }

    @PreDestroy
    public void destroy() {
        closeAllWriters();
    }

    @Override
    public void onLog(LogMessage logMessage) {
        try {
            writeLog(logMessage);
        } catch (IOException e) {
            log.error("写入日志文件失败", e);
        }
    }

    @Override
    public void onBatchLog(List<LogMessage> logs) {
        try {
            for (LogMessage logMessage : logs) {
                writeLog(logMessage);
            }
            flushAllWriters();
            log.debug("批量写入日志文件，数量: {}", logs.size());
        } catch (IOException e) {
            log.error("批量写入日志文件失败", e);
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    private void writeLog(LogMessage logMessage) throws IOException {
        Path filePath = makeLogFilePath(logMessage);

        // 确保目录存在
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        BufferedWriter writer = getOrCreateWriter(filePath.toString());

        // 直接写入原始内容（每个 instance 独立文件，无需前缀）
        String content = logMessage.getContent();
        if (content != null) {
            synchronized (writer) {
                writer.write(content);
                writer.newLine();
            }
        }
    }

    /**
     * 生成日志文件路径.
     *
     * <p>格式：{basePath}/{date}/job-{jobId}/{instanceId}.log</p>
     *
     * @param logMessage 日志消息
     * @return 文件路径
     */
    private Path makeLogFilePath(LogMessage logMessage) {
        String dateDir = logMessage.getTimestamp() != null
                ? logMessage.getTimestamp().toLocalDate().format(DATE_FORMATTER)
                : "unknown";

        Integer jobId = logMessage.getJobId();
        Long instanceId = logMessage.getInstanceId();

        return Paths.get(
                logBasePath,
                dateDir,
                "job-" + (jobId != null ? jobId : 0),
                (instanceId != null ? instanceId : 0) + ".log"
        );
    }

    /**
     * 静态方法：生成日志文件路径（供外部查询使用）.
     *
     * @param basePath   日志根目录
     * @param jobId      任务 ID
     * @param instanceId 实例 ID
     * @param date       日期字符串 (yyyy-MM-dd)
     * @return 文件路径
     */
    public static Path makeLogFilePath(String basePath, Integer jobId, Long instanceId, String date) {
        return Paths.get(
                basePath,
                date,
                "job-" + (jobId != null ? jobId : 0),
                (instanceId != null ? instanceId : 0) + ".log"
        );
    }

    private BufferedWriter getOrCreateWriter(String filePath) {
        return writerCache.computeIfAbsent(filePath, path -> {
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

    private void flushAllWriters() {
        for (BufferedWriter writer : writerCache.values()) {
            try {
                writer.flush();
            } catch (IOException e) {
                log.error("刷新文件写入器失败", e);
            }
        }
    }

    private void closeAllWriters() {
        for (BufferedWriter writer : writerCache.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                log.error("关闭文件写入器失败", e);
            }
        }
        writerCache.clear();
    }

    /**
     * 关闭指定实例的 Writer（任务完成后调用，释放资源）.
     *
     * @param jobId      任务 ID
     * @param instanceId 实例 ID
     * @param date       日期字符串 (yyyy-MM-dd)
     */
    public void closeWriter(Integer jobId, Long instanceId, String date) {
        Path filePath = makeLogFilePath(logBasePath, jobId, instanceId, date);
        BufferedWriter writer = writerCache.remove(filePath.toString());
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                log.error("关闭文件写入器失败: {}", filePath, e);
            }
        }
    }
}
