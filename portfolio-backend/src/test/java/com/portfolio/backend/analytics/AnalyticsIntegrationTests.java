package com.portfolio.backend.analytics;

import com.portfolio.backend.PortfolioBackendApplication;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class AnalyticsIntegrationTests {
    static ServletWebServerApplicationContext context;
    static AnalyticsRepository events;
    static AnalyticsGuardRepository guards;
    static TransactionTemplate tx;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();
    static class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-10-17T14:59:59Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
    MutableClock clock;
    AnalyticsService service;
    @BeforeAll static void start() {
        context = (ServletWebServerApplicationContext) SpringApplication.run(PortfolioBackendApplication.class,
            "--server.port=0", "--spring.profiles.active=local",
            // The local profile does not imply analytics is enabled. Never rely
            // on the developer's ignored application-local.yml for test setup.
            "--analytics.enabled=true", "--analytics.allowed-origin=http://localhost:5173",
            "--spring.datasource.url=" + System.getProperty("analytics.test.url", "jdbc:h2:mem:analytics;MODE=MySQL;DB_CLOSE_DELAY=-1"),
            "--spring.datasource.driver-class-name=" + System.getProperty("analytics.test.driver", "org.h2.Driver"),
            "--spring.datasource.username=" + System.getProperty("analytics.test.username", "sa"),
            "--spring.datasource.password=" + System.getProperty("analytics.test.password", ""),
            "--spring.jpa.hibernate.ddl-auto=validate");
        events = context.getBean(AnalyticsRepository.class);
        guards = context.getBean(AnalyticsGuardRepository.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        base = "http://localhost:" + context.getWebServer().getPort();
        assertThat(context.getBeansOfType(AnalyticsController.class)).hasSize(1);
        assertThat(context.getBeansOfType(AnalyticsReadController.class)).hasSize(1);
    }
    @AfterAll static void stop() { if (context != null) context.close(); }
    @BeforeEach void reset() {
        events.deleteAllInBatch(); guards.deleteAllInBatch();
        clock = new MutableClock(); service = new AnalyticsService(guards, events, clock);
    }
    boolean record(String id, String slug, String event) {
        return tx.execute(status -> service.record(id, slug, event, false).counted());
    }
    @Test void repeatVisitsAndClicksAreIndependentFromVisitorsAndSessions() {
        String id = UUID.randomUUID().toString();
        for (int i=0; i<3; i++) assertThat(record(id, null, UUID.randomUUID().toString())).isTrue();
        for (int i=0; i<5; i++) {
            String event = UUID.randomUUID().toString();
            assertThat(tx.<Boolean>execute(status -> service.record(id, "project-one", event, true).counted())).isTrue();
            assertThat(tx.<Boolean>execute(status -> service.record(id, "project-one", event, true).counted())).isFalse();
        }
        assertThat(service.summary().totalVisits()).isEqualTo(3);
        assertThat(service.summary().uniqueVisitors()).isEqualTo(1);
        assertThat(service.summary().estimatedVisitSessions()).isEqualTo(1);
        assertThat(events.countByScope("click:project-one")).isEqualTo(5);
        assertThat(events.countByScope("click:project-six")).isZero();
        assertThat(service.projects().stream().filter(p -> p.projectSlug().equals("project-one")).findFirst().orElseThrow().clicks()).isEqualTo(5);
        clock.now = clock.now.plusSeconds(1800);
        record(id, null, UUID.randomUUID().toString());
        assertThat(service.summary().estimatedVisitSessions()).isEqualTo(2);
        assertThat(service.summary().uniqueVisitors()).isEqualTo(1);
    }
    @Test void concurrentDuplicateRequestsCountOnce() throws Exception {
        String id = UUID.randomUUID().toString(), event = UUID.randomUUID().toString();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(6)) {
            var jobs = new java.util.ArrayList<java.util.concurrent.Callable<Boolean>>();
            var barrier = new java.util.concurrent.CyclicBarrier(6);
            for (int i=0; i<6; i++) jobs.add(() -> { barrier.await(); return record(id, null, event); });
            int counted = 0;
            for (var result : pool.invokeAll(jobs)) if (result.get()) counted++;
            assertThat(counted).isEqualTo(1);
            assertThat(service.summary().totalVisits()).isEqualTo(1);
        }
    }
    @Test void concurrentDistinctVisitsForSameBrowserAllCount() throws Exception {
        String id = UUID.randomUUID().toString();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(6)) {
            var jobs = new java.util.ArrayList<java.util.concurrent.Callable<Boolean>>();
            var barrier = new java.util.concurrent.CyclicBarrier(6);
            for (int i=0; i<6; i++) jobs.add(() -> { barrier.await(); return record(id, null, UUID.randomUUID().toString()); });
            for (var result : pool.invokeAll(jobs)) assertThat(result.get()).isTrue();
            assertThat(service.summary().totalVisits()).isEqualTo(6);
            assertThat(service.summary().uniqueVisitors()).isEqualTo(1);
            assertThat(service.summary().estimatedVisitSessions()).isEqualTo(1);
        }
    }
    @Test void rollbackIncludesReceiptsAndVisitors() {
        String id = UUID.randomUUID().toString(), event = UUID.randomUUID().toString();
        tx.executeWithoutResult(status -> { service.record(id, null, event, false); status.setRollbackOnly(); });
        assertThat(events.count()).isZero();
        assertThat(guards.count()).isZero();
        assertThat(record(id, null, event)).isTrue();
    }
    @Test void datesViewsAndHistoricalEventsRemainAvailable() {
        events.saveAndFlush(new AnalyticsEvent("visit", clock.now.minusSeconds(86400)));
        record(UUID.randomUUID().toString(), "project-one", UUID.randomUUID().toString());
        String id = UUID.randomUUID().toString();
        record(id, null, UUID.randomUUID().toString());
        clock.now = clock.now.plusSeconds(1);
        record(id, null, UUID.randomUUID().toString());
        assertThat(service.summary().totalVisits()).isEqualTo(3);
        assertThat(service.summary().todayVisits()).isEqualTo(1);
        assertThat(service.daily(7).stream().mapToLong(AnalyticsService.Daily::visits).sum()).isEqualTo(3);
        assertThat(service.projects().getFirst().views()).isEqualTo(1);
        assertThatThrownBy(() -> record(id, "unknown", UUID.randomUUID().toString())).isInstanceOf(ResponseStatusException.class);
        context.getBean(org.flywaydb.core.Flyway.class).validate();
    }
    @Test void upgradePreservesOldEventsAndCreatesSessionBaseline() throws Exception {
        String url = "jdbc:h2:mem:upgrade" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        var old = org.flywaydb.core.Flyway.configure().dataSource(url, "sa", "").target("4").load();
        old.migrate();
        try (var c = java.sql.DriverManager.getConnection(url, "sa", ""); var sql = c.createStatement()) {
            sql.executeUpdate("INSERT INTO analytics_event(scope, occurred_at, visit_date) VALUES ('visit', CURRENT_TIMESTAMP, CURRENT_DATE), ('project-one', CURRENT_TIMESTAMP, CURRENT_DATE)");
            var upgrade = org.flywaydb.core.Flyway.configure().dataSource(url, "sa", "").load();
            assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(upgrade.migrate().migrationsExecuted).isZero();
            try (var rows = sql.executeQuery("SELECT scope, COUNT(*) FROM analytics_event GROUP BY scope")) {
                var counts = new java.util.HashMap<String, Long>();
                while (rows.next()) counts.put(rows.getString(1), rows.getLong(2));
                assertThat(counts).containsEntry("visit", 1L).containsEntry("project-one", 1L).containsEntry("visit-session", 1L).hasSize(3);
            }
        }
    }
    int request(String path, String body, String origin, String forwarded) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) b.GET(); else b.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if (origin != null) b.header("Origin", origin);
        if (forwarded != null) b.header("X-Forwarded-For", forwarded);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }
    @Test void markdownDownloadsUseReceiptsAndDoNotChangeOtherMetrics() throws Exception {
        String id = UUID.randomUUID().toString(), event = UUID.randomUUID().toString();
        assertThat(tx.<Boolean>execute(status -> service.recordMarkdownDownload(id, event).counted())).isTrue();
        assertThat(tx.<Boolean>execute(status -> service.recordMarkdownDownload(id, event).counted())).isFalse();
        assertThat(tx.<Boolean>execute(status -> service.recordMarkdownDownload(id, UUID.randomUUID().toString()).counted())).isTrue();
        assertThat(events.countByScope(AnalyticsService.MARKDOWN_DOWNLOAD)).isEqualTo(2);
        assertThat(service.summary().totalVisits()).isZero();
        assertThat(service.summary().uniqueVisitors()).isZero();
        assertThat(service.projects().stream().mapToLong(AnalyticsService.Project::views).sum()).isZero();
        record(id, null, UUID.randomUUID().toString());
        assertThat(events.countByScope(AnalyticsService.MARKDOWN_DOWNLOAD)).isEqualTo(2);
        // Fixed KST calendar boundary: today plus the preceding six dates.
        events.saveAndFlush(new AnalyticsEvent(AnalyticsService.MARKDOWN_DOWNLOAD, Instant.parse("2026-10-10T15:00:00Z")));
        events.saveAndFlush(new AnalyticsEvent(AnalyticsService.MARKDOWN_DOWNLOAD, Instant.parse("2026-10-10T14:59:59Z")));
        try (var c = context.getBean(javax.sql.DataSource.class).getConnection(); var sql = c.createStatement();
             var rows = sql.executeQuery("SELECT COUNT(*) AS total_downloads, COALESCE(SUM(CASE WHEN visit_date BETWEEN DATE '2026-10-11' AND DATE '2026-10-17' THEN 1 ELSE 0 END), 0) AS last_7_days_downloads FROM analytics_event WHERE scope = 'portfolio-markdown-download'")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong("total_downloads")).isEqualTo(4);
            assertThat(rows.getLong("last_7_days_downloads")).isEqualTo(3);
        }
    }
    @Test void publicEventsValidateInputAndReadAccessIsLocalOnly() throws Exception {
        String body = "{\"browserId\":\"" + UUID.randomUUID() + "\",\"eventId\":\"" + UUID.randomUUID() + "\"}";
        String download = "/api/analytics/portfolio-markdown-download";
        assertThat(request(download, body, null, null)).isEqualTo(200);
        assertThat(request(download, body, null, null)).isEqualTo(200);
        assertThat(events.countByScope(AnalyticsService.MARKDOWN_DOWNLOAD)).isEqualTo(1);
        assertThat(request(download, "{}", null, null)).isEqualTo(400);
        assertThat(request(download, "{bad", null, null)).isEqualTo(400);
        assertThat(request(download, null, null, "203.0.113.1")).isIn(401,403);
        assertThat(request(download + "-unknown", body, null, null)).isIn(401,403);
        assertThat(request(download, body, "https://evil.example", null)).isEqualTo(403);
        assertThat(request("/api/analytics/visits", body, "http://localhost:5173", null)).isEqualTo(200);
        assertThat(request("/api/analytics/projects/project-one/views", body, null, null)).isEqualTo(200);
        assertThat(request("/api/analytics/projects/project-one/clicks", body, null, null)).isEqualTo(200);
        assertThat(request("/api/analytics/visits", "{\"browserId\":\"" + UUID.randomUUID() + "\"}", null, null)).isEqualTo(400);
        assertThat(request("/api/analytics/projects/unknown/views", body, null, null)).isEqualTo(404);
        assertThat(request("/api/analytics/visits", "{\"browserId\":\"bad\"}", null, null)).isEqualTo(400);
        assertThat(request("/api/analytics/visits", body, "https://evil.example", null)).isEqualTo(403);
        assertThat(request("/api/analytics/summary", null, null, null)).isEqualTo(200);
        assertThat(request("/api/analytics/summary", null, null, "203.0.113.1")).isIn(401,403);
        assertThat(request("/api/analytics/daily?days=91", null, null, null)).isEqualTo(400);
    }
}
