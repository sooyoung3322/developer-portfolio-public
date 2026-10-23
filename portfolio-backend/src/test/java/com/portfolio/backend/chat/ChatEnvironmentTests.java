package com.portfolio.backend.chat;

import java.net.http.HttpClient;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatEnvironmentTests {
    Map<String,Object> enabledEnvironment() {
        var env = new HashMap<String,Object>();
        env.put("AI_CHAT_ENABLED", "true");
        env.put("GEMINI_FREE_TIER_CONFIRMED", "true");
        env.put("GEMINI_API_KEY", "synthetic-test-value");
        env.put("GEMINI_MODEL", "gemini-3.5-flash-lite");
        env.put("GEMINI_FREE_TIER_MODEL", "gemini-3.5-flash-lite");
        env.put("AI_DAILY_LIMIT", "10");
        env.put("GEMINI_FREE_RPD", "20");
        env.put("GEMINI_FREE_RPM", "1");
        env.put("GEMINI_FREE_TPM", "20000");
        return env;
    }

    AnnotationConfigApplicationContext context(Map<String,Object> variables) throws Exception {
        var context = new AnnotationConfigApplicationContext();
        try {
            var sources = context.getEnvironment().getPropertySources();
            sources.remove("systemEnvironment");
            sources.remove("systemProperties");
            var loader = new YamlPropertySourceLoader();
            sources.addFirst(loader.load("common", new ClassPathResource("application.yml")).getFirst());
            sources.addFirst(loader.load("prod", new ClassPathResource("application-prod.yml")).getFirst());
            sources.addFirst(new SystemEnvironmentPropertySource("testEnvironment", variables));
            context.registerBean(GeminiFreeTierCatalog.class, () ->
                org.springframework.boot.context.properties.bind.Binder.get(context.getEnvironment())
                    .bind("ai.free-tier", org.springframework.boot.context.properties.bind.Bindable.of(GeminiFreeTierCatalog.class)).get());
            context.register(ChatSettings.class);
            context.refresh();
            return context;
        } catch (Exception failure) {
            context.close();
            throw failure;
        }
    }

    void assertNoCalls(ChatSettings settings) {
        assertThat(settings.available()).isFalse();
        var http = mock(HttpClient.class);
        assertThatThrownBy(() -> new GeminiProvider(settings, http).generate("Public project", List.of()))
            .isInstanceOf(ChatProvider.Failure.class);
        verifyNoInteractions(http);
        var service = mock(ChatService.class);
        @SuppressWarnings("unchecked") var services = (ObjectProvider<ChatService>) mock(ObjectProvider.class);
        when(services.getIfAvailable()).thenReturn(service);
        var limiter = mock(ChatSessionLimiter.class);
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = new ChatController(settings, services, limiter).ask(new ChatDtos.Request("Public project",false),request);
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().code()).isEqualTo("DISABLED");
        verifyNoInteractions(service,limiter,request);
    }

    @Test void allNineEnvironmentVariablesBindThroughActualYaml() throws Exception {
        try (var context = context(enabledEnvironment())) {
            var settings = context.getBean(ChatSettings.class);
            assertThat(settings.available()).isTrue();
            assertThat(settings.key).isEqualTo("synthetic-test-value");
            assertThat(settings.model).isEqualTo("gemini-3.5-flash-lite");
            assertThat(settings.confirmedModel).isEqualTo(settings.model);
            assertThat(settings.daily).isEqualTo(10);
            assertThat(settings.rpd).isEqualTo(20);
            assertThat(settings.rpm).isEqualTo(1);
            assertThat(settings.tpm).isEqualTo(20000);
            assertThat(settings.spacingMs()).isEqualTo(60000);
        }
    }

    @Test void deploymentExampleStartsDisabledWithoutSecretOrQuota() throws Exception {
        var properties=new Properties();
        try (var reader=java.nio.file.Files.newBufferedReader(java.nio.file.Path.of("src","test","resources","chat-disabled.env.example"))) {
            properties.load(reader);
        }
        var variables=new HashMap<String,Object>();
        properties.forEach((key,value)->variables.put((String)key,value));
        try (var context=context(variables)) {
            var settings=context.getBean(ChatSettings.class);
            assertThat(settings.enabled).isFalse();assertThat(settings.freeConfirmed).isFalse();
            assertThat(settings.key).isEmpty();assertThat(settings.daily).isZero();
            assertNoCalls(settings);
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"AI_CHAT_ENABLED","GEMINI_API_KEY","GEMINI_FREE_TIER_CONFIRMED","GEMINI_MODEL","GEMINI_FREE_TIER_MODEL","AI_DAILY_LIMIT","GEMINI_FREE_RPD","GEMINI_FREE_RPM","GEMINI_FREE_TPM"})
    void everyMissingProductionSecurityVariablePreventsCalls(String name) throws Exception {
        var variables = enabledEnvironment();
        variables.remove(name);
        try (var context = context(variables)) { assertNoCalls(context.getBean(ChatSettings.class)); }
    }

    @ParameterizedTest
    @ValueSource(strings={"false","","yes","on","1","TRUE","garbage"})
    void switchesRequireLiteralTrue(String value) throws Exception {
        for (var name : List.of("AI_CHAT_ENABLED","GEMINI_FREE_TIER_CONFIRMED")) {
            var variables = enabledEnvironment();variables.put(name,value);
            try (var context = context(variables)) { assertNoCalls(context.getBean(ChatSettings.class)); }
        }
    }

    @Test void blankKeyAndInvalidModelsPreventCalls() throws Exception {
        for (var entry : Map.of("GEMINI_API_KEY","   ","GEMINI_MODEL","gemini-unlisted","GEMINI_FREE_TIER_MODEL","different-model").entrySet()) {
            var variables = enabledEnvironment();variables.put(entry.getKey(),entry.getValue());
            try (var context = context(variables)) { assertNoCalls(context.getBean(ChatSettings.class)); }
        }
    }

    @Test void nonPositiveNumbersAndInsufficientTokenBudgetPreventCalls() throws Exception {
        for (var name : List.of("AI_DAILY_LIMIT","GEMINI_FREE_RPD","GEMINI_FREE_RPM","GEMINI_FREE_TPM")) {
            for (var value : List.of("0","-1")) {
                var variables=enabledEnvironment();variables.put(name,value);
                try (var context=context(variables)) { assertNoCalls(context.getBean(ChatSettings.class)); }
            }
        }
        var variables=enabledEnvironment();variables.put("GEMINI_FREE_TPM","19999");
        try (var context=context(variables)) { assertNoCalls(context.getBean(ChatSettings.class)); }
    }

    @Test void malformedNumbersFailStartupRatherThanAllowUnlimitedCalls() {
        for (var name : List.of("AI_DAILY_LIMIT","GEMINI_FREE_RPD","GEMINI_FREE_RPM","GEMINI_FREE_TPM")) {
            for (var value : List.of("", "not-a-number", "2147483648")) {
                var variables=enabledEnvironment();variables.put(name,value);
                assertThatThrownBy(() -> context(variables)).isInstanceOf(org.springframework.beans.BeansException.class);
            }
        }
    }
}
