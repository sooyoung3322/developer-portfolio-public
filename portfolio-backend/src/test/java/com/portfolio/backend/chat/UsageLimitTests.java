package com.portfolio.backend.chat;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsageLimitTests {
 final Instant now=Instant.parse("2026-10-18T10:00:00Z");
 final LocalDate day=LocalDate.of(2026,10,18);
 final ApiUsageRepository rows=mock(ApiUsageRepository.class);
 final ChatGuardRepository guards=mock(ChatGuardRepository.class);
 final ChatHistoryRepository history=mock(ChatHistoryRepository.class);
 final ChatGuard guard=new ChatGuard();
 final ApiUsage row=new ApiUsage(day,"gemini-3.5-flash-lite");
 UsageLimitTests(){
  when(guards.findLockedById(1L)).thenReturn(Optional.of(guard));
  when(rows.findByUsageDateAndModelName(day,"gemini-3.5-flash-lite")).thenReturn(Optional.of(row));
  when(rows.findByUsageDate(day)).thenReturn(List.of(row));
 }
 UsageService service(int daily,int rpd,int rpm,int tpm){
  return new UsageService(rows,guards,history,ChatTestSettings.create(true,true,"test","gemini-3.5-flash-lite",daily,rpd,rpm,tpm),Clock.fixed(now,ZoneOffset.UTC));
 }
 @Test void sessionTakesPrecedenceAndDoesNotConsumeDailyAdmission(){
  var result=service(50,50,5,20000).admit(false);
  assertThat(result.code()).isEqualTo("SESSION_RATE_LIMITED");
  assertThat(row.requestCount).isEqualTo(1);assertThat(row.admittedCount).isZero();
 }
 @Test void applicationAndGeminiDailyLimitsAreDistinct(){
  row.admittedCount=10;
  var app=service(10,50,5,20000).admit(true);
  var gemini=service(50,10,5,20000).admit(true);
  assertThat(app.code()).isEqualTo("APP_DAILY_LIMITED");
  assertThat(gemini.code()).isEqualTo("GEMINI_RPD_LIMITED");
  assertThat(app.retryAfterSeconds()).isEqualTo(21*3600);
 }
 @Test void pacingReasonPersistsAndRemainingWaitIsRoundedUp(){
  var rpm=service(50,50,1,1200000);
  assertThat(rpm.reserveApiDetailed(day).allowed()).isTrue();
  assertThat(rpm.reserveApiDetailed(day).code()).isEqualTo("GEMINI_RPM_LIMITED");
  assertThat(rpm.reserveApiDetailed(day).retryAfterSeconds()).isEqualTo(60);
  guard.nextApiAt=null;
  var tpm=service(50,50,60,20000);
  assertThat(tpm.reserveApiDetailed(day).allowed()).isTrue();
  guard.nextApiAt=now.plusMillis(1001);
  assertThat(tpm.reserveApiDetailed(day).code()).isEqualTo("GEMINI_TPM_LIMITED");
  assertThat(tpm.reserveApiDetailed(day).retryAfterSeconds()).isEqualTo(2);
  assertThat(row.apiRequestCount).isEqualTo(2);
 }
 @Test void providerCooldownNeverInventsRpmOrTpmCause(){
  var usage=service(50,50,60,1200000);
  usage.finish(day,"RATE_LIMITED","general",null,null,null,null,10,true,false);
  assertThat(usage.reserveApiDetailed(day).code()).isEqualTo("GEMINI_RATE_LIMITED");
  assertThat(usage.reserveApiDetailed(day).retryAfterSeconds()).isEqualTo(60);
 }
 @Test void failedApiReservationRemainsChargedWithoutInventingTokens(){
  var usage=service(50,50,60,1200000);
  usage.admit(true);usage.reserveApiDetailed(day);
  usage.finish(day,"ERROR","general",null,null,null,null,10,false,true);
  assertThat(row.admittedCount).isEqualTo(1);assertThat(row.apiRequestCount).isEqualTo(1);
  assertThat(row.apiErrorCount).isEqualTo(1);assertThat(row.promptTokens).isNull();
 }
 @Test void pacificDayUsesDaylightSavingTime(){
  assertThat(LocalDate.of(2026,10,19).atStartOfDay(UsageService.ZONE).toInstant()).isEqualTo(Instant.parse("2026-10-19T07:00:00Z"));
  assertThat(LocalDate.of(2026,12,19).atStartOfDay(UsageService.ZONE).toInstant()).isEqualTo(Instant.parse("2026-12-19T08:00:00Z"));
 }
}
