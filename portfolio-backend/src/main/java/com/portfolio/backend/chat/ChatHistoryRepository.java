package com.portfolio.backend.chat;
import java.time.Instant;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface ChatHistoryRepository extends JpaRepository<ChatHistory,Long> {
 long deleteByExpiresAtLessThanEqual(Instant now);
 @Query("select h.category as category, count(h) as count from ChatHistory h where h.question is not null group by h.category order by count(h) desc")
 List<Faq> faq();
 interface Faq { String getCategory(); long getCount(); }
}
