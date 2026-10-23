package com.portfolio.backend.chat;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
@Entity @Table(name="ai_chat_history") @Getter
public class ChatHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(columnDefinition="text") private String question, answer;
 private String modelName, status, category;
 private Integer promptTokens, completionTokens;
 private long responseTimeMs;
 private Instant createdAt, expiresAt;
 protected ChatHistory() {}
 public ChatHistory(String question,String answer,String model,String status,String category,Integer input,Integer output,long ms,Instant now) {
  this.question=question;this.answer=answer;modelName=model;this.status=status;this.category=category;
  promptTokens=input;completionTokens=output;responseTimeMs=ms;createdAt=now;expiresAt=now.plusSeconds(30L*86400);
 }
}
