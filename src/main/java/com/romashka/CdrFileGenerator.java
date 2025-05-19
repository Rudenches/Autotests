package com.romashka;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CdrFileGenerator {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final Random RANDOM = new Random();
    private static final int MIN_CALL_DURATION = 60; // 1 minute
    private static final int MAX_CALL_DURATION = 14 * 60;

    public static void generateValidCdrFile(String filePath, int numberOfRecords) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Записываем заголовок
            writer.write("call_type,caller_number,contact_number,start_time,end_rime");
            writer.newLine();

            // Генерируем записи
            for (int i = 0; i < numberOfRecords; i++) {
                List<CdrData> record = generateValidCdrRecord();
                for (CdrData cdr : record) {
                    writer.write(String.format("%s,%s,%s,%s,%s",
                            cdr.getCallType().getCode(),
                            cdr.getCallerNumber(),
                            cdr.getContactNumber(),
                            cdr.getStartTime().format(DATE_TIME_FORMATTER),
                            cdr.getEndTime().format(DATE_TIME_FORMATTER)));
                    writer.newLine();
                }


            }

        }
    }

    private static List<CdrData> generateValidCdrRecord() {
        List<CdrData> result = new ArrayList<>();
        String callType = "0";
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(new Random().nextInt(60));
        int duration = RANDOM.nextInt(MAX_CALL_DURATION - MIN_CALL_DURATION) + MIN_CALL_DURATION;
        LocalDateTime endTime = startTime.plusSeconds(duration);
        String callerNumber = "79" + String.format("%09d", new Random().nextInt(1000000000));
        String contactNumber = "79" + String.format("%09d", new Random().nextInt(1000000000));

        generateOutgoingCdr(result,callerNumber,contactNumber,startTime,endTime);
        generateIncomingCdr(result, callerNumber, contactNumber, startTime, endTime);

        return result;
    }
    private static List<CdrData> generateOutgoingCdr (List<CdrData> result, String callerNumber, String contactNumber,
                                                        LocalDateTime startTime, LocalDateTime endTime) {
        CdrData outgoing = new CdrData();
        outgoing.setCallType(CallType.OUTGOING);
        outgoing.setCallerNumber(callerNumber);
        outgoing.setContactNumber(contactNumber);
        outgoing.setStartTime(startTime);
        outgoing.setEndTime(endTime);
        result.add(outgoing);

        return result;
    }

    private static List<CdrData> generateIncomingCdr (List<CdrData> result, String callerNumber, String contactNumber,
                                                      LocalDateTime startTime, LocalDateTime endTime) {
        CdrData outgoing = new CdrData();
        outgoing.setCallType(CallType.INCOMING);
        outgoing.setCallerNumber(contactNumber);
        outgoing.setContactNumber(callerNumber);
        outgoing.setStartTime(startTime);
        outgoing.setEndTime(endTime);
        result.add(outgoing);

        return result;
    }


}