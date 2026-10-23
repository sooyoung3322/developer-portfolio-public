package com.portfolio.backend.chat;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
public class ChatRequestFilter extends OncePerRequestFilter {
 private final String origin;
 public ChatRequestFilter(String origin){this.origin=origin;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  if(request.getServletPath().equals("/api/portfolio-chat") && request.getMethod().equals("POST")) {
   if(!origin.equals(request.getHeader("Origin")) || request.getContentType()==null || !request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {response.sendError(403);return;}
   if(request.getContentLengthLong()<0 || request.getContentLengthLong()>4096) {response.sendError(413);return;}
  }
  chain.doFilter(request,response);
 }
}
