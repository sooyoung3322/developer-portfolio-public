package com.portfolio.backend.chat;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
@Entity @Table(name="portfolio_knowledge") @Getter
public class Knowledge {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String category, title;
 @Column(columnDefinition="text") private String content;
 private String projectSlug, sourceRef;
 private boolean published;
 private Instant createdAt, updatedAt;
 protected Knowledge() {}
}
