package com.portfolio.backend.analytics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class AnalyticsRateFilterTests {
    @Test void projectClicksUseExistingProtection() throws Exception {
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/api/analytics/projects/project-one/clicks");
        when(request.getHeader("Origin")).thenReturn("https://evil.example");
        new AnalyticsRateFilter("http://localhost:5173").doFilterInternal(request, response, chain);
        verify(response).sendError(403);
        verifyNoInteractions(chain);
    }
    @Test void rejectsRepeatedRequestsAndOversizedBody() throws Exception {
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn("/api/analytics/portfolio-markdown-download");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getContentLengthLong()).thenReturn(60L);
        var filter = new AnalyticsRateFilter("http://localhost:5173");
        // Exercise the filter directly to avoid a clock-dependent minute boundary.
        for (int i = 0; i < 61; i++) filter.doFilterInternal(request, response, chain);
        verify(response, atLeastOnce()).sendError(429);
        when(request.getRemoteAddr()).thenReturn("127.0.0.2");
        when(request.getContentLengthLong()).thenReturn(1025L);
        filter.doFilterInternal(request, response, chain);
        verify(response).sendError(413);
    }
}
