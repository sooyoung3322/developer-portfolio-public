package com.portfolio.backend.chat;
import java.util.List;
public interface ChatProvider {
 record Result(String answer,List<Long> evidenceIds,Integer promptTokens,Integer completionTokens) {}
 Result generate(String question,List<Knowledge> context);
 class Failure extends RuntimeException {
  public final boolean quota;
  public final String code;
  public final Integer promptTokens,completionTokens;
  public Failure(boolean quota) {this(quota,null,null);}
  public Failure(boolean quota,Integer input,Integer output) { this(quota?"GEMINI_RATE_LIMITED":"API_ERROR",input,output); }
  public Failure(String code,Integer input,Integer output) { super("AI provider unavailable",null,false,false);this.code=code;this.quota=code.equals("GEMINI_RATE_LIMITED");promptTokens=input;completionTokens=output; }
 }
}
