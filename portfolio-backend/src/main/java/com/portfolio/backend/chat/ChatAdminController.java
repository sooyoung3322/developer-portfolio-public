package com.portfolio.backend.chat;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController @Profile("local") @RequestMapping("/api/ai-admin")
public class ChatAdminController {
 private final UsageService service;
 public ChatAdminController(UsageService service){this.service=service;}
 @GetMapping("/summary") public Object summary(){return service.summary();}
 @GetMapping("/daily") public Object daily(@RequestParam(defaultValue="7") int days){if(days<1 || days>90)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return service.daily(days);}
 @GetMapping("/usage") public Object usage(){return service.usage();}
}
