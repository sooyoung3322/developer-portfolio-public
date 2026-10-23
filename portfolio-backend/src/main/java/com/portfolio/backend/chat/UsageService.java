package com.portfolio.backend.chat;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @Profile({"local","production","prod"})
public class UsageService {
 public static final ZoneId ZONE=ZoneId.of("America/Los_Angeles");
 private final ApiUsageRepository usage; private final ChatGuardRepository guards; private final ChatHistoryRepository history;
 private final ChatSettings settings; private final Clock clock;
 public UsageService(ApiUsageRepository usage,ChatGuardRepository guards,ChatHistoryRepository history,ChatSettings settings,Clock clock) {this.usage=usage;this.guards=guards;this.history=history;this.settings=settings;this.clock=clock;}
 public long cooldownSeconds(){return Math.max(60,settings.spacingMs()/1000);}
 public record Admission(LocalDate date,boolean allowed,String code,int retryAfterSeconds) {
  public Admission(LocalDate date,boolean allowed){this(date,allowed,allowed?"SUCCESS":"APP_DAILY_LIMITED",0);}
 }
 public record Reservation(boolean allowed,String code,int retryAfterSeconds) {}
 private int waitSeconds(Instant until) {return (int)Math.max(1,(Duration.between(clock.instant(),until).toMillis()+999)/1000);}
 private int dailyWait(LocalDate day) {return waitSeconds(day.plusDays(1).atStartOfDay(ZONE).toInstant());}
 private ApiUsage row(LocalDate day) {return usage.findByUsageDateAndModelName(day,settings.model).orElseGet(()->usage.save(new ApiUsage(day,settings.model)));}
 @Transactional public Admission admit(boolean sessionAllowed) {
  guards.findLockedById(1L).orElseThrow();
  var day=clock.instant().atZone(ZONE).toLocalDate();var row=row(day);
  long admitted=usage.findByUsageDate(day).stream().mapToLong(ApiUsage::getAdmittedCount).sum();
  row.requestCount++;row.updatedAt=clock.instant();
  boolean allowed=sessionAllowed && admitted<settings.dailyLimit();if(allowed)row.admittedCount++;
  String code=allowed?"SUCCESS":!sessionAllowed?"SESSION_RATE_LIMITED":settings.daily<=settings.rpd?"APP_DAILY_LIMITED":"GEMINI_RPD_LIMITED";
  return new Admission(day,allowed,code,allowed?0:!sessionAllowed?600:dailyWait(day));
 }
 @Transactional public boolean reserveApi(LocalDate day) {return reserveApiDetailed(day).allowed();}
 @Transactional public Reservation reserveApiDetailed(LocalDate day) {
  var guard=guards.findLockedById(1L).orElseThrow(); var now=clock.instant();
  // Crossing midnight requires a fresh admission; it does not prove daily exhaustion.
  if(!now.atZone(ZONE).toLocalDate().equals(day))return new Reservation(false,"GEMINI_RATE_LIMITED",1);
  if(guard.nextApiAt!=null && now.isBefore(guard.nextApiAt)) {
   // Legacy reservations have no reliable reason; do not guess their cause.
   String code=guard.nextApiReason==null?"GEMINI_RATE_LIMITED":guard.nextApiReason;
   return new Reservation(false,code,waitSeconds(guard.nextApiAt));
  }
  var row=row(day);
  if(usage.findByUsageDate(day).stream().mapToLong(ApiUsage::getApiRequestCount).sum()>=settings.dailyLimit())
   return new Reservation(false,settings.daily<=settings.rpd?"APP_DAILY_LIMITED":"GEMINI_RPD_LIMITED",dailyWait(day));
  guard.nextApiAt=now.plusMillis(settings.spacingMs());
  guard.nextApiReason=(long)Math.ceil(60000.0*20000/settings.tpm)>=(long)Math.ceil(60000.0/settings.rpm)?"GEMINI_TPM_LIMITED":"GEMINI_RPM_LIMITED";
  row.apiRequestCount++;row.updatedAt=now;
  return new Reservation(true,"SUCCESS",0);
 }
 @Transactional public void finish(LocalDate day,String status,String category,String question,String answer,Integer input,Integer output,long ms,boolean quota,boolean apiError) {
  var guard=guards.findLockedById(1L).orElseThrow();var row=row(day);var now=clock.instant();
  switch(status) {case "SUCCESS"->row.successCount++;case "RATE_LIMITED"->row.rateLimitedCount++;case "NO_CONTEXT"->row.noContextCount++;default->row.errorCount++;}
  if(quota)row.providerRateLimitedCount++;if(apiError)row.apiErrorCount++;
  if(input!=null)row.promptTokens=(row.promptTokens==null?0:row.promptTokens)+input;
  if(output!=null)row.completionTokens=(row.completionTokens==null?0:row.completionTokens)+output;
  if(input!=null || output!=null)row.tokenReportedCount++;
  row.responseTimeMs+=ms;row.completedCount++;row.updatedAt=now;
  if(quota){guard.nextApiAt=now.plusSeconds(cooldownSeconds());guard.nextApiReason="GEMINI_RATE_LIMITED";}
  history.save(new ChatHistory(question,answer,settings.model,status,question==null?null:category,input,output,ms,now));
 }
 @Transactional public long purge() {return history.deleteByExpiresAtLessThanEqual(clock.instant());}
 @Transactional(readOnly=true) public Map<String,Object> summary() {
  var rows=usage.findAll();var today=clock.instant().atZone(ZONE).toLocalDate();
  long completed=rows.stream().mapToLong(ApiUsage::getCompletedCount).sum();
  var result=new LinkedHashMap<String,Object>();
  result.put("totalQuestions",rows.stream().mapToLong(ApiUsage::getRequestCount).sum());
  result.put("todayQuestions",rows.stream().filter(r->r.getUsageDate().equals(today)).mapToLong(ApiUsage::getRequestCount).sum());
  result.put("last7DaysQuestions",rows.stream().filter(r->!r.getUsageDate().isBefore(today.minusDays(6)) && !r.getUsageDate().isAfter(today)).mapToLong(ApiUsage::getRequestCount).sum());
  result.put("successCount",rows.stream().mapToLong(ApiUsage::getSuccessCount).sum());result.put("errorCount",rows.stream().mapToLong(ApiUsage::getErrorCount).sum());
  result.put("apiErrorCount",rows.stream().mapToLong(ApiUsage::getApiErrorCount).sum());result.put("geminiQuotaExceededCount",rows.stream().mapToLong(ApiUsage::getProviderRateLimitedCount).sum());
  result.put("rateLimitedCount",rows.stream().mapToLong(ApiUsage::getRateLimitedCount).sum());result.put("noContextCount",rows.stream().mapToLong(ApiUsage::getNoContextCount).sum());
  result.put("averageResponseTimeMs",completed==0?0.0:rows.stream().mapToLong(ApiUsage::getResponseTimeMs).sum()/(double)completed);
  result.put("timezone",ZONE.getId());result.put("savedQuestionCategories",history.faq());return result;
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> daily(int days) {
  var end=clock.instant().atZone(ZONE).toLocalDate();var start=end.minusDays(days-1);
  var rows=usage.findByUsageDateBetweenOrderByUsageDate(start,end);
  return start.datesUntil(end.plusDays(1)).map(day->Map.<String,Object>of("date",day,"questions",rows.stream().filter(r->r.getUsageDate().equals(day)).mapToLong(ApiUsage::getRequestCount).sum())).toList();
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> usage() {
  return usage.findAll().stream().map(r->{var m=new LinkedHashMap<String,Object>();m.put("date",r.getUsageDate());m.put("model",r.getModelName());m.put("apiRequests",r.getApiRequestCount());m.put("promptTokens",r.getPromptTokens());m.put("completionTokens",r.getCompletionTokens());m.put("tokenReportedCount",r.getTokenReportedCount());return (Map<String,Object>)m;}).toList();
 }
}
