package com.portfolio.backend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioBackendApplicationTests {
    private static ServletWebServerApplicationContext context;
    private static String baseUrl;
    private static final HttpClient client = HttpClient.newHttpClient();

    // Start the actual server directly: Spring Framework 7's SpringExtension
    // requires JUnit 6 APIs, while this project deliberately uses JUnit 5.
    @BeforeAll
    static void startApplication() {
        context = (ServletWebServerApplicationContext) SpringApplication.run(
                PortfolioBackendApplication.class,
                "--server.port=0", "--spring.profiles.active=default");
        baseUrl = "http://localhost:" + context.getWebServer().getPort();
    }

    @AfterAll
    static void stopApplication() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void defaultProfileStartsWithoutDatabase() {
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
    }

    @Test
    void healthIsPublic() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/health"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                value -> assertThat(value).startsWith("application/json"));
        assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void otherEndpointsRequireAuthentication() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/private"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void statisticsAreDeniedOutsideLocalEvenOnLoopback() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/analytics/summary"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isIn(401, 403);
    }

    @Test
    void csrfProtectionRemainsEnabled() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/health"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(403);
    }
    @Test void chatbotDisabledWithoutKeyAndStatsDeniedOutsideLocal() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/portfolio-chat/status")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);assertThat(response.body()).contains("false");
        response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/ai-admin/summary")).GET().build(), HttpResponse.BodyHandlers.ofString());assertThat(response.statusCode()).isIn(401,403);
    }
}
