package com.simple.pulsejob.admin.business.log.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 文件日志读取服务.
 *
 * <p>每次执行一个独立文件，查询 O(1) 定位。</p>
 *
 * <h3>目录结构</h3>
 * <pre>
 * job-logs/
 * └── 2026-01-09/
 *     └── job-123/
 *         ├── 1001.log
 *         ├── 1002.log
 *         └── 1003.log
 * </pre>
 */
@Slf4j
@Service
public class FileLogReader {

    @Value("${pulse-job.log.listener.file.path:./logs/job-logs}")
    private String logBasePath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 读取指定实例的全部日志.
     *
     * @param jobId      任务 ID
     * @param instanceId 任务实例 ID
     * @param date       日期
     * @return 日志行列表
     */
    public List<String> readLogs(Integer jobId, Long instanceId, LocalDate date) {
        Path filePath = makeLogFilePath(jobId, instanceId, date);

        if (!Files.exists(filePath)) {
            log.debug("日志文件不存在: {}", filePath);
            return Collections.emptyList();
        }

        List<String> logs = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", filePath, e);
        }

        return logs;
    }

    /**
     * 读取指定实例的日志（分页）.
     *
     * @param jobId      任务 ID
     * @param instanceId 任务实例 ID
     * @param date       日期
     * @param fromLine   起始行号（1-based）
     * @param limit      返回行数
     * @return 日志行列表
     */
    public List<String> readLogs(Integer jobId, Long instanceId, LocalDate date, int fromLine, int limit) {
        Path filePath = makeLogFilePath(jobId, instanceId, date);

        if (!Files.exists(filePath)) {
            return Collections.emptyList();
        }

        List<String> logs = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum >= fromLine) {
                    logs.add(line);
                    if (logs.size() >= limit) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", filePath, e);
        }

        return logs;
    }

    /**
     * 读取日志文件末尾 N 行（用于实时查看）.
     *
     * @param jobId      任务 ID
     * @param instanceId 任务实例 ID
     * @param date       日期
     * @param tailLines  末尾行数
     * @return 日志行列表
     */
    public List<String> tailLogs(Integer jobId, Long instanceId, LocalDate date, int tailLines) {
        Path filePath = makeLogFilePath(jobId, instanceId, date);

        if (!Files.exists(filePath)) {
            return Collections.emptyList();
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) {
                return Collections.emptyList();
            }

            List<String> lines = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            long pos = fileLength - 1;

            // 从文件末尾往前读
            while (pos >= 0 && lines.size() < tailLines) {
                raf.seek(pos);
                int ch = raf.read();
                if (ch == '\n') {
                    if (sb.length() > 0) {
                        lines.add(sb.reverse().toString());
                        sb.setLength(0);
                    }
                } else if (ch != '\r') {
                    sb.append((char) ch);
                }
                pos--;
            }

            // 处理最后一行
            if (sb.length() > 0 && lines.size() < tailLines) {
                lines.add(sb.reverse().toString());
            }

            Collections.reverse(lines);
            return lines;
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", filePath, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取日志文件总行数.
     *
     * @param jobId      任务 ID
     * @param instanceId 任务实例 ID
     * @param date       日期
     * @return 行数
     */
    public long countLines(Integer jobId, Long instanceId, LocalDate date) {
        Path filePath = makeLogFilePath(jobId, instanceId, date);

        if (!Files.exists(filePath)) {
            return 0;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            return reader.lines().count();
        } catch (IOException e) {
            log.error("统计日志行数失败: {}", filePath, e);
            return 0;
        }
    }

    /**
     * 获取某个 Job 某天所有 instanceId 列表.
     *
     * @param jobId 任务 ID
     * @param date  日期
     * @return instanceId 列表（排序）
     */
    public List<Long> listInstanceIds(Integer jobId, LocalDate date) {
        Path jobDir = Paths.get(
                logBasePath,
                date.format(DATE_FORMATTER),
                "job-" + (jobId != null ? jobId : 0)
        );

        if (!Files.exists(jobDir) || !Files.isDirectory(jobDir)) {
            return Collections.emptyList();
        }

        List<Long> instanceIds = new ArrayList<>();
        try (Stream<Path> files = Files.list(jobDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".log"))
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        // 提取 {instanceId}.log 中的 instanceId
                        String idStr = fileName.substring(0, fileName.length() - 4);
                        try {
                            instanceIds.add(Long.parseLong(idStr));
                        } catch (NumberFormatException ignored) {
                        }
                    });
        } catch (IOException e) {
            log.error("列出实例文件失败: {}", jobDir, e);
        }

        Collections.sort(instanceIds);
        return instanceIds;
    }

    /**
     * 获取某天所有 jobId 列表.
     *
     * @param date 日期
     * @return jobId 列表（排序）
     */
    public List<Integer> listJobIds(LocalDate date) {
        Path dateDir = Paths.get(logBasePath, date.format(DATE_FORMATTER));

        if (!Files.exists(dateDir) || !Files.isDirectory(dateDir)) {
            return Collections.emptyList();
        }

        List<Integer> jobIds = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(dateDir)) {
            dirs.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("job-"))
                    .forEach(p -> {
                        String dirName = p.getFileName().toString();
                        String idStr = dirName.substring(4); // 去掉 "job-" 前缀
                        try {
                            jobIds.add(Integer.parseInt(idStr));
                        } catch (NumberFormatException ignored) {
                        }
                    });
        } catch (IOException e) {
            log.error("列出 Job 目录失败: {}", dateDir, e);
        }

        Collections.sort(jobIds);
        return jobIds;
    }

    /**
     * 检查日志文件是否存在.
     *
     * @param jobId      任务 ID
     * @param instanceId 任务实例 ID
     * @param date       日期
     * @return 是否存在
     */
    public boolean exists(Integer jobId, Long instanceId, LocalDate date) {
        return Files.exists(makeLogFilePath(jobId, instanceId, date));
    }

    /**
     * 获取日志文件大小（字节）.
     *
     * @param jobId      任务 ID
     * @param instanceId 任务实例 ID
     * @param date       日期
     * @return 文件大小
     */
    public long getFileSize(Integer jobId, Long instanceId, LocalDate date) {
        Path filePath = makeLogFilePath(jobId, instanceId, date);
        try {
            return Files.exists(filePath) ? Files.size(filePath) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 获取日志文件路径（供下载使用）.
     *
     * @param jobId      任务 ID
     * @param instanceId 任务实例 ID
     * @param date       日期
     * @return 文件路径
     */
    public Path getFilePath(Integer jobId, Long instanceId, LocalDate date) {
        return makeLogFilePath(jobId, instanceId, date);
    }

    /**
     * 生成日志文件路径：{basePath}/{date}/job-{jobId}/{instanceId}.log
     *
     * @param jobId      任务 ID
     * @param instanceId 任务实例 ID
     * @param date       日期
     * @return 文件路径
     */
    private Path makeLogFilePath(Integer jobId, Long instanceId, LocalDate date) {
        String dateDir = date != null ? date.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);

        return Paths.get(
                logBasePath,
                dateDir,
                "job-" + (jobId != null ? jobId : 0),
                (instanceId != null ? instanceId : 0) + ".log"
        );
    }
}
