package com.portfolio.backend.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicitly reviewed project quotas only; never discover or fall back to another model. */
@Component
@ConfigurationProperties(prefix="ai.free-tier")
public class GeminiFreeTierCatalog {
 public record Quota(boolean confirmed,int rpm,int rpd,int tpm) {}
 private Map<String,Quota> models=new LinkedHashMap<>();
 public Map<String,Quota> getModels(){return models;}
 public void setModels(Map<String,Quota> models){this.models=new LinkedHashMap<>(models);}
}
