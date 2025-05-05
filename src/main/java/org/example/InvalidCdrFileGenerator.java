package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class InvalidCdrFileGenerator {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final Random RANDOM = new Random();
    private static final int MIN_CALL_DURATION = 60; // 1 minute
    private static final int MAX_CALL_DURATION = 14 * 60;

    public static void generateInvalidCdrFile(String filePath, InvalidType type) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            switch (type) {
                case INVALID_TIME:
                    generateTestDataWithIncorrectTime(writer);
                    break;
                case INVALID_NUMBER_FORMAT:
                    generateInvalidNumberFormatFile(writer);
                    break;
                case MISSING_FIELDS:
                    generateMissingFieldsFile(writer);
                    break;
            }
        }
    }


    private static void generateTestDataWithIncorrectTime(BufferedWriter writer) throws IOException {
        writer.write("call_type,caller_number,contact_number,start_time,end_time");
        writer.newLine();
        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {

            String callerNumber = "79" + String.format("%09d", new Random().nextInt(1000000000));
            String contactNumber = "79" + String.format("%09d", new Random().nextInt(1000000000));

            int duration = RANDOM.nextInt(MAX_CALL_DURATION - MIN_CALL_DURATION) + MIN_CALL_DURATION;

            LocalDateTime endTime = baseTime.plusMinutes(i * 15);
            LocalDateTime startTime = endTime.plusSeconds(duration); // Each pair starts 15 minutes after the previous


            CdrData outgoing = new CdrData();
            outgoing.setCallType(CallType.OUTGOING);
            outgoing.setCallerNumber(callerNumber);
            outgoing.setContactNumber(contactNumber);
            outgoing.setStartTime(startTime);
            outgoing.setEndTime(endTime);
            writer.write(String.format("%s,%s,%s,%s,%s",
                    outgoing.getCallType().getCode(),
                    outgoing.getCallerNumber(),
                    outgoing.getContactNumber(),
                    outgoing.getStartTime().format(DATE_TIME_FORMATTER),
                    outgoing.getEndTime().format(DATE_TIME_FORMATTER)));
            writer.newLine();



            CdrData incoming = new CdrData();
            incoming.setCallType(CallType.INCOMING);
            incoming.setCallerNumber(contactNumber);
            incoming.setContactNumber(callerNumber);
            incoming.setStartTime(startTime);
            incoming.setEndTime(endTime);
            writer.write(String.format("%s,%s,%s,%s,%s",
                    incoming.getCallType().getCode(),
                    incoming.getCallerNumber(),
                    incoming.getContactNumber(),
                    incoming.getStartTime().format(DATE_TIME_FORMATTER),
                    incoming.getEndTime().format(DATE_TIME_FORMATTER)));
            writer.newLine();
        }
    }

    private static void generateInvalidNumberFormatFile(BufferedWriter writer) throws IOException {
        writer.write("call_type,caller_number,contact_number,start_time,end_time");
        writer.newLine();
        // Неправильный формат номера
        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {

            String callerNumber = "79" + String.format("%04d", new Random().nextInt(10000));
            String contactNumber = "79" + String.format("%04d", new Random().nextInt(10000));

            int duration = RANDOM.nextInt(MAX_CALL_DURATION - MIN_CALL_DURATION) + MIN_CALL_DURATION;

            LocalDateTime startTime = baseTime.plusMinutes(i * 15);
            LocalDateTime endTime = startTime.plusSeconds(duration); // Each pair starts 15 minutes after the previous

            CdrData outgoing = new CdrData();
            outgoing.setCallType(CallType.OUTGOING);
            outgoing.setCallerNumber(callerNumber);
            outgoing.setContactNumber(contactNumber);
            outgoing.setStartTime(startTime);
            outgoing.setEndTime(endTime);
            writer.write(String.format("%s,%s,%s,%s,%s",
                    outgoing.getCallType().getCode(),
                    outgoing.getCallerNumber(),
                    outgoing.getContactNumber(),
                    outgoing.getStartTime().format(DATE_TIME_FORMATTER),
                    outgoing.getEndTime().format(DATE_TIME_FORMATTER)));
            writer.newLine();


            CdrData incoming = new CdrData();
            incoming.setCallType(CallType.INCOMING);
            incoming.setCallerNumber(contactNumber);
            incoming.setContactNumber(callerNumber);
            incoming.setStartTime(startTime);
            incoming.setEndTime(endTime);
            writer.write(String.format("%s,%s,%s,%s,%s",
                    incoming.getCallType().getCode(),
                    incoming.getCallerNumber(),
                    incoming.getContactNumber(),
                    incoming.getStartTime().format(DATE_TIME_FORMATTER),
                    incoming.getEndTime().format(DATE_TIME_FORMATTER)));
            writer.newLine();


        }
    }

    private static void generateMissingFieldsFile(BufferedWriter writer) throws IOException {
        writer.write("call_type,caller_number,contact_number,start_time,end_time");
        writer.newLine();
        // Неправильный формат номера
        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {

            String callerNumber = "79" + String.format("%09d", new Random().nextInt(1000000000));
            String contactNumber = "79" + String.format("%09d", new Random().nextInt(1000000000));

            int duration = RANDOM.nextInt(MAX_CALL_DURATION - MIN_CALL_DURATION) + MIN_CALL_DURATION;

            LocalDateTime startTime = baseTime.plusMinutes(i * 15);
            LocalDateTime endTime = startTime.plusSeconds(duration); // Each pair starts 15 minutes after the previous

            CdrData outgoing = new CdrData();
            //outgoing.setCallType(CallType.OUTGOING);
            outgoing.setCallerNumber(callerNumber);
            outgoing.setContactNumber(contactNumber);
            outgoing.setStartTime(startTime);
            outgoing.setEndTime(endTime);
            writer.write(String.format("%s,%s,%s,%s",
                    //outgoing.getCallType().getCode(),
                    outgoing.getCallerNumber(),
                    outgoing.getContactNumber(),
                    outgoing.getStartTime().format(DATE_TIME_FORMATTER),
                    outgoing.getEndTime().format(DATE_TIME_FORMATTER)));
            writer.newLine();


            CdrData incoming = new CdrData();
            incoming.setCallType(CallType.INCOMING);
            incoming.setCallerNumber(contactNumber);
            incoming.setContactNumber(callerNumber);
            incoming.setStartTime(startTime);
            incoming.setEndTime(endTime);
            writer.write(String.format("%s,%s,%s,%s,%s",
                    incoming.getCallType().getCode(),
                    incoming.getCallerNumber(),
                    incoming.getContactNumber(),
                    incoming.getStartTime().format(DATE_TIME_FORMATTER),
                    incoming.getEndTime().format(DATE_TIME_FORMATTER)));
            writer.newLine();


        }
    }



    public enum InvalidType {
        INVALID_TIME,
        INVALID_NUMBER_FORMAT,
        MISSING_FIELDS
    }
}