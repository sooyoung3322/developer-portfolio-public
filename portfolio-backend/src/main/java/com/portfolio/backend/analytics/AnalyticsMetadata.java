package com.portfolio.backend.analytics;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMetadata {
    public record Context(DeviceType device, String ipToken) {
        public static final Context UNKNOWN = new Context(DeviceType.UNKNOWN, null);
    }
    private final boolean enabled;
    private final byte[] secret;
    private final Clock clock;

    public AnalyticsMetadata(@Value("${analytics.ip.enabled:false}") boolean enabled,
            @Value("${analytics.ip.hmac-key:}") String key, Clock clock) {
        if (enabled && key.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalArgumentException("Analytics IP HMAC key must contain at least 32 bytes");
        this.enabled = enabled;
        this.secret = key.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public Context from(HttpServletRequest request) {
        // Ignore client-supplied forwarding headers; use the servlet peer address.
        return new Context(DeviceType.classify(request.getHeader("User-Agent")), token(request.getRemoteAddr()));
    }

    String token(String address) {
        if (!enabled || address == null || address.isBlank()) return null;
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] dailyKey = mac.doFinal(("analytics-ip:" + LocalDate.now(clock.withZone(ZoneOffset.UTC))).getBytes(StandardCharsets.UTF_8));
            mac.init(new SecretKeySpec(dailyKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(address.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Analytics HMAC unavailable", e);
        }
    }
}
