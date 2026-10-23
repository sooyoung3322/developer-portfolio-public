package com.portfolio.backend;

import com.portfolio.backend.chat.ChatRetention;
import com.portfolio.backend.chat.ChatService;
import com.portfolio.backend.chat.KnowledgeService;
import com.portfolio.backend.chat.UsageService;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

class ProductionProfileTests {
    @Test
    void prodStartsWithMigrationsAndRejectsLocalOnlyApis() throws Exception {
        // Isolated H2 only: no production database or Gemini requests.
        try (var context = (ServletWebServerApplicationContext) SpringApplication.run(
                PortfolioBackendApplication.class,
                "--spring.profiles.active=prod", "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:prodcheck;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--analytics.allowed-origin=https://portfolio.example.com", "--ai.enabled=false")) {
            assertThat(context.getBeansOfType(ChatService.class)).hasSize(1);
            assertThat(context.getBeansOfType(UsageService.class)).hasSize(1);
            assertThat(context.getBeansOfType(KnowledgeService.class)).hasSize(1);
            assertThat(context.getBeansOfType(ChatRetention.class)).hasSize(1);
            var pool = context.getBean(HikariDataSource.class);
            assertThat(pool.getMaximumPoolSize()).isEqualTo(4);
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            assertThat(context.getEnvironment().getProperty("server.address")).isEqualTo("127.0.0.1");
            assertThat(context.getBean(org.flywaydb.core.Flyway.class).info().applied()).hasSize(5);
            var base = "http://127.0.0.1:" + context.getWebServer().getPort();
            var client = HttpClient.newHttpClient();
            var health = client.send(HttpRequest.newBuilder(URI.create(base + "/api/health")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(health.statusCode()).isEqualTo(200);
            for (var path : new String[]{"/api/analytics/summary", "/api/ai-admin/summary"}) {
                var denied = client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
                assertThat(denied.statusCode()).isIn(401, 403);
            }
            var event = client.send(HttpRequest.newBuilder(URI.create(base + "/api/analytics/visits"))
                    .header("Origin", "https://portfolio.example.com").header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"browserId\":\"34514ab8-812f-48af-8f17-109a151803ea\",\"eventId\":\"adf14ab8-812f-48af-8f17-109a151803ea\"}")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(event.statusCode()).isBetween(200, 299);
        }
    }
}
