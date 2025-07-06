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
public class LoginUserTests {
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
    @DisplayName("Успешная авторизация")
    @Description("Успешная авторизация с валидными данными и проверка кода и тела ответа")
    public void loginSuccessfullyAndCheckResponse() {
        createUser(email, password, name);

        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", email);
        loginData.put("password", password);

        Response response = loginUser(loginData);
        validateSuccessfulLogin(response);
    }

    @Test
    @DisplayName("Ошибка авторизации")
    @Description("Попытка авторизации с некорректным имейлом и паролем")
    public void checkUnsuccessfulLogin() {

        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", email);
        loginData.put("password", password);

        Response response = loginUser(loginData);
        checkInvalidEmailOrPassword(response);
    }

    @Step("Создание пользователя: {email}, {password}, {name}")
    private Response createUser(String email, String password, String name) {
        User user = new User(email, password, name);

        Response response = given()
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .body(user)
                .when()
                .post("/api/auth/register");

        response.then().statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", not(emptyOrNullString()))
                .body("refreshToken", not(emptyOrNullString()));

        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");

        Assertions.assertTrue(accessToken.startsWith("Bearer "), "accessToken должен начинаться с 'Bearer '");

        return response;
    }

    @Step("Удаление пользователя по accessToken = {accessToken}")
    private void deleteUser(String accessToken) {
        given()
                .header("Authorization", accessToken)
                .header("Content-type", "application/json")
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(202);
    }

    @Step("Авторизация пользователя")
    private Response loginUser(Map<String, String> loginData) {
        return given()
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .body(loginData)
                .when()
                .post("/api/auth/login");
    }

    @Step("Проверка успешной авторизации")
    private void validateSuccessfulLogin(Response response) {
        response.then().statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", not(emptyString()))
                .body("refreshToken", not(emptyString()))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name));

        accessToken = response.jsonPath().getString("accessToken");
        refreshToken = response.jsonPath().getString("refreshToken");

        Assertions.assertTrue(accessToken.startsWith("Bearer "), "accessToken должен начинаться с 'Bearer '");
    }

    @Step("Проверка ошибки авторизации")
    private void checkInvalidEmailOrPassword(Response response) {
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }
}

