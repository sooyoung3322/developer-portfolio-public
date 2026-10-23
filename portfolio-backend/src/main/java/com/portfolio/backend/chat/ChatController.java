package com.portfolio.backend.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.portfolio.backend.chat.ChatDtos.*;

@RestController
public class ChatController {
    private final ChatSettings settings;
    private final ObjectProvider<ChatService> service;
    private final ChatSessionLimiter limiter;

    public ChatController(ChatSettings settings, ObjectProvider<ChatService> service, ChatSessionLimiter limiter) {
        this.settings = settings;
        this.service = service;
        this.limiter = limiter;
    }

    @GetMapping("/api/portfolio-chat/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", settings.available() && service.getIfAvailable() != null);
    }

    @PostMapping("/api/portfolio-chat")
    public ResponseEntity<Response> ask(@Valid @RequestBody Request request, HttpServletRequest http) {
        var chat = service.getIfAvailable();
        if (!settings.available() || chat == null) {
            ChatLog.failure("chat_disabled",
                !settings.available() ? settings.unavailableReason() : "SERVICE_UNAVAILABLE", null, false);
            return ResponseEntity.status(503).body(new Response(
                "현재 AI 챗봇을 준비 중입니다.\n이용에 양해 부탁드립니다.",
                List.of(), "DISABLED", new Limits(false, 0), false));
        }

        var result = chat.ask(request, limiter.check(http.getSession(true)));
        int status = switch (result.code()) {
            case "SESSION_RATE_LIMITED", "APP_DAILY_LIMITED", "GEMINI_RPM_LIMITED",
                 "GEMINI_RPD_LIMITED", "GEMINI_TPM_LIMITED", "GEMINI_RATE_LIMITED" -> 429;
            case "API_ERROR", "RESPONSE_PARSE_ERROR", "RESPONSE_REJECTED",
                 "KNOWLEDGE_ERROR", "CONTEXT_TOO_LARGE" -> 503;
            default -> 200;
        };
        return ResponseEntity.status(status).headers(headers -> {
            if (status == 429) {
                headers.set("Retry-After", Integer.toString(result.limits().retryAfterSeconds()));
            }
        }).body(result);
    }
}
