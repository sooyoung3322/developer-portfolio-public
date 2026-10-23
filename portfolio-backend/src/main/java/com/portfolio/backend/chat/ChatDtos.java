package com.portfolio.backend.chat;
import jakarta.validation.constraints.*;
import java.util.List;
public final class ChatDtos {
 private ChatDtos() {}
 public record Request(@NotBlank @Size(max=300) String question, Boolean saveConversation) { public Request { saveConversation=Boolean.TRUE.equals(saveConversation); } }
 public record ProjectLink(String title, String url) {}
 public record Limits(boolean limited, int retryAfterSeconds) {}
 public record Response(String answer, List<ProjectLink> projects, String code, Limits limits, boolean conversationStored) {}
}
