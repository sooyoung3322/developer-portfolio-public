package com.portfolio.backend.chat;

import com.portfolio.backend.PortfolioBackendApplication;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.ConfigurableApplicationContext;
import static org.assertj.core.api.Assertions.*;

class ChatPersistenceTests {
    static final class MutableClock extends Clock {
        Instant now=Instant.parse("2026-10-21T10:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return now;}
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class Configuration {
        @Bean @Primary MutableClock persistentTestClock(){return new MutableClock();}
    }

    ConfigurableApplicationContext start(Path database) {
        // File-backed H2 survives application/pool shutdown; never connects to MySQL/Gemini.
        return new SpringApplication(PortfolioBackendApplication.class,Configuration.class).run(
            "--spring.profiles.active=prod", "--spring.main.web-application-type=servlet", "--server.port=0",
            "--spring.datasource.url=jdbc:h2:file:"+database.toAbsolutePath().toString().replace('\\','/')+";MODE=MySQL",
            "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa", "--spring.datasource.password=",
            "--analytics.allowed-origin=https://portfolio.example.com", "--ai.enabled=true", "--ai.free-tier-confirmed=true",
            "--ai.api-key=synthetic-test-value", "--ai.model=gemini-3.5-flash-lite",
            "--ai.free-tier.confirmed-model=gemini-3.5-flash-lite", "--ai.daily-limit=2",
            "--ai.free-rpd=2", "--ai.free-rpm=1", "--ai.free-tpm=20000", "--ai.cleanup-ms=86400000");
    }

    @Test void restartRetainsFailedReservationsCooldownAndConcurrentDailyLimit() throws Exception {
        var parent=Path.of("build","safety-test-db");Files.createDirectories(parent);
        var directory=Files.createTempDirectory(parent,"restart-");
        try {
            var database=directory.resolve("usage");
            LocalDate day;
            try (var app=start(database)) {
                var usage=app.getBean(UsageService.class);
                day=usage.admit(true).date();
                assertThat(usage.reserveApi(day)).isTrue();
                usage.finish(day,"RATE_LIMITED","general",null,null,null,null,10,true,false);
            }
            try (var app=start(database)) {
                var usage=app.getBean(UsageService.class);
                var clock=app.getBean(MutableClock.class);
                var rows=app.getBean(ApiUsageRepository.class);
                assertThat(rows.findAll().getFirst().getApiRequestCount()).isEqualTo(1);
                assertThat(rows.findAll().getFirst().getAdmittedCount()).isEqualTo(1);
                assertThat(usage.reserveApiDetailed(day).code()).isEqualTo("GEMINI_RATE_LIMITED");
                clock.now=clock.now.plusSeconds(61);
                try (var pool=Executors.newFixedThreadPool(8)) {
                    var admissions=new ArrayList<Callable<Boolean>>();
                    for(int i=0;i<16;i++)admissions.add(()->usage.admit(true).allowed());
                    long allowed=0;for(var result:pool.invokeAll(admissions))if(result.get())allowed++;
                    assertThat(allowed).isEqualTo(1);
                    var reservations=new ArrayList<Callable<Boolean>>();
                    for(int i=0;i<16;i++)reservations.add(()->usage.reserveApi(day));
                    allowed=0;for(var result:pool.invokeAll(reservations))if(result.get())allowed++;
                    assertThat(allowed).isEqualTo(1);
                }
                clock.now=clock.now.plusSeconds(61);
                assertThat(usage.reserveApiDetailed(day).code()).isEqualTo("APP_DAILY_LIMITED");
                assertThat(rows.findAll().getFirst().getApiRequestCount()).isEqualTo(2);
            }
            try (var app=start(database)) {
                var usage=app.getBean(UsageService.class);
                assertThat(usage.admit(true).allowed()).isFalse();
                var row=app.getBean(ApiUsageRepository.class).findAll().getFirst();
                assertThat(row.getApiRequestCount()).isEqualTo(2);
                assertThat(row.getAdmittedCount()).isEqualTo(2);
                assertThat(row.getPromptTokens()).isNull();
            }
        } finally {
            try (var files=Files.walk(directory)) {
                for(var file:files.sorted(Comparator.reverseOrder()).toList())Files.delete(file);
            }
        }
    }
}
