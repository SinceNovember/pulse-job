package com.simple.pulsejob.admin.business.log.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

/**
 * 文件日志清理服务.
 *
 * <p>定时清理过期的日志文件，按日期目录删除。</p>
 *
 * <h3>配置项</h3>
 * <pre>
 * pulse-job:
 *   log:
 *     listener:
 *       file:
 *         retention-days: 7    # 日志保留天数（默认7天）
 *         clean-enabled: true  # 是否启用自动清理
 * </pre>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pulse-job.log.listener.file.clean-enabled", havingValue = "true", matchIfMissing = true)
public class FileLogCleaner {

    @Value("${pulse-job.log.listener.file.path:./logs/job-logs}")
    private String logBasePath;

    @Value("${pulse-job.log.listener.file.retention-days:7}")
    private int retentionDays;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 每天凌晨 3 点执行清理.
     */
    @Scheduled(cron = "${pulse-job.log.listener.file.clean-cron:0 0 3 * * ?}")
    public void cleanOldLogs() {
        log.info("开始清理过期日志，保留天数: {}", retentionDays);

        LocalDate expireDate = LocalDate.now().minusDays(retentionDays);
        Path basePath = Paths.get(logBasePath);

        if (!Files.exists(basePath)) {
            return;
        }

        int deletedDirs = 0;
        long deletedFiles = 0;
        long deletedBytes = 0;

        try (Stream<Path> dateDirs = Files.list(basePath)) {
            for (Path dateDir : dateDirs.toList()) {
                if (!Files.isDirectory(dateDir)) {
                    continue;
                }

                String dirName = dateDir.getFileName().toString();
                try {
                    LocalDate dirDate = LocalDate.parse(dirName, DATE_FORMATTER);
                    if (dirDate.isBefore(expireDate)) {
                        // 统计并删除
                        long[] stats = deleteDirectoryRecursively(dateDir);
                        deletedFiles += stats[0];
                        deletedBytes += stats[1];
                        deletedDirs++;
                        log.info("已删除过期日志目录: {}", dateDir);
                    }
                } catch (DateTimeParseException e) {
                    // 目录名不是日期格式，跳过
                    log.debug("跳过非日期目录: {}", dirName);
                }
            }
        } catch (IOException e) {
            log.error("清理日志失败", e);
        }

        log.info("日志清理完成，删除目录: {}，删除文件: {}，释放空间: {} MB",
                deletedDirs, deletedFiles, deletedBytes / 1024 / 1024);
    }

    /**
     * 手动触发清理（可通过 API 调用）.
     *
     * @param beforeDate 删除此日期之前的日志
     * @return 删除的文件数
     */
    public long cleanBefore(LocalDate beforeDate) {
        log.info("手动清理日志，删除 {} 之前的数据", beforeDate);

        Path basePath = Paths.get(logBasePath);
        if (!Files.exists(basePath)) {
            return 0;
        }

        long totalDeleted = 0;

        try (Stream<Path> dateDirs = Files.list(basePath)) {
            for (Path dateDir : dateDirs.toList()) {
                if (!Files.isDirectory(dateDir)) {
                    continue;
                }

                String dirName = dateDir.getFileName().toString();
                try {
                    LocalDate dirDate = LocalDate.parse(dirName, DATE_FORMATTER);
                    if (dirDate.isBefore(beforeDate)) {
                        long[] stats = deleteDirectoryRecursively(dateDir);
                        totalDeleted += stats[0];
                        log.info("已删除日志目录: {}", dateDir);
                    }
                } catch (DateTimeParseException ignored) {
                }
            }
        } catch (IOException e) {
            log.error("清理日志失败", e);
        }

        return totalDeleted;
    }

    /**
     * 递归删除目录.
     *
     * @return [删除文件数, 删除字节数]
     */
    private long[] deleteDirectoryRecursively(Path dir) throws IOException {
        final long[] stats = {0, 0}; // [fileCount, byteCount]

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                stats[0]++;
                stats[1] += attrs.size();
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        return stats;
    }

    /**
     * 获取日志存储统计信息.
     */
    public LogStorageStats getStorageStats() {
        Path basePath = Paths.get(logBasePath);
        LogStorageStats stats = new LogStorageStats();

        if (!Files.exists(basePath)) {
            return stats;
        }

        try (Stream<Path> dateDirs = Files.list(basePath)) {
            for (Path dateDir : dateDirs.toList()) {
                if (!Files.isDirectory(dateDir)) {
                    continue;
                }

                stats.totalDays++;

                try (Stream<Path> jobDirs = Files.list(dateDir)) {
                    for (Path jobDir : jobDirs.toList()) {
                        if (!Files.isDirectory(jobDir)) {
                            continue;
                        }

                        try (Stream<Path> files = Files.list(jobDir)) {
                            for (Path file : files.toList()) {
                                if (Files.isRegularFile(file)) {
                                    stats.totalFiles++;
                                    stats.totalBytes += Files.size(file);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("统计日志存储失败", e);
        }

        return stats;
    }

    /**
     * 日志存储统计.
     */
    public static class LogStorageStats {
        public int totalDays;
        public long totalFiles;
        public long totalBytes;

        public String getTotalSizeFormatted() {
            if (totalBytes < 1024) {
                return totalBytes + " B";
            } else if (totalBytes < 1024 * 1024) {
                return String.format("%.2f KB", totalBytes / 1024.0);
            } else if (totalBytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", totalBytes / 1024.0 / 1024.0);
            } else {
                return String.format("%.2f GB", totalBytes / 1024.0 / 1024.0 / 1024.0);
            }
        }
    }
}

