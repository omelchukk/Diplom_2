package Order;

import User.User;
import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ExtendWith(AllureJunit5.class)
public class GetOrderListTests {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String password;
    private String name;

    @BeforeEach
    @Step("Подготовка тестовых данных")
    public void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        this.email = UUID.randomUUID() + "@test.com";
        this.password = "pass_" + UUID.randomUUID();
        this.name = "name_" + UUID.randomUUID();
    }

    @AfterEach
    @Step("Удаление созданного пользователя")
    public void tearDown() {
        if (accessToken != null) {
            deleteUser(accessToken);
        }
    }

    @Test
    @DisplayName("Получение списка заказов")
    @Description("Проверяет, что с валидными данными можно получить правильный список заказов")
    public void getOrderList() {

        createUser(email, password, name);
        Response response = sendGetOrderListRequest(accessToken);
        successfulOrderGetList(response);
    }

    @Test
    @DisplayName("Попытка получения списка заказов без авторизации")
    @Description("Проверяет, что без авторизации не получится получить список заказов")
    public void getOrderListWithoutAuth() {

        createUser(email, password, name);
        Response response = sendGetOrderListRequest(null);
        noAuthError(response);
    }

    @Step("Создание пользователя")
    private Response createUser(String email, String password, String name) {
        User user = new User(email, password, name);

        Response response = given()
                .header("Content-type", "application/json")
                .body(user)
                .when()
                .post("/api/auth/register");

        response.then().statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", not(emptyOrNullString()))
                .body("refreshToken", not(emptyOrNullString()));

        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");

        Assertions.assertTrue(accessToken.startsWith("Bearer "), "accessToken должен начинаться с 'Bearer '");

        return response;
    }

    @Step("Удаление пользователя")
    private void deleteUser(String accessToken) {
        given()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(202);
    }

    @Step("Успешное получение списка заказов")
    private Response sendGetOrderListRequest(String token) {
        if (token == null) token = "";

        return given()
                .header("Authorization", token)
                .header("Content-type", "application/json")
                .when()
                .get("/api/orders");
    }

    @Step("Проверка успешного получения списка")
    private void successfulOrderGetList(Response response) {
        response.then().statusCode(200)
                .body("success", equalTo(true));

        System.out.println(response.getBody().asString());
    }

    @Step("Попытка получения заказов без авторизации")
    private void noAuthError(Response response) {
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));

    }

}
