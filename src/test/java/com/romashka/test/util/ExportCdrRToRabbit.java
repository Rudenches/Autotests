package com.romashka.test.util;

import com.romashka.test.TestConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Сервис для отправки CSV файлов в RabbitMQ.
 * Читает содержимое файла и отправляет его в очередь.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportCdrRToRabbit {

    private final RabbitTemplate rabbitTemplate;

//    @Value("${rabbitmq.exchange.name}")
//    private String exchangeName;
//
//    @Value("${rabbitmq.routing.key}")
//    private String routingKey;
//


    /**
     * Отправляет содержимое CSV файла в RabbitMQ.
     *
     * @param filePath путь к CSV файлу
     * @throws IOException если возникла ошибка при чтении файла
     */
    public void sendCsvToRabbit(Path filePath) throws IOException {
        log.info("Starting to send CSV file to RabbitMQ: {}", filePath);
        
        String csvContent = Files.readString(filePath);
        System.out.println(csvContent);
        log.debug("Read CSV content, size: {} bytes", csvContent.length());

        rabbitTemplate.convertAndSend(TestConfig.CDR_EXCHANGE, TestConfig.CDR_ROUTING_KEY, csvContent);
        log.info("Successfully sent CSV file to RabbitMQ");
    }
} 