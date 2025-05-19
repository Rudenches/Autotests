package com.romashka.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ContainerLogReader {

    private static final String DOCKER_LOGS_CMD = "docker compose logs %s --tail %d";
    private static final String CONTAINER_NAME = "brt"; // Имя вашего контейнера BRT
    private static final String LOGS_DIR = "test-logs";
    private static final Path PROJECT_ROOT = Paths.get("C:\\Users\\Rudenches\\IdeaProjects\\romashka-telecom-main");

    /**
     * Получает логи контейнера за последние N секунд
     * @param maxLines Максимальное количество строк для чтения
     * @return Список строк логов
     */
    public List<String> getContainerLogs(int maxLines) {
        List<String> logs = new ArrayList<>();
        try {
            String command = "docker compose logs brt --tail 1000";
            log.info("Executing command: {}", command);

            // Проверяем, что команда существует
            Process checkProcess = Runtime.getRuntime().exec("where docker");
            if (!checkProcess.waitFor(5, TimeUnit.SECONDS)) {
                log.error("Docker command not found in system PATH");
                return logs;
            }

            // Выполняем основную команду
            Process process = Runtime.getRuntime().exec(command);
            log.info("Process started with PID: {}", process.pid());

            // Ждем завершения процесса с таймаутом
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("Timeout while reading container logs");
                return logs;
            }

            // Проверяем ошибки выполнения команды
            if (process.exitValue() != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    StringBuilder errorOutput = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                    log.error("Error executing docker logs command. Exit code: {}. Error output:\n{}",
                            process.exitValue(), errorOutput);
                }
                return logs;
            }

            // Читаем вывод команды
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    logs.add(line);
                    lineCount++;
                    if (lineCount % 100 == 0) {
                        log.debug("Read {} lines so far", lineCount);
                    }
                }
                log.info("Finished reading {} lines from process output", lineCount);
            }

            log.info("Successfully read {} log lines", logs.size());

        } catch (Exception e) {
            log.error("Failed to read container logs", e);
            // Добавляем стектрейс для лучшей диагностики
            for (StackTraceElement element : e.getStackTrace()) {
                log.error("  at {}", element);
            }
        }
        return logs;
    }

    /**
     * Сохраняет логи в файл
     * @param logs Список строк логов для сохранения
     * @return Путь к сохраненному файлу
     */
    public String saveLogsToFile(List<String> logs) {
        try {
            // Создаем директорию для логов, если она не существует
            Path logsDir = Paths.get(LOGS_DIR);
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            // Создаем имя файла с текущей датой и временем
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = String.format("%s/brt_logs_%s.csv", LOGS_DIR, timestamp);

            // Сохраняем логи в файл
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                for (String log : logs) {
                    writer.write(log);
                    writer.newLine();
                }
            }

            log.info("Logs saved to file: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to save logs to file", e);
            return null;
        }
    }

    /**
     * Проверяет наличие определенного сообщения в логах
     * @param message Сообщение для поиска
     * @param secondsBack За сколько секунд искать
     * @return true если сообщение найдено
     */
    public boolean containsLogMessage(String message, int secondsBack) {
        List<String> logs = getContainerLogs(1000);
        boolean contains = logs.stream().anyMatch(log -> log.contains(message));
        log.info("Checking for message '{}' in logs. Found: {}", message, contains);

        // Сохраняем логи в файл
        String logFile = saveLogsToFile(logs);
        if (logFile != null) {
            log.info("Logs saved to file: {}", logFile);
        }

        return contains;
    }

    /**
     * Очищает буфер логов контейнера
     */
    public void clearLogs() {
        try {
            String command = String.format("docker logs %s --tail 0", CONTAINER_NAME);
            log.info("Executing command to clear logs: {}", command);
            Process process = Runtime.getRuntime().exec(command);
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("Timeout while clearing container logs");
                return;
            }
            log.info("Successfully cleared container logs");
        } catch (Exception e) {
            log.error("Failed to clear container logs", e);
        }
    }

    /**
     * Записывает логи Docker напрямую в файл
     * @param maxLines Максимальное количество строк для чтения
     * @return Путь к сохраненному файлу
     */
    public String writeDockerLogsToFile(int maxLines) {
        try {
            // Создаем директорию для логов, если она не существует
            Path logsDir = Paths.get(LOGS_DIR);
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            // Создаем имя файла с текущей датой и временем
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = String.format("%s/docker_logs_%s.txt", LOGS_DIR, timestamp);

            // Формируем команду для получения логов
            String command = String.format("docker compose logs %s --tail %d", CONTAINER_NAME, maxLines);
            log.info("Executing command: {}", command);

            // Создаем процесс
            Process process = Runtime.getRuntime().exec(command);

            // Ждем завершения процесса с таймаутом
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("Timeout while reading container logs");
                return null;
            }

            // Проверяем ошибки выполнения команды
            if (process.exitValue() != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    StringBuilder errorOutput = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                    log.error("Error executing docker logs command. Exit code: {}. Error output:\n{}",
                            process.exitValue(), errorOutput);
                }
                return null;
            }

            // Читаем вывод команды и сразу записываем в файл
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                    lineCount++;
                    if (lineCount % 100 == 0) {
                        log.debug("Written {} lines so far", lineCount);
                    }
                }
                log.info("Successfully written {} lines to file: {}", lineCount, fileName);
            }

            return fileName;

        } catch (Exception e) {
            log.error("Failed to write docker logs to file", e);
            for (StackTraceElement element : e.getStackTrace()) {
                log.error("  at {}", element);
            }
            return null;
        }
    }

    /**
     * Извлекает логи BRT контейнера и сохраняет их в файл
     * @param linesCount Количество последних строк логов
     * @return Путь к сохраненному файлу
     */
    public String extractBrtLogs(int linesCount) {
        try {
            // Создаем директорию для логов, если она не существует
            Path logsDir = Paths.get(LOGS_DIR);
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            // Создаем имя файла с текущей датой и временем
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path outputPath = logsDir.resolve(String.format("brt_logs_%s.txt", timestamp));

            // Формируем команду для получения логов
            String command = String.format("docker compose logs --tail %d %s", linesCount, CONTAINER_NAME);
            log.info("Executing command: {}", command);

            // Создаем процесс с перенаправлением вывода в файл
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd", "/c", command);
            processBuilder.directory(PROJECT_ROOT.toFile());
            processBuilder.redirectOutput(outputPath.toFile());
            processBuilder.redirectErrorStream(true);

            // Запускаем процесс
            Process process = processBuilder.start();

            // Ждем завершения процесса с таймаутом
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("Timeout while reading container logs");
                return null;
            }

            // Проверяем результат выполнения
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Failed to extract logs. Exit code: {}", exitCode);
                return null;
            }

            log.info("Successfully extracted logs to file: {}", outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to extract BRT logs", e);
            for (StackTraceElement element : e.getStackTrace()) {
                log.error("  at {}", element);
            }
            return null;
        }
    }
}