package com.portfolio.backend.chat;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ChatLoggingTests {
 static final String SECRET="sensitive-marker-key-question-answer?key=secret";
 final Logger logger=(Logger)LoggerFactory.getLogger(ChatLog.class);
 final ListAppender<ILoggingEvent> logs=new ListAppender<>();
 Level previous;
 final ChatSettings settings=ChatTestSettings.create(true,true,SECRET,"gemini-3.5-flash-lite",50,50,5,250000);
 @BeforeEach void capture(){previous=logger.getLevel();logger.setLevel(Level.INFO);logs.start();logger.addAppender(logs);MDC.put("chatTraceId","test-trace");}
 @AfterEach void cleanup(){logger.detachAppender(logs);logger.setLevel(previous);logs.stop();MDC.clear();}
 String output(){return logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("",(a,b)->a+"\n"+b);}
 void safe(){assertThat(output()).doesNotContain(SECRET);assertThat(logs.list).allSatisfy(e->assertThat(e.getThrowableProxy()).isNull());}
 @Test void startupSettingsLogOnlySafeEffectiveValues(){
  settings.logConfiguration();
  assertThat(output()).contains("event=chat_configuration","enabled=true","keyPresent=true","dailyLimit=50");safe();
 }
 Knowledge fact(){var k=mock(Knowledge.class);when(k.getId()).thenReturn(1L);when(k.getTitle()).thenReturn("Public");when(k.getContent()).thenReturn("Public fact");when(k.getCategory()).thenReturn("career");return k;}
 @SuppressWarnings("unchecked") HttpClient transport(int status,String body) throws Exception {
  var http=mock(HttpClient.class);var response=(HttpResponse<InputStream>)mock(HttpResponse.class);
  when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
  when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);return http;
 }
 @ParameterizedTest @CsvSource({"401,API_AUTHENTICATION","403,API_AUTHENTICATION","400,MODEL_OR_ENDPOINT","404,MODEL_OR_ENDPOINT","429,API_QUOTA_EXCEEDED","503,UPSTREAM_UNAVAILABLE"})
 void httpFailuresHaveSafeClassification(int status,String reason) throws Exception {
  var http=transport(status,SECRET);
  assertThatThrownBy(()->new GeminiProvider(settings,http).generate(SECRET,List.of(fact()))).isInstanceOf(ChatProvider.Failure.class);
  assertThat(output()).contains("gemini_call_started","model=gemini-3.5-flash-lite","status="+status,"reason="+reason,"durationMs=");
  assertThat(logs.list).anySatisfy(e->{assertThat(e.getLevel()).isEqualTo(Level.WARN);assertThat(e.getFormattedMessage()).contains(reason);});
  verify(http,times(1)).send(any(),any());safe();
 }
 @Test void timeoutAndUnexpectedTransportErrorsAreDistinguished() throws Exception {
  for(var failure:List.of(new HttpTimeoutException(SECRET),new IOException(SECRET),new IllegalStateException(SECRET))){
   logs.list.clear();var http=mock(HttpClient.class);when(http.send(any(),any())).thenThrow(failure);
   assertThatThrownBy(()->new GeminiProvider(settings,http).generate(SECRET,List.of(fact()))).isInstanceOf(ChatProvider.Failure.class);
   String reason=failure instanceof HttpTimeoutException?"TIMEOUT":failure instanceof IOException?"NETWORK_ERROR":"INTERNAL_ERROR";
   assertThat(output()).contains("reason="+reason,"exceptionType="+failure.getClass().getSimpleName());
   assertThat(logs.list).anySatisfy(e->{assertThat(e.getLevel()).isEqualTo(failure instanceof IllegalStateException?Level.ERROR:Level.WARN);assertThat(e.getFormattedMessage()).contains(reason);});safe();
  }
 }
 @Test void jsonFailureIsSafe() throws Exception {
  assertThatThrownBy(()->new GeminiProvider(settings,transport(200,SECRET)).generate(SECRET,List.of(fact()))).isInstanceOf(ChatProvider.Failure.class);
  assertThat(output()).contains("RESPONSE_PARSE_ERROR");safe();
 }
 @Test void successfulHttpParsingIsLogged() throws Exception {
  String body="{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{\\\"answer\\\":\\\"safe answer\\\",\\\"evidenceIds\\\":[1]}\"}]}}]}";
  new GeminiProvider(settings,transport(200,body)).generate(SECRET,List.of(fact()));
  assertThat(output()).contains("gemini_parse_completed","status=200","durationMs=").doesNotContain("safe answer");safe();
 }
 @ParameterizedTest @CsvSource({"SUCCESS,200","API_ERROR,503","GEMINI_RATE_LIMITED,429","RESPONSE_PARSE_ERROR,503","INTERNAL,503","KNOWLEDGE,503","AUTH,503","MODEL,503","UPSTREAM,503","TIMEOUT,503"})
 @SuppressWarnings("unchecked") void mockProviderRequestFlow(String code,int status) throws Exception {
  var source=mock(KnowledgeContextSource.class);var usage=mock(UsageService.class);var provider=mock(ChatProvider.class);
  var day=LocalDate.of(2026,10,18);when(usage.admit(anyBoolean())).thenReturn(new UsageService.Admission(day,true));when(usage.reserveApiDetailed(day)).thenReturn(new UsageService.Reservation(true,"SUCCESS",0));
  var evidence=fact();when(source.search(any())).thenReturn(List.of(evidence));
  if(code.equals("KNOWLEDGE"))when(source.search(any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException(SECRET));
  else if(code.equals("SUCCESS"))when(provider.generate(any(),any())).thenReturn(new ChatProvider.Result("safe answer",List.of(1L),1,1));
  else if(List.of("AUTH","MODEL","UPSTREAM","TIMEOUT").contains(code)) {
   var http=code.equals("TIMEOUT")?mock(HttpClient.class):transport(switch(code){case "AUTH"->401;case "MODEL"->404;default->503;},SECRET);
   if(code.equals("TIMEOUT"))when(http.send(any(),any())).thenThrow(new HttpTimeoutException(SECRET));
   var delegate=new GeminiProvider(settings,http);
   when(provider.generate(any(),any())).thenAnswer(call->delegate.generate(call.getArgument(0),call.getArgument(1)));
  }
  else when(provider.generate(any(),any())).thenThrow(code.equals("INTERNAL")?new IllegalStateException(SECRET):new ChatProvider.Failure(code,null,null));
  var service=mock(ObjectProvider.class);when(service.getIfAvailable()).thenReturn(new ChatService(source,usage,provider,new PrivacyGuard()));
  var mvc=MockMvcBuilders.standaloneSetup(new ChatController(settings,service,mock(ChatSessionLimiter.class)))
   .setControllerAdvice(new ChatErrorHandler()).addFilters(new ChatTraceFilter()).build();
  logs.list.clear();MDC.remove("chatTraceId");
  var response=mvc.perform(post("/api/portfolio-chat").servletPath("/api/portfolio-chat").contentType("application/json")
   .content("{\"question\":\"Describe experience\",\"saveConversation\":false}")).andReturn().getResponse();
  assertThat(response.getStatus()).isEqualTo(status);
  String trace=response.getHeader("X-Chat-Trace-Id");assertThat(trace).matches("[0-9a-f-]{36}");
  assertThat(logs.list).allSatisfy(e->assertThat(e.getFormattedMessage()).contains("traceId="+trace));
  assertThat(output()).contains("chat_received","chat_completed","durationMs=","status="+status).doesNotContain("Describe experience","safe answer");
  if(code.equals("INTERNAL"))assertThat(output()).contains("chat_internal_failed","IllegalStateException");
  if(code.equals("KNOWLEDGE"))assertThat(output()).contains("knowledge_search_failed");
  switch(code){
   case "AUTH"->assertThat(output()).contains("API_AUTHENTICATION");
   case "MODEL"->assertThat(output()).contains("MODEL_OR_ENDPOINT");
   case "UPSTREAM"->assertThat(output()).contains("UPSTREAM_UNAVAILABLE");
   case "TIMEOUT"->assertThat(output()).contains("TIMEOUT");
  }
  assertThat(MDC.get("chatTraceId")).isNull();safe();
 }
 @Test void traceContextRestoredWhenFilterChainThrows() {
  var request=new MockHttpServletRequest("POST","/api/portfolio-chat");request.setServletPath("/api/portfolio-chat");
  assertThatThrownBy(()->new ChatTraceFilter().doFilter(request,new MockHttpServletResponse(),(a,b)->{throw new IllegalStateException(SECRET);})).isInstanceOf(IllegalStateException.class);
  assertThat(MDC.get("chatTraceId")).isEqualTo("test-trace");assertThat(output()).contains("outcome=failure");safe();
 }
}
