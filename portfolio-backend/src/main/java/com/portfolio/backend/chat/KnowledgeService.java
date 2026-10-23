package com.portfolio.backend.chat;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Profile({"local","production","prod"})
public class KnowledgeService implements KnowledgeContextSource {
 private final KnowledgeRepository repository;
 private final PrivacyGuard privacy;
 public KnowledgeService(KnowledgeRepository repository,PrivacyGuard privacy) { this.repository=repository;this.privacy=privacy; }
 @Override @Transactional(readOnly=true)
 public List<Knowledge> search(String question) {
  // Do not silently omit long records or require exact question keywords.
  return repository.findByPublishedTrueOrderById().stream()
   .filter(k->!privacy.sensitive(k.getContent()) && !privacy.injection(k.getContent()))
   .filter(k->!privacy.sensitive(k.getTitle()) && !privacy.injection(k.getTitle())).toList();
 }
}
