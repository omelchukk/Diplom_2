package User;

import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ExtendWith(AllureJunit5.class)
public class ChangeUserDataTests {

    private String email;
    private String password;
    private String name;
    private String accessToken;
    private String refreshToken;

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
    @DisplayName("Обновление информации с авторизацией")
    @Description("Проверка позитивного сценария обновления email и имени авторизованного пользователя")
    public void changeDataSuccessfullyAndCheckResponse() {
        createUser(email, password, name);

        String newEmail = UUID.randomUUID() + "@newmail.com";
        String newName = "updated_" + UUID.randomUUID();

        Map<String, String> updatedData = new HashMap<>();
        updatedData.put("email", newEmail);
        updatedData.put("name", newName);

        Response response = changeUserData(updatedData, accessToken);
        validateSuccessfulDataChange(response, newEmail, newName);
    }

    @Test
    @DisplayName("Изменение только имени")
    @Description("Проверка успешного изменения только поля name")
    public void changeOnlyName() {
        createUser(email, password, name);

        String newName = "OnlyName_" + UUID.randomUUID();

        Response response = changeUserData(Map.of(
                "name", newName
        ), accessToken);

        validateSuccessfulDataChange(response, email, newName);
    }

    @Test
    @DisplayName("Изменение только email")
    @Description("Проверка успешного изменения только поля email")
    public void changeOnlyEmail() {
        createUser(email, password, name);

        String newEmail = UUID.randomUUID() + "@onlyemail.com";

        Response response = changeUserData(Map.of(
                "email", newEmail
        ), accessToken);

        validateSuccessfulDataChange(response, newEmail, name);
    }

    @Test
    @DisplayName("Обновление информации без авторизации")
    @Description("Проверка ошибки 401 при попытке изменить данные пользователя без авторизации")
    public void changeDataWithoutAuth() {
        Map<String, String> updatedData = new HashMap<>();
        updatedData.put("email", "unauthorized@test.com");
        updatedData.put("name", "NoAuth");

        Response response = changeUserData(updatedData, null); // без токена
        validateNoAuthError(response);
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

    @Step("Обновление информации о пользователе")
    private Response changeUserData(Map<String, String> updatedData, String token) {
        if (token == null) token = "";

        return given()
                .header("Authorization", token)
                .header("Content-type", "application/json")
                .body(updatedData)
                .when()
                .patch("/api/auth/user");
    }

    @Step("Проверка успешного обновления данных: email = {expectedEmail}, name = {expectedName}")
    private void validateSuccessfulDataChange(Response response, String expectedEmail, String expectedName) {
        response.then().statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(expectedEmail))
                .body("user.name", equalTo(expectedName));
    }

    @Step("Проверка ошибки авторизации")
    private void validateNoAuthError(Response response) {
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }
}
