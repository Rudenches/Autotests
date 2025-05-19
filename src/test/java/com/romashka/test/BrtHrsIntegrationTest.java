package com.romashka.test;



import com.romashka.CallType;
import com.romashka.NetworkType;

import com.romashka.test.model.BillingMessage;
import com.romashka.test.util.ContainerLogReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
public class BrtHrsIntegrationTest {
    private static final String BRT_URL = "jdbc:postgresql://localhost:5432/brt_db";
    private static final String BRT_USER = "brt_user";
    private static final String BRT_PASSWORD = "brt_pass";
    private static final String HRS_URL = "jdbc:postgresql://localhost:5433/hrs_db";
    private static final String HRS_USER = "hrs_user";
    private static final String HRS_PASSWORD = "hrs_pass";
    private Connection brt_connection;
    private Statement brt_statement;

    private Connection hrs_connection;
    private Statement hrs_statement;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageHandler messageHandler;
    @Autowired
    private ContainerLogReader logReader;

    @BeforeEach
    void setUp() throws SQLException, ClassNotFoundException {
        // Очищаем очереди перед каждым тестом
//        rabbitTemplate.execute(channel -> {
//            channel.queuePurge(TestConfig.BRT_TO_HRS_QUEUE);
//            channel.queuePurge(TestConfig.HRS_TO_BRT_QUEUE);
//            return null;
//        });
        messageHandler.clearHrsResponse();
        logReader.clearLogs();
        Class.forName("org.postgresql.Driver");
        brt_connection = DriverManager.getConnection(BRT_URL, BRT_USER, BRT_PASSWORD);
        brt_statement = brt_connection.createStatement();

        hrs_connection = DriverManager.getConnection(HRS_URL, HRS_USER, HRS_PASSWORD);
        hrs_statement = hrs_connection.createStatement();
    }

    @Test
    void testBrtToHrsMonthTarification() throws InterruptedException, IOException {
        // Создаем тестовое сообщение
        BillingMessage message = new BillingMessage();
        message.setCallerId((long)2);
        message.setRateId((long)12);
        message.setDurationMinutes((long)10);
        message.setCallType(CallType.OUTGOING);
        message.setNetworkType(NetworkType.INTERNAL);
        Map<String, Double> resources = new HashMap<>();
        resources.put("minutes", 50.0);
        message.setResources(resources);

        try {
            String current_balance = "SELECT current_balance FROM caller_resources where caller_id = '2' ";
            ResultSet currentSet = brt_statement.executeQuery(current_balance);

            if (!currentSet.next()) {
                throw new SQLException("No balance found for caller_id = 2");
            }
            double initialBalance = currentSet.getDouble("current_balance");

            // Отправляем сообщение в HRS
            log.info("Sending message to HRS: {}", message);
            rabbitTemplate.convertAndSend(
                    TestConfig.BRT_TO_HRS_EXCHANGE,
                    TestConfig.BRT_TO_HRS_ROUTING_KEY,
                    message
            );



            // Ждем ответа от HRS
            TimeUnit.SECONDS.sleep(2);
            String logFile = logReader.extractBrtLogs(300);
            List<String> logs = Files.readAllLines(Path.of(logFile));

            String new_balance = "SELECT current_balance FROM caller_resources where caller_id = '2' ";
            ResultSet new_resultSet = brt_statement.executeQuery(new_balance);
            if (!new_resultSet.next()) {
                throw new SQLException("No new balance found for caller_id = 2");
            }
            double finalBalance = new_resultSet.getDouble("current_balance");

            String expected_result = String.valueOf(initialBalance - message.getDurationMinutes());
            log.info("expected_result: {}", expected_result);

            assertTrue(logs.stream()
                            .anyMatch(log -> log.contains("Получен запрос от HRS")),
                    "Ошибка");
            assertEquals(expected_result, String.valueOf(finalBalance));

            // Закрываем ресурсы
            new_resultSet.close();
            currentSet.close();

        } catch (Exception e) {
            System.out.println("Ошибка при работе с базой данных:");
            e.printStackTrace();
        }
    }
    @Test
    void testBrtToHrsMonthTarificationWhenBalanceIsZero() throws InterruptedException, IOException {
        // Создаем тестовое сообщение
        BillingMessage message = new BillingMessage();
        message.setCallerId((long)5);
        message.setRateId((long)12);
        message.setDurationMinutes((long)10);
        message.setCallType(CallType.OUTGOING);
        message.setNetworkType(NetworkType.INTERNAL);
        Map<String, Double> resources = new HashMap<>();
        resources.put("minutes", 50.0);
        message.setResources(resources);

        try {


            String rate_id = "Select rate_id from callers where caller_id ='5'";
            ResultSet currentRateIdSet = brt_statement.executeQuery(rate_id);
            if (!currentRateIdSet.next()) {
                throw new SQLException("No rate_id found for caller_id = 5");
            }
            int currentRate = currentRateIdSet.getInt("rate_id");

            String currentMoneyBalance = "SELECT balance FROM callers where caller_id = '5' ";
            ResultSet currentMoneySet = brt_statement.executeQuery(currentMoneyBalance);

            if (!currentMoneySet.next()) {
                throw new SQLException("No balance found for caller_id = 5");
            }
            double initialMoneyBalance = currentMoneySet.getDouble("balance");




            // Отправляем сообщение в HRS
            log.info("Sending message to HRS: {}", message);
            rabbitTemplate.convertAndSend(
                    TestConfig.BRT_TO_HRS_EXCHANGE,
                    TestConfig.BRT_TO_HRS_ROUTING_KEY,
                    message
            );



            // Ждем ответа от HRS
            TimeUnit.SECONDS.sleep(2);
            String logFile = logReader.extractBrtLogs(300);
            List<String> logs = Files.readAllLines(Path.of(logFile));

            String new_rate_id = "SELECT rate_id FROM callers where caller_id = '5' ";
            ResultSet newRateIdSet = brt_statement.executeQuery(new_rate_id);
            if (!newRateIdSet.next()) {
                throw new SQLException("No new rate_id found for caller_id = 5");
            }
            int finalRateId = newRateIdSet.getInt("rate_id");

            String newMinuteBalance = "SELECT current_balance FROM caller_resources where caller_id = '5' ";
            ResultSet new_resultSet = brt_statement.executeQuery(newMinuteBalance);
            if (!new_resultSet.next()) {
                throw new SQLException("No new balance found for caller_id = 5");
            }
            double finalMinuteBalance = new_resultSet.getDouble("current_balance");

            String call_cost = "SELECT call_cost FROM call_cost where rate_id = '11' and call_type = '01' and network_type = 'INTERNAL' ";
            ResultSet costSet = hrs_statement.executeQuery(call_cost);
            if (!costSet.next()) {
                throw new SQLException("No call cost found for the specified parameters");
            }
            double callCost = costSet.getDouble("call_cost");

            String newMoneyBalance = "SELECT balance FROM callers where caller_id = '5' ";
            ResultSet newMoneySet = brt_statement.executeQuery(newMoneyBalance);
            if (!newMoneySet.next()) {
                throw new SQLException("No balance found for caller_id = 7");
            }
            double finalMoneyBalance = newMoneySet.getDouble("balance");

            double expectedMoneyBalance = initialMoneyBalance - callCost * message.getDurationMinutes();



            assertNotEquals(finalRateId,currentRate);
            assertTrue(finalMinuteBalance == 0 );

            assertTrue(logs.stream()
                            .anyMatch(log -> log.contains("Получен запрос от HRS")),
                    "Ошибка");
            assertEquals(expectedMoneyBalance, String.valueOf(finalMoneyBalance));

            // Закрываем ресурсы
            new_resultSet.close();
            //currentSet.close();

        } catch (Exception e) {
            System.out.println("Ошибка при работе с базой данных:");
            e.printStackTrace();
        }
    }

    @Test
    void testBrtToHrsMonthTarificationWhenBalanceLessThanDuration() throws InterruptedException, IOException {
        // Создаем тестовое сообщение
        BillingMessage message = new BillingMessage();
        message.setCallerId((long)7);
        message.setRateId((long)12);
        message.setDurationMinutes((long)20);
        message.setCallType(CallType.OUTGOING);
        message.setNetworkType(NetworkType.INTERNAL);
        Map<String, Double> resources = new HashMap<>();
        resources.put("minutes", 50.0);
        message.setResources(resources);

        try {
            String currentMinuteBalance = "SELECT current_balance FROM caller_resources where caller_id = '7' ";
            ResultSet currentSet = brt_statement.executeQuery(currentMinuteBalance);

            if (!currentSet.next()) {
                throw new SQLException("No balance found for caller_id = 7");
            }
            double initialMinuteBalance = currentSet.getDouble("current_balance");

            String rate_id = "Select rate_id from callers where caller_id ='7'";
            ResultSet currentRateIdSet = brt_statement.executeQuery(rate_id);
            if (!currentRateIdSet.next()) {
                throw new SQLException("No rate_id found for caller_id = 7");
            }
            int currentRate = currentRateIdSet.getInt("rate_id");

            String currentMoneyBalance = "SELECT balance FROM callers where caller_id = '7' ";
            ResultSet currentMoneySet = brt_statement.executeQuery(currentMoneyBalance);

            if (!currentMoneySet.next()) {
                throw new SQLException("No balance found for caller_id = 7");
            }
            double initialMoneyBalance = currentMoneySet.getDouble("balance");




            // Отправляем сообщение в HRS
            log.info("Sending message to HRS: {}", message);
            rabbitTemplate.convertAndSend(
                    TestConfig.BRT_TO_HRS_EXCHANGE,
                    TestConfig.BRT_TO_HRS_ROUTING_KEY,
                    message
            );



            // Ждем ответа от HRS
            TimeUnit.SECONDS.sleep(2);
            String logFile = logReader.extractBrtLogs(300);
            List<String> logs = Files.readAllLines(Path.of(logFile));

            String new_rate_id = "SELECT rate_id FROM callers where caller_id = '7' ";
            ResultSet newRateIdSet = brt_statement.executeQuery(new_rate_id);
            if (!newRateIdSet.next()) {
                throw new SQLException("No new rate_id found for caller_id = 7");
            }
            int finalRateId = newRateIdSet.getInt("rate_id");

            String newMinuteBalance = "SELECT current_balance FROM caller_resources where caller_id = '7' ";
            ResultSet new_resultSet = brt_statement.executeQuery(newMinuteBalance);
            if (!new_resultSet.next()) {
                throw new SQLException("No new balance found for caller_id = 7");
            }
            double finalMinuteBalance = new_resultSet.getDouble("current_balance");

            String call_cost = "SELECT call_cost FROM call_cost where rate_id = '11' and call_type = '01' and network_type = 'INTERNAL' ";
            ResultSet costSet = hrs_statement.executeQuery(call_cost);
            if (!costSet.next()) {
                throw new SQLException("No call cost found for the specified parameters");
            }
            double callCost = costSet.getDouble("call_cost");

            String newMoneyBalance = "SELECT balance FROM callers where caller_id = '7' ";
            ResultSet newMoneySet = brt_statement.executeQuery(newMoneyBalance);
            if (!newMoneySet.next()) {
                throw new SQLException("No balance found for caller_id = 7");
            }
            double finalMoneyBalance = newMoneySet.getDouble("balance");

            double remainingMinutes = message.getDurationMinutes() - initialMinuteBalance;

            double expectedMoneyBalance = initialMoneyBalance - callCost * remainingMinutes;




            assertEquals(0,finalMinuteBalance);
            assertNotEquals(finalRateId,currentRate);


            assertTrue(logs.stream()
                            .anyMatch(log -> log.contains("Получен запрос от HRS")),
                    "Ошибка");
            assertEquals(expectedMoneyBalance, finalMoneyBalance);

            // Закрываем ресурсы
            new_resultSet.close();
            currentSet.close();

        } catch (Exception e) {
            System.out.println("Ошибка при работе с базой данных:");
            e.printStackTrace();
        }
    }

    @Test
    void testBrtToHrsClassicTarification() throws InterruptedException, IOException {
        // Создаем тестовое сообщение
        BillingMessage message = new BillingMessage();
        message.setCallerId((long)1);
        message.setRateId((long)11);
        message.setDurationMinutes((long)10);
        message.setCallType(CallType.OUTGOING);
        message.setNetworkType(NetworkType.INTERNAL);
        Map<String, Double> resources = new HashMap<>();
        resources.put("minutes", 0.0);
        message.setResources(resources);


        try {




            String current_balance = "SELECT balance FROM callers where caller_id = '1' ";
            ResultSet currentSet = brt_statement.executeQuery(current_balance);
            if (!currentSet.next()) {
                throw new SQLException("No balance found for caller_id = 1");
            }
            double initialBalance = currentSet.getDouble("balance");

            // Отправляем сообщение в HRS
            log.info("Sending message to HRS: {}", message);
            rabbitTemplate.convertAndSend(
                    TestConfig.BRT_TO_HRS_EXCHANGE,
                    TestConfig.BRT_TO_HRS_ROUTING_KEY,
                    message
            );

            String new_balance = "SELECT balance FROM callers where caller_id = '1' ";
            ResultSet new_resultSet = brt_statement.executeQuery(new_balance);
            if (!new_resultSet.next()) {
                throw new SQLException("No new balance found for caller_id = 1");
            }
            double finalBalance = new_resultSet.getDouble("balance");

            String call_cost = "SELECT call_cost FROM call_cost where rate_id = '11' and call_type = '01' and network_type = 'INTERNAL' ";
            ResultSet costSet = hrs_statement.executeQuery(call_cost);
            if (!costSet.next()) {
                throw new SQLException("No call cost found for the specified parameters");
            }
            double callCost = costSet.getDouble("call_cost");
            System.out.println("Call cost: " + callCost);

            // Ждем ответа от HRS
            TimeUnit.SECONDS.sleep(2);
            String logFile = logReader.extractBrtLogs(300);
            List<String> logs = Files.readAllLines(Path.of(logFile));

            String expected_result = String.valueOf(initialBalance - message.getDurationMinutes() * callCost);
            log.info("expected_result: {}", expected_result);

            assertTrue(logs.stream()
                            .anyMatch(log -> log.contains("Получен запрос от HRS")),
                    "Ошибка");
            assertEquals(expected_result,String.valueOf(finalBalance));

            // Закрываем ресурсы
            costSet.close();
            new_resultSet.close();
            currentSet.close();

        } catch (Exception e) {
            System.out.println("Ошибка при работе с базой данных:");
            e.printStackTrace();
        }
        // Проверяем, что все записи были отклонены


    }

    @Test
    void testBrtToHrsClassicTarificationWhenBalanceLessThanDuration() throws InterruptedException, IOException {
        // Создаем тестовое сообщение
        BillingMessage message = new BillingMessage();
        message.setCallerId((long)1);
        message.setRateId((long)11);
        message.setDurationMinutes((long)100);
        message.setCallType(CallType.OUTGOING);
        message.setNetworkType(NetworkType.INTERNAL);
        Map<String, Double> resources = new HashMap<>();
        resources.put("minutes", 0.0);
        message.setResources(resources);


        try {




            String current_balance = "SELECT balance FROM callers where caller_id = '1' ";
            ResultSet currentSet = brt_statement.executeQuery(current_balance);
            if (!currentSet.next()) {
                throw new SQLException("No balance found for caller_id = 1");
            }
            double initialBalance = currentSet.getDouble("balance");

            // Отправляем сообщение в HRS
            log.info("Sending message to HRS: {}", message);
            rabbitTemplate.convertAndSend(
                    TestConfig.BRT_TO_HRS_EXCHANGE,
                    TestConfig.BRT_TO_HRS_ROUTING_KEY,
                    message
            );

            String new_balance = "SELECT balance FROM callers where caller_id = '1' ";
            ResultSet new_resultSet = brt_statement.executeQuery(new_balance);
            if (!new_resultSet.next()) {
                throw new SQLException("No new balance found for caller_id = 1");
            }
            double finalBalance = new_resultSet.getDouble("balance");

            String call_cost = "SELECT call_cost FROM call_cost where rate_id = '11' and call_type = '01' and network_type = 'INTERNAL' ";
            ResultSet costSet = hrs_statement.executeQuery(call_cost);
            if (!costSet.next()) {
                throw new SQLException("No call cost found for the specified parameters");
            }
            double callCost = costSet.getDouble("call_cost");
            System.out.println("Call cost: " + callCost);

            // Ждем ответа от HRS
            TimeUnit.SECONDS.sleep(2);
            String logFile = logReader.extractBrtLogs(300);
            List<String> logs = Files.readAllLines(Path.of(logFile));

            String expected_result = String.valueOf(initialBalance - message.getDurationMinutes() * callCost);
            log.info("expected_result: {}", expected_result);

            assertTrue(logs.stream()
                            .anyMatch(log -> log.contains("Получен запрос от HRS")),
                    "Ошибка");
            assertEquals(expected_result,String.valueOf(finalBalance));

            // Закрываем ресурсы
            costSet.close();
            new_resultSet.close();
            currentSet.close();

        } catch (Exception e) {
            System.out.println("Ошибка при работе с базой данных:");
            e.printStackTrace();
        }
        // Проверяем, что все записи были отклонены


    }
}