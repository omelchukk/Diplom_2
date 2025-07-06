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

public class CreateOrderTests {
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
    @DisplayName("Успешное создание заказа с авторизацией и валидными ингредиентами")
    @Description("Проверяет, что авторизованный пользователь может создать заказ с валидными ингредиентами")
    public void testCreateOrderSuccessfullyWithAuth() {
        createUser(email, password, name);

        List<String> ingredients = Arrays.asList(
                "61c0c5a71d1f82001bdaaa6d",
                "61c0c5a71d1f82001bdaaa6f"
        );

        Response response = sendCreateOrderRequest(ingredients, accessToken);

        successfulOrderPlacement(response);
    }

    @Test
    @DisplayName("Создание заказа без авторизации")
    @Description("Проверка ошибки 401 при попытке сделать заказ без авторизации")
    public void createOrderWithoutAuth() {

        List<String> ingredients = Arrays.asList(
                "61c0c5a71d1f82001bdaaa6d",
                "61c0c5a71d1f82001bdaaa6f"
        );

        Response response = sendCreateOrderRequest(ingredients, accessToken);
        orderPlacementWithoutAuth(response);
    }

    @Test
    @DisplayName("Создание заказа без ингредиентов")
    @Description("Проверка ошибки 400 при попытке сделать заказ без ингредиентов")
    public void createOrderWithoutIngredients() {

        List<String> ingredients = Arrays.asList(

        );

        Response response = sendCreateOrderRequest(ingredients, accessToken);
        orderPlacementWithoutIngredients(response);
    }

    @Test
    @DisplayName("Создание заказа с невалидным хэшем")
    @Description("Проверка ошибки 500 при попытке сделать заказ с невалидным хэшем заказов")
    public void createOrderWithIncorrectHash() {

        List<String> ingredients = Arrays.asList(
                "Incorrect Hash",
                "Incorrect Hash"
        );

        Response response = sendCreateOrderRequest(ingredients, accessToken);
        orderPlacementWithIncorrectHash(response);
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

    @Step("Успешное создание заказа")
    private Response sendCreateOrderRequest(List<String> ingredients, String token) {
        if (token == null) token = "";

        Map<String, Object> body = new HashMap<>();
        body.put("ingredients", ingredients);

        return given()
                .header("Authorization", token)
                .header("Content-type", "application/json")
                .body(body)
                .when()
                .post("/api/orders");
    }


    @Step("Проверка успешного создания заказа")
    private void successfulOrderPlacement(Response response) {
        response.then().statusCode(200)
                .body("success", equalTo(true));

        System.out.println(response.getBody().asString());
    }

    @Step("Проверка создания заказа без авторизации")
    private void orderPlacementWithoutAuth(Response response) {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true));

        System.out.println(response.getBody().asString());
    }

    @Step("Проверка создания заказа без ингредиентов")
    private void orderPlacementWithoutIngredients(Response response) {
        response.then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));

    }

    @Step("Проверка создания заказа c неверным хэшем ингредиентов")
    private void orderPlacementWithIncorrectHash(Response response) {
        response.then()
                .statusCode(500)
                .onFailMessage("Internal server error");

    }
    }
