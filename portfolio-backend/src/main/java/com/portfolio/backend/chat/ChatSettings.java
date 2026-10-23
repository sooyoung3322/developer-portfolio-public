package com.portfolio.backend.chat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
@Component
public class ChatSettings {
 public final boolean enabled, freeConfirmed;
 public final String key, model, confirmedModel;
 private final GeminiFreeTierCatalog.Quota quota;
 public final int daily, rpd, rpm, tpm;
 @Autowired
 public ChatSettings(@Value("${ai.enabled:false}") String enabled,
  @Value("${ai.free-tier-confirmed:false}") String freeConfirmed,
  @Value("${ai.api-key:}") String key, @Value("${ai.model:gemini-3.5-flash-lite}") String model,
  @Value("${ai.daily-limit:50}") int daily, @Value("${ai.free-rpd:0}") int rpd,
  @Value("${ai.free-rpm:0}") int rpm, @Value("${ai.free-tpm:0}") int tpm,
  @Value("${ai.free-tier.confirmed-model:gemini-3.5-flash-lite}") String confirmedModel,
  GeminiFreeTierCatalog catalog) {
  // Spring's boolean converter also accepts yes/on/1. Require explicit true here.
  this.enabled="true".equals(enabled); this.freeConfirmed="true".equals(freeConfirmed); this.key=key; this.model=model;
  this.confirmedModel=confirmedModel;this.quota=catalog.getModels().get(model);
  this.daily=daily;
  this.rpd=quota==null?0:Math.min(rpd,quota.rpd());
  this.rpm=quota==null?0:Math.min(rpm,quota.rpm());
  this.tpm=quota==null?0:Math.min(tpm,quota.tpm());
 }
 // Synthetic settings used by isolated tests; application injection uses the strict constructor above.
 public ChatSettings(boolean enabled,boolean freeConfirmed,String key,String model,int daily,int rpd,int rpm,int tpm,String confirmedModel,GeminiFreeTierCatalog catalog) {
  this(Boolean.toString(enabled),Boolean.toString(freeConfirmed),key,model,daily,rpd,rpm,tpm,confirmedModel,catalog);
 }
 @jakarta.annotation.PostConstruct
 void logConfiguration() {
  // Only validated identifiers, booleans and numbers; never log keys or raw environment values.
  String safeModel=quota!=null && model.matches("gemini-[a-z0-9.-]{1,80}")?model:"unlisted";
  ChatLog.info("chat_configuration","enabled="+enabled+" freeTierConfirmed="+freeConfirmed+
   " keyPresent="+!key.isBlank()+" available="+available()+" reason="+unavailableReason()+
   " model="+safeModel+" dailyLimit="+dailyLimit()+" rpd="+rpd+" rpm="+rpm+" tpm="+tpm);
 }
 public boolean available() { return unavailableReason().equals("AVAILABLE"); }
 public String unavailableReason() {
  if(!enabled)return "AI_DISABLED";
  if(!freeConfirmed)return "FREE_TIER_NOT_CONFIRMED";
  if(key.isBlank())return "API_KEY_MISSING";
  if(quota==null || !model.matches("gemini-[a-z0-9.-]+"))return "MODEL_NOT_ALLOWED";
  if(!model.equals(confirmedModel))return "FREE_TIER_MODEL_MISMATCH";
  if(!quota.confirmed())return "MODEL_FREE_TIER_NOT_CONFIRMED";
  if(daily<=0)return "DAILY_LIMIT_INVALID";
  if(rpd<=0 || rpm<=0 || tpm<20000)return "FREE_TIER_LIMITS_INVALID";
  return "AVAILABLE";
 }
 public int dailyLimit() { return Math.min(daily,rpd); }
 // Conservative upper bound on the bounded UTF-8 prompt, not a reported usage estimate.
 public long spacingMs() { return Math.max((long)Math.ceil(60000.0/rpm),(long)Math.ceil(60000.0*20000/tpm)); }
}
