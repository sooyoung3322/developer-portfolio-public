package com.portfolio.backend.config;

import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.*;
import com.portfolio.backend.analytics.AnalyticsRateFilter;

@Configuration
public class SecurityConfig {
    @Bean public Clock analyticsClock() { return Clock.systemUTC(); }
    public static boolean event(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getServletPath();
        return "POST".equals(request.getMethod()) && (path.equals("/api/analytics/visits") || path.equals("/api/analytics/portfolio-markdown-download") || path.matches("/api/analytics/projects/[^/]+/(views|clicks)"));
    }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
            @Value("${analytics.allowed-origin:http://localhost:5173}") String origin) throws Exception {
        RequestMatcher events = SecurityConfig::event;
        return http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers(events, request -> "POST".equals(request.getMethod()) && request.getServletPath().equals("/api/portfolio-chat")))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(events).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/portfolio-chat").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/portfolio-chat/status").permitAll()
                        .requestMatchers("/api/ai-admin/**").access((auth, ctx) -> {
                            var request = ctx.getRequest();
                            String remote = request.getRemoteAddr();
                            boolean loopback = "127.0.0.1".equals(remote) || "::1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote);
                            return new AuthorizationDecision(environment.matchesProfiles("local") && loopback
                                && request.getHeader("Origin") == null && request.getHeader("Sec-Fetch-Site") == null
                                && request.getHeader("Forwarded") == null && request.getHeader("X-Forwarded-For") == null
                                && request.getHeader("X-Forwarded-Host") == null);
                        })
                        .requestMatchers(HttpMethod.GET, "/api/analytics/**").access((auth, ctx) -> {
                            var request = ctx.getRequest();
                            String remote = request.getRemoteAddr();
                            boolean loopback = "127.0.0.1".equals(remote) || "::1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote);
                            return new AuthorizationDecision(environment.matchesProfiles("local") && loopback
                                && request.getHeader("Forwarded") == null && request.getHeader("X-Forwarded-For") == null);
                        })
                        .anyRequest().authenticated())
                .addFilterBefore(new AnalyticsRateFilter(origin), AuthorizationFilter.class)
                .addFilterBefore(new com.portfolio.backend.chat.ChatRequestFilter(origin), AuthorizationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${analytics.allowed-origin:http://localhost:5173}") String origin) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type"));
        config.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        var chat = new CorsConfiguration(config);
        chat.setAllowCredentials(true);
        source.registerCorsConfiguration("/api/portfolio-chat", chat);
        source.registerCorsConfiguration("/api/portfolio-chat/status", chat);
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
