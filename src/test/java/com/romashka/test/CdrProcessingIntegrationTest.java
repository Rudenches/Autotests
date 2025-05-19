package com.romashka.test;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.awaitility.Awaitility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    classes = TestApplication.class,
    properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@Testcontainers
public class CdrProcessingIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(CdrProcessingIntegrationTest.class);

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageHandler messageHandler;

    @DynamicPropertySource
    static void configureRabbitMQ(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    @RabbitListener(queues = TestConfig.CDR_QUEUE)
    public void handleCdrMessage(Message message) {
        log.info("Received CDR message: {}", message);
        // Здесь можно добавить логику обработки CDR сообщения
    }

    @RabbitListener(queues = TestConfig.HRS_TO_BRT_QUEUE)
    public void handleHrsResponse(Message message) {
        log.info("Received HRS response: {}", message);
        if (message != null && message.getBody() != null) {
            String response = new String(message.getBody());
            log.info("Processing HRS response: {}", response);
            messageHandler.setHrsResponse(response);
        }
    }

    @Test
    public void testCompleteCdrProcessingFlow() throws IOException {
        // Create test CDR file
        String cdrContent = "call_type,caller_number,contact_number,start_time,end_time\n" +
                "01,79001112233,79872332221,2025-05-05T14:21:52.0190924,2025-05-05T14:31:42.0190924\n" +
                "02,79872332221,79001112233,2025-05-05T14:21:52.0190924,2025-05-05T14:31:42.0190924\n" +
                "01,79002223344,79178748447,2025-05-11T10:04:52.0340908,2025-05-11T10:06:43.0340908\n" +
                "02,79178748447,79002223344,2025-05-11T10:04:52.0340908,2025-05-11T10:06:43.0340908";
        Path cdrPath = Paths.get("test.csv");
        Files.write(cdrPath, cdrContent.getBytes());

        // Clear previous response
        messageHandler.clearHrsResponse();

        // Send CDR content to queue
        log.info("Sending CDR content to queue: {}", TestConfig.CDR_QUEUE);
        log.info("CDR content: {}", cdrContent);
        rabbitTemplate.convertAndSend(TestConfig.CDR_EXCHANGE, TestConfig.CDR_ROUTING_KEY, cdrContent);
        log.info("CDR content sent successfully");

        // Wait for HRS response
        log.info("Waiting for HRS response...");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String response = messageHandler.getHrsResponse();
                    log.info("Current HRS response: {}", response);
                    assertNotNull(response, "No response received from HRS service");
                });

        // Clean up
        Files.deleteIfExists(cdrPath);
    }
} 