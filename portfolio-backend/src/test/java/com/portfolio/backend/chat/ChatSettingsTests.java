package com.portfolio.backend.chat;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ChatSettingsTests {
 GeminiFreeTierCatalog catalog(boolean confirmed){
  var catalog=new GeminiFreeTierCatalog();
  catalog.setModels(Map.of("gemini-3.5-flash-lite",new GeminiFreeTierCatalog.Quota(confirmed,15,500,250000)));
  return catalog;
 }
 ChatSettings settings(String model,String confirmedModel,int daily,int rpd,int rpm,int tpm){
  return new ChatSettings(true,true,"test",model,daily,rpd,rpm,tpm,confirmedModel,catalog(true));
 }
 @Test void confirmedNewModelAndActualQuotaAreAvailable(){
  var settings=settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",50,500,15,250000);
  assertThat(settings.available()).isTrue();assertThat(settings.dailyLimit()).isEqualTo(50);
  assertThat(settings.spacingMs()).isEqualTo(4800);
 }
 @Test void dailyLimitIsConfigurableAndCannotExceedProjectRpd(){
  assertThat(settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",200,500,15,250000).dailyLimit()).isEqualTo(200);
  assertThat(settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",1000,500,15,250000).dailyLimit()).isEqualTo(500);
 }
 @Test void environmentLimitsCannotExceedReviewedModelLimits(){
  var settings=settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",1000,5000,150,2500000);
  assertThat(settings.rpd).isEqualTo(500);assertThat(settings.rpm).isEqualTo(15);assertThat(settings.tpm).isEqualTo(250000);
  var lower=settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",50,20,2,20000);
  assertThat(lower.dailyLimit()).isEqualTo(20);assertThat(lower.spacingMs()).isEqualTo(60000);
 }
 @Test void wrongConfirmationAndUnlistedModelsFailClosed(){
  assertThat(settings("gemini-3.5-flash-lite","gemini-2.5-flash-lite",50,500,15,250000).unavailableReason()).isEqualTo("FREE_TIER_MODEL_MISMATCH");
  for(String model:new String[]{"gemini-2.5-flash-lite","paid-model","gemini-flash-lite-latest"})
   assertThat(settings(model,model,50,500,15,250000).unavailableReason()).isEqualTo("MODEL_NOT_ALLOWED");
 }
 @Test void unconfirmedEntryAndGlobalOptOutDisableChat(){
  assertThat(new ChatSettings(true,true,"test","gemini-3.5-flash-lite",50,500,15,250000,"gemini-3.5-flash-lite",catalog(false)).unavailableReason()).isEqualTo("MODEL_FREE_TIER_NOT_CONFIRMED");
  assertThat(new ChatSettings(true,false,"test","gemini-3.5-flash-lite",50,500,15,250000,"gemini-3.5-flash-lite",catalog(true)).available()).isFalse();
 }
 @Test void invalidQuotaNeverEnablesChat(){
  for(int daily:new int[]{0,-1})assertThat(settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",daily,500,15,250000).unavailableReason()).isEqualTo("DAILY_LIMIT_INVALID");
  assertThat(settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",50,0,15,250000).available()).isFalse();
  assertThat(settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",50,500,0,250000).available()).isFalse();
  assertThat(settings("gemini-3.5-flash-lite","gemini-3.5-flash-lite",50,500,15,19999).available()).isFalse();
 }
}
