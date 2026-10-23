package com.portfolio.backend.chat;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface KnowledgeRepository extends JpaRepository<Knowledge,Long> {
 List<Knowledge> findByPublishedTrueOrderById();
}
