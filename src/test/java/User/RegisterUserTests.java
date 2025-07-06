package User;

import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ExtendWith(AllureJunit5.class)
public class RegisterUserTests {
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
    @Step("Удаление созданных пользователей")
    public void tearDown() {
        if (accessToken != null) {
            deleteUser(accessToken);
        }
    }

    @Test
    @DisplayName("Успешное создание пользователя")
    @Description("Cоздание пользователя с валидными данными и проверка кода и тела ответа")
    public void createNewUserAndCheckResponse() {
        User user = new User(email, password, name);
        Response response = createUser(user);
        validateUserCreated(response);
    }

    @Test
    @DisplayName("Создание двух одинаковых пользователей")
    @Description("Создаем одного пользователя с валидными данными, а затем пробуем создать точно такого же. В результате должна быть ошибка.")
    public void createTwoSameUsers() {
        User user = new User(email, password, name);
        Response first = createUser(user);
        validateUserCreated(first);

        Response second = createUser(user);
        validateDuplicateUserError(second);
    }

    @Test
    @DisplayName("Создание пользователя без почты")
    @Description("Попытка создать юзера без ввода почты. В результате должна быть ошибка.")
    public void createUserWithoutEmail() {
        User user = new User("", password, name);
        Response response = createUser(user);
        validateMissingFieldError(response);
    }

    @Test
    @DisplayName("Создание пользователя без пароля")
    @Description("Попытка создать юзера без ввода пароля. В результате должна быть ошибка.")
    public void createUserWithoutPassword() {
        User user = new User(email, "", name);
        Response response = createUser(user);
        validateMissingFieldError(response);
    }

    @Test
    @DisplayName("Создание пользователя без имени")
    @Description("Попытка создать юзера без ввода имени. В результате должна быть ошибка.")
    public void createCourierWithoutFirstName() {
        User user = new User(email, password, "");
        Response response = createUser(user);
        validateMissingFieldError(response);
    }

    @Step("Создание пользователя: {user.email}, {user.password}, {user.name}")
    private Response createUser(User user) {
        return given()
                .header("Content-type", "application/json")
                .body(user)
                .when()
                .post("/api/auth/register");
    }

    @Step("Проверка, что юзер успешно создан")
    private void validateUserCreated(Response response) {
        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");

        response.then().statusCode(200)
                .body("success", equalTo(true))
                .body("user", not(emptyString()))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", not(emptyOrNullString()))
                .body("refreshToken", not(emptyOrNullString()));

        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");

        Assertions.assertTrue(accessToken.startsWith("Bearer "), "accessToken должен начинаться с 'Bearer '");

        Object userAccessToken = response.path("accessToken");
        if (userAccessToken != null) {
            accessToken = String.valueOf(userAccessToken);
        }
    }

    @Step("Удаление пользователя по accessToken = {accessToken}")
    private void deleteUser(String accessToken) {
        given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(202);
    }

    @Step("Проверка ошибки: пользователь уже существует")
    private void validateDuplicateUserError(Response response) {
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    @Step("Проверка ошибки: отсутствует обязательное поле")
    private void validateMissingFieldError(Response response) {
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }
}

