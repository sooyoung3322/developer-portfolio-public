package com.portfolio.backend.chat;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ChatGuardRepository extends JpaRepository<ChatGuard,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<ChatGuard> findLockedById(Long id);
}
