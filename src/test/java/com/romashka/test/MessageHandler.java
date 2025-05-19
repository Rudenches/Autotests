package com.romashka.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    
    private final AtomicReference<String> hrsResponse = new AtomicReference<>();

    public void handleCdrMessage(Message message) {
        log.info("Received CDR message: {}", message);
        if (message != null && message.getBody() != null) {
            String content = new String(message.getBody());
            log.info("CDR message content: {}", content);
        }
    }

    public void handleCdrMessage(byte[] messageBody) {
        log.info("Received CDR message bytes: {}", messageBody);
        if (messageBody != null) {
            String content = new String(messageBody);
            log.info("CDR message content: {}", content);
        }
    }

    public void handleCdrMessage(File file) {
        log.info("Received CDR file: {}", file);
        try {
            String content = Files.readString(file.toPath());
            log.info("CDR file content: {}", content);
        } catch (IOException e) {
            log.error("Error reading CDR file", e);
        }
    }

    public void handleHrsResponse(Message message) {
        log.info("Received HRS response message: {}", message);
        if (message != null && message.getBody() != null) {
            String response = new String(message.getBody());
            log.info("Processing HRS response content: {}", response);
            setHrsResponse(response);
            log.info("HRS response saved: {}", getHrsResponse());
        } else {
            log.warn("Received empty HRS response message");
        }
    }

    public void handleHrsResponse(byte[] messageBody) {
        log.info("Received HRS response bytes: {}", messageBody);
        if (messageBody != null) {
            String response = new String(messageBody);
            log.info("Processing HRS response content: {}", response);
            setHrsResponse(response);
            log.info("HRS response saved: {}", getHrsResponse());
        } else {
            log.warn("Received empty HRS response bytes");
        }
    }

    public String getHrsResponse() {
        return hrsResponse.get();
    }

    public void setHrsResponse(String response) {
        hrsResponse.set(response);
    }

    public void clearHrsResponse() {
        hrsResponse.set(null);
    }
} 