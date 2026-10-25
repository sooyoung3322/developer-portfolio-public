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
 static Knowledge publicContact() {
  var fact = new Knowledge();
  fact.id = -1L; fact.category = "contact"; fact.title = "공개 연락처";
  fact.content = "공개 소스 합성 이메일 (실제 연락 불가): " + PublicContact.EMAIL;
  fact.published = true; fact.sourceRef = "public-contact.json";
  return fact;
 }
}
