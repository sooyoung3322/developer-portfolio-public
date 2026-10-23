package com.portfolio.backend.chat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface ApiUsageRepository extends JpaRepository<ApiUsage,Long> {
 Optional<ApiUsage> findByUsageDateAndModelName(LocalDate date,String model);
 List<ApiUsage> findByUsageDateBetweenOrderByUsageDate(LocalDate start,LocalDate end);
 List<ApiUsage> findByUsageDate(LocalDate date);
}
