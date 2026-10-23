package com.portfolio.backend.chat;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import java.util.*;
import static com.portfolio.backend.chat.ChatDtos.*;
@Service @Profile({"local","production","prod"})
public class ChatService {
 public static final String BUSY="현재 AI 질문 이용량이 많아 잠시 답변을 제공하기 어렵습니다. 잠시 후 다시 이용해 주시거나 프로젝트 소개를 확인해 주세요.";
 private final KnowledgeContextSource knowledge;private final UsageService usage;private final ChatProvider provider;private final PrivacyGuard privacy;
 public ChatService(KnowledgeContextSource knowledge,UsageService usage,ChatProvider provider,PrivacyGuard privacy){this.knowledge=knowledge;this.usage=usage;this.provider=provider;this.privacy=privacy;}
 public Response ask(Request request,boolean sessionAllowed) {return ask(request,sessionAllowed?0:600);}
 public Response ask(Request request,int sessionRetry) {
  boolean sessionAllowed=sessionRetry==0;
  long start=System.nanoTime();var admission=usage.admit(sessionAllowed);var day=admission.date();
  String code="SUCCESS",answer="확인 가능한 포트폴리오 정보가 없습니다.",category="general";Integer input=null,output=null;boolean quota=false;int retry=0;boolean apiReserved=false;
  var links=new ArrayList<ProjectLink>();
  if(!admission.allowed()){code=admission.code();retry=sessionAllowed?admission.retryAfterSeconds():sessionRetry;answer=BUSY;}
  else if(privacy.sensitive(request.question())){code="PRIVACY_BLOCKED";answer="개인정보나 비밀 키가 포함될 수 있어 처리하지 않았습니다. 해당 내용을 제외하고 질문해 주세요.";}
  else if(privacy.injection(request.question())){code="PRIVACY_BLOCKED";answer="내부 지시나 비공개 정보는 제공할 수 없습니다. 공개된 경력과 프로젝트에 대해 질문해 주세요.";}
  else {
   List<Knowledge> context;
   try {context=knowledge.search(request.question());ChatLog.info("knowledge_search_completed","count="+(context==null?0:context.size()));}
   catch(org.springframework.dao.DataAccessException failure) {ChatLog.failure("knowledge_search_failed","KNOWLEDGE_ERROR",failure,true);context=null;}
   catch(RuntimeException failure) {ChatLog.failure("knowledge_search_failed","KNOWLEDGE_INTERNAL_ERROR",failure,true);throw failure;}
   if(context==null){code="KNOWLEDGE_ERROR";answer="공개 포트폴리오 자료를 불러오지 못했습니다. 잠시 후 다시 질문해 주세요.";}
   else
   if(context.isEmpty())code="NO_CONTEXT";
   else {
   var reservation=usage.reserveApiDetailed(day);
   if(!reservation.allowed()){code=reservation.code();retry=reservation.retryAfterSeconds();answer=BUSY;}
   else try {
    apiReserved=true;
    var generated=provider.generate(request.question(),context);input=generated.promptTokens();output=generated.completionTokens();
    var allowed=new HashMap<Long,Knowledge>();context.forEach(k->allowed.put(k.getId(),k));
    if(generated.evidenceIds()==null || generated.answer()==null || generated.answer().isBlank() || generated.answer().length()>6000 || generated.evidenceIds().size()>context.size() || generated.evidenceIds().stream().anyMatch(id->!allowed.containsKey(id)))throw new ChatProvider.Failure("RESPONSE_PARSE_ERROR",input,output);
    var selected=generated.evidenceIds().stream().distinct().map(allowed::get).toList();
    if(privacy.sensitive(generated.answer()) || privacy.injection(generated.answer()) || generated.answer().matches("(?s).*(?:https?://|www\\.).*"))throw new ChatProvider.Failure("RESPONSE_REJECTED",input,output);
    ChatLog.info("chat_response_validated","outcome=success");
    answer=generated.answer();
    if(selected.isEmpty())code="NO_CONTEXT";
    else {
     category=selected.getFirst().getCategory();
     for(var k:selected)if(k.getProjectSlug()!=null && k.getProjectSlug().matches("project-one|project-two|project-three|project-four|project-five|project-six")) {
      var link=new ProjectLink(k.getTitle(),"/projects/"+k.getProjectSlug());if(links.stream().noneMatch(l->l.url().equals(link.url())))links.add(link);
     }
    }
   } catch(ChatProvider.Failure failure) {
    ChatLog.failure("chat_generation_failed",failure.code,failure,false);
    quota=failure.quota;if(input==null)input=failure.promptTokens;if(output==null)output=failure.completionTokens;code=failure.code;
    answer="AI 서비스 연결에 문제가 발생했습니다.\n잠시 후 다시 시도해 주세요.";
   }
  }
  }
  boolean store=(code.equals("SUCCESS") || code.equals("NO_CONTEXT")) && request.saveConversation() && !privacy.sensitive(request.question()) && !privacy.sensitive(answer) && !privacy.injection(request.question());
  String status=switch(code){case "SUCCESS"->"SUCCESS";case "NO_CONTEXT"->"NO_CONTEXT";case "SESSION_RATE_LIMITED","APP_DAILY_LIMITED","GEMINI_RPM_LIMITED","GEMINI_RPD_LIMITED","GEMINI_TPM_LIMITED","GEMINI_RATE_LIMITED"->"RATE_LIMITED";default->"ERROR";};
  if(status.equals("RATE_LIMITED")) {
   if(retry==0)retry=(int)Math.max(60,usage.cooldownSeconds());
   answer=switch(code){
    case "APP_DAILY_LIMITED","GEMINI_RPD_LIMITED"->"오늘의 AI 질문 한도가 소진되었습니다.\n내일 다시 이용해 주세요.";
    case "SESSION_RATE_LIMITED","GEMINI_RPM_LIMITED","GEMINI_TPM_LIMITED"->"잠시 질문이 많아졌어요.\n잠시 후 다시 시도해 주세요.";
    default->"현재 AI 서비스 이용량이 많아 답변하기 어렵습니다. 잠시 후 다시 이용해 주세요.";
   };
   ChatLog.info("chat_rate_limited","code="+code+" retryAfterSeconds="+retry+" apiReserved="+apiReserved+" source="+(quota?"provider":"application")+" timezone="+UsageService.ZONE);
  }
  usage.finish(day,status,category,store?request.question():null,store?answer:null,input,output,(System.nanoTime()-start)/1000000,quota,Set.of("API_ERROR","RESPONSE_PARSE_ERROR","RESPONSE_REJECTED").contains(code));
  ChatLog.info("chat_processing_completed","code="+code+" durationMs="+ChatLog.elapsed(start));
  return new Response(answer,links,code,new Limits(status.equals("RATE_LIMITED"),status.equals("RATE_LIMITED")?retry:0),store);
 }
}
