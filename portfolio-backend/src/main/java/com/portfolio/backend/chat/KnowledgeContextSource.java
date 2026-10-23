package com.portfolio.backend.chat;
import java.util.List;
/** Replace with semantic retrieval when the full public corpus outgrows the prompt budget. */
public interface KnowledgeContextSource {
 List<Knowledge> search(String question);
}
