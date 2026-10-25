package com.portfolio.backend.analytics;

import java.util.Locale;

/** Coarse User-Agent estimate only; the original User-Agent is never persisted. */
public enum DeviceType {
    PC, MOBILE, TABLET, UNKNOWN;

    public static DeviceType classify(String agent) {
        if (agent == null || agent.isBlank() || agent.length() > 2048) return UNKNOWN;
        String ua = agent.toLowerCase(Locale.ROOT);
        if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")) return UNKNOWN;
        if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("kindle") || ua.contains("silk/")
                || ua.contains("android") && !ua.contains("mobile")) return TABLET;
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("ipod")) return MOBILE;
        if (ua.contains("windows nt") || ua.contains("macintosh") || ua.contains("x11")) return PC;
        return UNKNOWN;
    }
}
