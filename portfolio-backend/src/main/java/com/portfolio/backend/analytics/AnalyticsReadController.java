package com.portfolio.backend.analytics;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController
@Profile("local")
@ConditionalOnProperty(name = "analytics.enabled", havingValue = "true")
@RequestMapping("/api/analytics")
public class AnalyticsReadController {
    private final AnalyticsService service;
    public AnalyticsReadController(AnalyticsService service) { this.service = service; }
    @GetMapping("/summary") public AnalyticsService.Summary summary() { return service.summary(); }
    @GetMapping("/daily") public List<AnalyticsService.Daily> daily(@RequestParam(defaultValue="7") int days) {
        if (days < 1 || days > 90) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "days must be 1..90");
        return service.daily(days);
    }
    @GetMapping("/projects") public List<AnalyticsService.Project> projects() { return service.projects(); }
}
