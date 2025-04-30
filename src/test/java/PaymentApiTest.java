import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PaymentApiTest {

    public record PaymentRequest(
            String msisdn,
            double amount

    ) {}

    public record PaymentResponse(
            int transactionId,
            String msisdn,
            double amount,
            double newBalance,
            String transactionTime
    ) {}



    @BeforeAll
    static void setup() {

        RestAssured.baseURI = "http://crm-api.com";
    }

    @Nested
    @Tag("subscriber")
    @DisplayName("Пополнение баланса")
    class BalancePaymentTests {

        @Test
        @Tag("subscriber")
        @DisplayName("Должен вернуть 201 и новый баланс при успешном пополнении")
        void shouldReturn201AndNewBalanceWhenPaymentSuccessful() {

            var request = new PaymentRequest(
                    "79123456789",
                    100.50
            );
            var response = new PaymentResponse(
                    812,
                    "79123456789",
                    100.50,
                    1253.50,
                    "2025-04-28T18:02:52"

            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                        .post("/pay")
                    .then()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body(equalTo(response));

        }

        @Test
        @Tag("subscriber")
        @DisplayName("Должен вернуть 400 при попытке пополнить баланс отрицательной суммой")
        void shouldReturn400WhenAmountIsNegative() {

            var request = new PaymentRequest(
                    "79123456789",
                    -50.00
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/pay")
                    .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_AMOUNT"))
                    .body("message", equalTo("Amount cannot be negative"));
        }

        @Test
        @Tag("subscriber")
        @DisplayName("Должен вернуть 400 при попытке пополнить баланс несуществующего абонента")
        void shouldReturn400WhenSubscriberNotFound() {

            var request = new PaymentRequest(
                    "7912345",
                    100.50
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/pay")
                    .then()
                    .statusCode(400)
                    .body("error", equalTo("MSISDN_NOT_FOUND"))
                    .body("message", equalTo("Subscriber with msisdn 7912345 not found"));
        }
    }

    public record changeTariffRequest(
            String msisdn,
            int tariffId

    ){}

    public record changeTariffResponse(
            String msisdn,
            int tariffId,
            String tariffDate
    ){}


    @Nested
    @Tag("manager")
    @DisplayName("Смена тарифа пользователя")
    class ChangeTariffTest {

        @Test
        @Tag("manager")
        @DisplayName("Должен вернуть 201 и новый тариф при успешной смене")
        void shouldReturn201andNewTariffWhenChangeSuccessful() {

            var request = new changeTariffRequest(
                    "79123456789",
                    11
            );

            var response = new changeTariffResponse(
                    "79123456789",
                    11,
                    "2025-04-28T18:02:52"
            );



            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                        .put("/changeTariff")
                    .then()
                        .statusCode(201)
                        .body(equalTo(response))
                        ;
        }

        @Test
        @Tag("manager")
        @DisplayName("Должен вернуть 400 при смене на не существующий тариф")
        void shouldReturn400WhenTariffIsInvalid() {
            // 1. Red: Тест падает, так как валидация формата суммы еще не реализована
            var request = new changeTariffRequest(
                    "79123456789",
                    13
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                        .put("/changeTariff")
                    .then()
                        .statusCode(400)
                        .body("error", equalTo("TARIFF ID NOT FOUND"))
                        .body("message", containsString("Tariff with id 13 not found"));
        }
    }

    public record addSubscriberRequest(
            String subscriberName,
            String msisdn,
            int tarrifId
    ){}

    public record addSubscriberResponse(
            int subscriberId,
            String subscriberName,
            String msisdn,
            int tariffId

    ){}

    @Nested
    @Tag("subscriber")
    @DisplayName("Добавление пользователя")
    class AddSubscriberTest {
        //нужен ли в responce баланс
        @Test
        @Tag("subscriber")
        @DisplayName("Должен вернуть 201 и id абонента, при успешном добавлении нового абонента")
        void shouldReturn201AndSubscriberIdWhenAddSuccseful(){
            var request = new addSubscriberRequest(
                    "Иванов Иван Иванович",
                    "79123456789",
                    11
            );

            var response = new addSubscriberResponse(
                    52,
                    "Иванов Иван Иванович",
                    "79123456789",
                    11
            );
            given().
                    contentType(ContentType.JSON).
                    body(request).
                    when().
                        post("/save").
                    then().
                    statusCode(201).
                    body(equalTo(response));
        }

        @Test
        @Tag("subscriber")
        @DisplayName("Должен вернуть 400 при неправильном id тарифа  ")
        void shouldReturn500WhenServerUnavailable(){
            var request = new addSubscriberRequest(
                    "Иванов Иван Иванович",
                    "79123456789",
                    13
            );

            given().
                    contentType(ContentType.JSON).
                    body(request).
                    when().
                    post("/save").
                    then().
                    statusCode(400).
                    body("error", equalTo("TARIFF ID NOT FOUND ")).
                    body("message", equalTo("Tariff with id 13 not found"));
        }
    }


}