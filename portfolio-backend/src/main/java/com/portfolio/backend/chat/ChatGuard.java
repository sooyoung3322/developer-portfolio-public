package com.portfolio.backend.chat;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="ai_chat_guard")
public class ChatGuard {
 @Id Long id;
 Instant nextApiAt;
 String nextApiReason;
 protected ChatGuard() {}
}
