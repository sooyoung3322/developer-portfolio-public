package com.portfolio.backend.chat;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChatTraceFilter extends OncePerRequestFilter {
 @Override protected boolean shouldNotFilter(HttpServletRequest request){
  return !"POST".equals(request.getMethod()) || !"/api/portfolio-chat".equals(request.getServletPath());
 }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  String previous=MDC.get("chatTraceId");long start=System.nanoTime();boolean completed=false;
  MDC.put("chatTraceId",UUID.randomUUID().toString());
  response.setHeader("X-Chat-Trace-Id",MDC.get("chatTraceId"));
  try {
   ChatLog.info("chat_received", "method=POST");
   chain.doFilter(request,response);completed=true;
  } finally {
   int status=completed?response.getStatus():500;
   String details="status="+status+" outcome="+(status<400?"success":"failure")+" durationMs="+ChatLog.elapsed(start);
   if(status>=400)ChatLog.failure("chat_completed",details,null,status>=500 && status!=503);
   else ChatLog.info("chat_completed",details);
   if(previous==null)MDC.remove("chatTraceId");else MDC.put("chatTraceId",previous);
  }
 }
}
