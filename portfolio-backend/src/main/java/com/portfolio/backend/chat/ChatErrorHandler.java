package com.portfolio.backend.chat;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import static com.portfolio.backend.chat.ChatDtos.*;

@RestControllerAdvice(assignableTypes = ChatController.class)
public class ChatErrorHandler {
    private Response response(String code, String text) {
        return new Response(text, List.of(), code, new Limits(false, 0), false);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<Response> invalid(Exception failure) {
        ChatLog.failure("chat_invalid_request", "INVALID_QUESTION", failure, false);
        return ResponseEntity.badRequest().body(response("INVALID_QUESTION", "질문은 1~300자로 입력해 주세요."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Response> failure(Exception failure) {
        ChatLog.failure("chat_internal_failed", "SERVER_ERROR", failure, true);
        return ResponseEntity.status(503).body(response("SERVER_ERROR",
            "AI 서비스 연결에 문제가 발생했습니다.\n잠시 후 다시 시도해 주세요."));
    }
}
