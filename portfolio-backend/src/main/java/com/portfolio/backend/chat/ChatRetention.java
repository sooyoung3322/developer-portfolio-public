package com.portfolio.backend.chat;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
@Configuration @EnableScheduling @Profile({"local","production","prod"})
public class ChatRetention {
 private final UsageService usage;
 public ChatRetention(UsageService usage){this.usage=usage;}
 @Scheduled(fixedDelayString="${ai.cleanup-ms:60000}") public void purge(){usage.purge();}
}
