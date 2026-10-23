package com.portfolio.backend.analytics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@ConditionalOnProperty(name = "analytics.enabled", havingValue = "true")
public class AnalyticsController {
    private final AnalyticsService service;
    public AnalyticsController(AnalyticsService service) { this.service = service; }
    public record EventRequest(@jakarta.validation.constraints.NotNull @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}") String browserId,
        @jakarta.validation.constraints.NotNull @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}") String eventId) {}
    @PostMapping("/portfolio-markdown-download")
    public AnalyticsService.Recorded markdownDownload(@Valid @RequestBody EventRequest request) { return service.recordMarkdownDownload(request.browserId(), request.eventId()); }
    @PostMapping("/visits")
    public AnalyticsService.Recorded visit(@Valid @RequestBody EventRequest request) { return service.record(request.browserId(), null, request.eventId(), false); }
    @PostMapping("/projects/{slug}/views")
    public AnalyticsService.Recorded view(@PathVariable String slug, @Valid @RequestBody EventRequest request) { return service.record(request.browserId(), slug, request.eventId(), false); }
    @PostMapping("/projects/{slug}/clicks")
    public AnalyticsService.Recorded click(@PathVariable String slug, @Valid @RequestBody EventRequest request) { return service.record(request.browserId(), slug, request.eventId(), true); }
}
