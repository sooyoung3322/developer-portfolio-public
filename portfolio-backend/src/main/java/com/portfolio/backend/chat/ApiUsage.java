package com.portfolio.backend.chat;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.*;
@Entity @Table(name="ai_api_usage",uniqueConstraints=@UniqueConstraint(columnNames={"usageDate","modelName"})) @Getter
public class ApiUsage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private LocalDate usageDate;
 private String modelName;
 long requestCount, admittedCount, apiRequestCount, successCount, errorCount, rateLimitedCount, noContextCount, providerRateLimitedCount, apiErrorCount;
 Long promptTokens, completionTokens;
 long tokenReportedCount, responseTimeMs, completedCount;
 Instant updatedAt;
 protected ApiUsage() {}
 public ApiUsage(LocalDate date,String model) { usageDate=date;modelName=model;updatedAt=Instant.now(); }
}
