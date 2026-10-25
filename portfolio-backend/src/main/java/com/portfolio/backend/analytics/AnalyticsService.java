package com.portfolio.backend.analytics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@ConditionalOnProperty(name = "analytics.enabled", havingValue = "true")
public class AnalyticsService {
    public static final String MARKDOWN_DOWNLOAD = "portfolio-markdown-download";
    public static final List<String> SLUGS = List.of("project-one", "project-two", "project-three", "project-four", "project-five", "project-six", "developer-portfolio");
    private final AnalyticsGuardRepository guards;
    private final AnalyticsRepository events;
    private final Clock clock;
    public AnalyticsService(AnalyticsGuardRepository guards, AnalyticsRepository events, Clock clock) {
        this.guards = guards;
        this.events = events;
        this.clock = clock;
    }
    public record Recorded(boolean counted) {}
    public record Summary(long totalVisits, long todayVisits, String timezone, String metric, long uniqueVisitors, long estimatedVisitSessions) {}
    public record Daily(LocalDate date, long visits) {}
    public record Project(String projectSlug, long views, int rank, long clicks) {}
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    @Transactional
    public Recorded record(String browserId, String slug, String eventId, boolean click) {
        return record(browserId, slug, eventId, click, AnalyticsMetadata.Context.UNKNOWN);
    }
    @Transactional
    public Recorded record(String browserId, String slug, String eventId, boolean click, AnalyticsMetadata.Context context) {
        if (slug != null && !SLUGS.contains(slug)) throw new ResponseStatusException(NOT_FOUND, "Unknown project");
        String scope = slug == null ? "visit" : click ? "click:" + slug : slug;
        return recordScope(browserId, scope, eventId, slug == null, context);
    }
    @Transactional
    public Recorded recordMarkdownDownload(String browserId, String eventId) {
        return recordMarkdownDownload(browserId, eventId, AnalyticsMetadata.Context.UNKNOWN);
    }
    @Transactional
    public Recorded recordMarkdownDownload(String browserId, String eventId, AnalyticsMetadata.Context context) {
        return recordScope(browserId, MARKDOWN_DOWNLOAD, eventId, false, context);
    }
    private Recorded recordScope(String browserId, String scope, String eventId, boolean visit, AnalyticsMetadata.Context context) {
        // A receipt identifies an interaction, never a user. Permanent receipts make retries
        // idempotent across instances; the existing atomic upsert serializes concurrent requests.
        String receiptScope = "event:" + scope;
        String receiptHash = hash(browserId + ":" + eventId);
        guards.ensureExists(receiptHash, receiptScope);
        AnalyticsGuard receipt = guards.findLockedById(new AnalyticsGuardId(receiptHash, receiptScope)).orElseThrow();
        if (receipt.getLastRecordedAt() != null) return new Recorded(false);
        Instant now = clock.instant();
        if (visit) {
            String browserHash = hash(browserId);
            guards.ensureExists(browserHash, "visit");
            AnalyticsGuard visitor = guards.findLockedById(new AnalyticsGuardId(browserHash, "visit")).orElseThrow();
            now = clock.instant();
            if (visitor.canRecordAt(now)) {
                events.save(new AnalyticsEvent("visit-session", now));
                visitor.markRecordedAt(now);
            }
        }
        events.save(new AnalyticsEvent(scope, now, context));
        receipt.markRecordedAt(now);
        return new Recorded(true);
    }
    private LocalDate today() { return clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate(); }
    @Transactional(readOnly = true)
    public Summary summary() {
        return new Summary(events.countByScope("visit"),
            events.countByScopeAndVisitDate("visit", today()), "Asia/Seoul", "total_document_visits", guards.countByIdScope("visit"),
            events.countByScope("visit-session"));
    }
    @Transactional(readOnly = true)
    public List<Daily> daily(int days) {
        LocalDate end = today(), start = end.minusDays(days - 1);
        Map<LocalDate,Long> counts = new HashMap<>();
        events.countDailyVisits(start, end).forEach(row -> counts.put(row.getDate(), row.getVisits()));
        return start.datesUntil(end.plusDays(1)).map(d -> new Daily(d, counts.getOrDefault(d, 0L))).toList();
    }

    @Transactional(readOnly = true)
    public List<Project> projects() {
        Map<String, Long> viewsBySlug = new HashMap<>();
        events.countProjectViews().forEach(row -> viewsBySlug.put(row.getScope(), row.getViews()));
        var rankedSlugs = SLUGS.stream()
            .sorted(Comparator.<String>comparingLong(slug -> viewsBySlug.getOrDefault(slug, 0L))
                .reversed()
                .thenComparing(slug -> slug))
            .toList();
        List<Project> result = new ArrayList<>();
        for (int index = 0; index < rankedSlugs.size(); index++) {
            String slug = rankedSlugs.get(index);
            result.add(new Project(slug, viewsBySlug.getOrDefault(slug, 0L), index + 1,
                events.countByScope("click:" + slug)));
        }
        return result;
    }
}

