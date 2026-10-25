package com.portfolio.backend.analytics;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.*;

class AnalyticsMetadataTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);
    private final String key = "local-test-only-key-at-least-32-bytes";
    @Test void deviceEstimatesAndUnknowns() {
        assertThat(DeviceType.classify("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")).isEqualTo(DeviceType.PC);
        assertThat(DeviceType.classify("Mozilla/5.0 (Macintosh; Intel Mac OS X)")).isEqualTo(DeviceType.PC);
        assertThat(DeviceType.classify("Mozilla/5.0 (iPhone) Mobile Safari")).isEqualTo(DeviceType.MOBILE);
        assertThat(DeviceType.classify("Mozilla/5.0 Android Mobile")).isEqualTo(DeviceType.MOBILE);
        assertThat(DeviceType.classify("Mozilla/5.0 iPad Mobile Safari")).isEqualTo(DeviceType.TABLET);
        assertThat(DeviceType.classify("Mozilla/5.0 Android Safari")).isEqualTo(DeviceType.TABLET);
        for (String ua : new String[]{null, "", "curl/8", "Googlebot Mobile", "x".repeat(2049)})
            assertThat(DeviceType.classify(ua)).isEqualTo(DeviceType.UNKNOWN);
    }
    @Test void tokensRotateAndNeverUseUntrustedHeadersOrBody() {
        var metadata = new AnalyticsMetadata(true, key, clock);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.2");
        request.addHeader("X-Forwarded-For", "203.0.113.99");
        request.addHeader("Forwarded", "for=203.0.113.99");
        request.addHeader("User-Agent", "iPhone Mobile");
        var result = metadata.from(request);
        assertThat(result.device()).isEqualTo(DeviceType.MOBILE);
        assertThat(result.ipToken()).matches("[0-9a-f]{64}").isEqualTo(metadata.token("198.51.100.2"))
            .isNotEqualTo(metadata.token("203.0.113.99"));
        assertThat(new AnalyticsMetadata(true, key, Clock.offset(clock, Duration.ofDays(1))).token("198.51.100.2"))
            .isNotEqualTo(result.ipToken());
        assertThat(new AnalyticsMetadata(true, key + "rotated", clock).token("198.51.100.2"))
            .isNotEqualTo(result.ipToken());
        assertThat(new AnalyticsMetadata(false, "", clock).from(request).ipToken()).isNull();
        assertThatIllegalArgumentException().isThrownBy(() -> new AnalyticsMetadata(true, "short", clock));
    }
}
