package com.portfolio.backend.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Only server-controlled metadata belongs here. Never log throwable messages or bodies. */
final class ChatLog {
 private static final Logger LOG=LoggerFactory.getLogger(ChatLog.class);
 static long elapsed(long start){return (System.nanoTime()-start)/1_000_000;}
 static void info(String event,String details){LOG.info("event={} traceId={} {}",event,MDC.get("chatTraceId"),details);}
 static void failure(String event,String reason,Throwable error,boolean unexpected){
  String type=error==null?"none":error.getClass().getSimpleName();
  if(!type.matches("[A-Za-z0-9_$]{1,120}"))type="Exception";
  // Do not attach Throwable: URLs, credentials and user content can occur in its message/causes.
  if(unexpected)LOG.error("event={} traceId={} reason={} exceptionType={}",event,MDC.get("chatTraceId"),reason,type);
  else LOG.warn("event={} traceId={} reason={} exceptionType={}",event,MDC.get("chatTraceId"),reason,type);
 }
 static String httpReason(int status){return switch(status){
  case 401,403 -> "API_AUTHENTICATION";case 400,404 -> "MODEL_OR_ENDPOINT";
  case 429 -> "API_QUOTA_EXCEEDED";default -> status>=500?"UPSTREAM_UNAVAILABLE":"UPSTREAM_HTTP_ERROR";};}
 private ChatLog(){}
}
