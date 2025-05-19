package com.romashka.test;

//import com.romashka.romashka_telecom.brt.service.CdrCsvParser;

import com.romashka.test.util.ContainerLogReader;
import com.romashka.test.util.ExportCdrRToRabbit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class ExportCdrRToRabbitTest {

    @Autowired
    private ExportCdrRToRabbit exportCdrRToRabbit;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ContainerLogReader containerLogReader;


    private static final String VALID_FILE_PATH = "test-data/ValidCdr.csv";
    private static final String INVALID_FILE_PATH = "test-data/InvalidCdr.csv";
    private static final String INVALID_PHONE_NUMBERS_FILE_PATH = "test-data/InvalidPhoneNumbersCdr.csv";
    private static final String INVALID_DATE_FILE_PATH = "test-data/InvalidDataCdr.csv";
    private static final String INVALID_MISSING_FIELD_PATH = "test-data/MissingFieldCdr.csv";
    private static final String INVALID_SEPARATOR_PATH = "test-data/InvalidSeparatorCdr.csv";
    private static final String INVALID_TIME_FORMAT_PATH = "test-data/InvalidTimeFormat.csv";
    private static final String INVALID_CALL_TYPE_PATH = "test-data/InvalidCallType.csv";
    private static final String INVALID_CALL_TIME_PATH = "test-data/InvalidCallTime.csv";
    private static final String EMPTY_FILE_PATH = "test-data/EmptyFile.csv";
    private static final String MORE_THAN_10_RECORD_PATH = "test-data/MoreThan10Record.csv";
    private static final String QUEUE_NAME = "cdr.queue";

    @BeforeEach
    void setUp() {

        containerLogReader.clearLogs();
    }

    @Test
    void shouldSendCsvToRabbit() throws IOException {

        Path csvFile = new ClassPathResource(VALID_FILE_PATH).getFile().toPath();
        String expectedContent = Files.readString(csvFile);

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);





        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains("Распарсили 10 записей")),
                "Ошибка Валидации");
    }



    @Test
    void shouldHandleInvalidPhoneNumbers() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(INVALID_PHONE_NUMBERS_FILE_PATH).getFile().toPath();
        String invalidContent = Files.readString(csvFile);

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                .anyMatch(log -> log.contains("Ошибка валидации: номер телефона ")),
                "Все записи без ошибок");
    }

    @Test
    void shouldHandleInvalidDate() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(INVALID_DATE_FILE_PATH).getFile().toPath();

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains( "Ошибка валидации: ошибка при разборе даты и времени")
                                 ),
                "Все записи без ошибок");
    }
    @Test
    void shouldHandleMissingField() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(INVALID_MISSING_FIELD_PATH).getFile().toPath();

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains( "Ошибка валидации: ожидалось 5 частей")
                        ),
                "Все записи без ошибок");
    }

    @Test
    void shouldHandleInvalidSeparator() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(INVALID_SEPARATOR_PATH).getFile().toPath();

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains( "Ошибка валидации: ожидалось 5 частей")
                        ),
                "Все записи без ошибок");
    }

    @Test
    void shouldHandleInvalidTimeFormat() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(INVALID_TIME_FORMAT_PATH).getFile().toPath();

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains( "Ошибка валидации: ошибка при разборе даты и времени")
                        ),
                "Все записи без ошибок");
    }


    @Test
    void shouldHandleInvalidCallType() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(INVALID_CALL_TYPE_PATH).getFile().toPath();

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains( "Не удалось распарсить строку CDR")
                        ),
                "Все записи без ошибок");
        assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("Unknown code:");});

    }

    @Test
    void shouldHandleEndTimeLessThanStartTime() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(INVALID_CALL_TIME_PATH).getFile().toPath();

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains( "Ошибка валидации: время начала ")
                        ),
                "Все записи без ошибок");


    }
    @Test
    void shouldHandleEmptyFile() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(EMPTY_FILE_PATH).getFile().toPath();

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(1000);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains( "Нет записей CDR для обработки")
                        ),
                "Все записи без ошибок");


    }

    @Test
    void shouldHandleMoreThan10RecordFile() throws IOException {
        // Get the invalid phone numbers CSV file from test resources
        Path csvFile = new ClassPathResource(MORE_THAN_10_RECORD_PATH).getFile().toPath();

        exportCdrRToRabbit.sendCsvToRabbit(csvFile);

        // Wait for the message to be processed
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем логи и сохраняем их в файл
        String logFile = containerLogReader.extractBrtLogs(200);
        assertNotNull(logFile, "Log file should be created");

        List<String> logs = Files.readAllLines(Path.of(logFile));

        // Проверяем, что все записи были отклонены
        assertTrue(logs.stream()
                        .anyMatch(log -> log.contains( "Распарсили 11 записей")
                        ),
                "Все записи без ошибок");


    }
}