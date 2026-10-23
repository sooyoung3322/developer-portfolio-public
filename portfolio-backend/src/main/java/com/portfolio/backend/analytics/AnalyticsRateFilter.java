package com.portfolio.backend.analytics;

import com.portfolio.backend.config.SecurityConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;

/** Per-process, fixed-minute limits. No raw network identifier is retained or logged. */
public class AnalyticsRateFilter extends OncePerRequestFilter {
    private final String origin;
    private final Map<String,Integer> clients = new HashMap<>();
    private long minute = -1;
    private int total;
    public AnalyticsRateFilter(String origin) { this.origin = origin; }
    private synchronized boolean allow(String key) {
        long current = System.currentTimeMillis() / 60000;
        if (current != minute) { minute = current; total = 0; clients.clear(); }
        if (++total > 600 || clients.size() >= 600 && !clients.containsKey(key)) return false;
        int count = clients.merge(key, 1, Integer::sum);
        return count <= 60;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (SecurityConfig.event(request)) {
            String suppliedOrigin = request.getHeader("Origin");
            if (suppliedOrigin != null && !origin.equals(suppliedOrigin)) { response.sendError(403); return; }
            if (!allow(AnalyticsService.hash(request.getRemoteAddr()))) {
                response.setHeader("Retry-After", "60"); response.sendError(429); return;
            }
            // Browser fetch JSON supplies Content-Length. Reject unbounded/chunked payloads.
            if (request.getContentLengthLong() < 0 || request.getContentLengthLong() > 1024) { response.sendError(413); return; }
        }
        chain.doFilter(request, response);
    }
}
