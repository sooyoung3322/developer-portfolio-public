package com.portfolio.backend.chat;

import java.util.Map;

/** Synthetic project quotas for isolated unit tests, never application configuration. */
final class ChatTestSettings {
 static ChatSettings create(boolean enabled,boolean confirmed,String key,String model,int daily,int rpd,int rpm,int tpm){
  var catalog=new GeminiFreeTierCatalog();
  catalog.setModels(Map.of("gemini-3.5-flash-lite",new GeminiFreeTierCatalog.Quota(true,rpm,rpd,tpm)));
  return new ChatSettings(enabled,confirmed,key,model,daily,rpd,rpm,tpm,"gemini-3.5-flash-lite",catalog);
 }
}
