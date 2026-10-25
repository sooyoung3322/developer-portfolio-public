package com.portfolio.backend.chat;
import com.portfolio.backend.PortfolioBackendApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static com.portfolio.backend.chat.ChatDtos.*;
class ChatIntegrationTests {
 static ServletWebServerApplicationContext context;
 static final class TestClock extends Clock { volatile Instant now=Instant.parse("2026-10-18T10:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now;} }
 static final class MockProvider implements ChatProvider {
  int calls; boolean fail,quota,invalid; String answer="Generated synthesis across public projects."; List<Knowledge> received; String questionReceived;
  public Result generate(String question,List<Knowledge> context) {calls++;received=context;questionReceived=question;if(fail)throw new Failure(quota);return new Result(answer,invalid?List.of(99999L):context.stream().map(Knowledge::getId).toList(),12,4);}
 }
 @Configuration(proxyBeanMethods=false) static class MockConfiguration {
  @Bean @Primary TestClock testClock(){return new TestClock();}
  @Bean @Primary MockProvider mockProvider(){return new MockProvider();}
 }
 static ChatService chat;static UsageService usage;static ChatHistoryRepository history;static ApiUsageRepository rows;static MockProvider mock;static TestClock clock;static String base;
 @BeforeAll static void start(){
  context=(ServletWebServerApplicationContext)new SpringApplication(PortfolioBackendApplication.class,MockConfiguration.class).run(
   "--server.port=0","--spring.profiles.active=local","--spring.datasource.url="+System.getProperty("chat.test.url","jdbc:h2:mem:chat;MODE=MySQL;DB_CLOSE_DELAY=-1"),
   "--spring.datasource.driver-class-name="+System.getProperty("chat.test.driver","org.h2.Driver"),"--spring.datasource.username="+System.getProperty("chat.test.username","sa"),"--spring.datasource.password="+System.getProperty("chat.test.password",""),
   "--ai.enabled=true","--ai.free-tier-confirmed=true","--ai.api-key=test-only-not-a-real-key",
   "--ai.free-rpd=500","--ai.free-rpm=15","--ai.free-tpm=250000","--ai.cleanup-ms=86400000");
  chat=context.getBean(ChatService.class);usage=context.getBean(UsageService.class);history=context.getBean(ChatHistoryRepository.class);rows=context.getBean(ApiUsageRepository.class);
  mock=context.getBean(MockProvider.class);clock=context.getBean(TestClock.class);base="http://localhost:"+context.getWebServer().getPort();
 }
 @AfterAll static void stop(){if(context!=null)context.close();}
 @BeforeEach void reset(){history.deleteAllInBatch();rows.deleteAllInBatch();clock.now=Instant.parse("2026-10-18T10:00:00Z");mock.calls=0;mock.fail=false;mock.quota=false;mock.invalid=false;mock.answer="Generated synthesis across public projects.";
  var tx=new org.springframework.transaction.support.TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class));
  tx.executeWithoutResult(s->context.getBean(ChatGuardRepository.class).findLockedById(1L).orElseThrow().nextApiAt=null);
 }
 Response ask(boolean save){return chat.ask(new Request("DEMO PROJECT 기술을 알려주세요",save),true);}
 @Test void defaultModelBindsReviewedQuotaAndStatusEnablesWithoutCallingProvider() throws Exception {
  var settings=context.getBean(ChatSettings.class);
  assertThat(settings.model).isEqualTo("gemini-3.5-flash-lite");
  assertThat(settings.confirmedModel).isEqualTo(settings.model);
  assertThat(settings.rpm).isEqualTo(15);assertThat(settings.rpd).isEqualTo(500);assertThat(settings.tpm).isEqualTo(250000);
  assertThat(settings.spacingMs()).isEqualTo(4800);
  var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/api/portfolio-chat/status")).GET().build(),HttpResponse.BodyHandlers.ofString());
  assertThat(response.statusCode()).isEqualTo(200);assertThat(response.body()).contains("\"enabled\":true");assertThat(mock.calls).isZero();
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={
  "이 개발자는 어떤 사람인가요?","Java와 Spring Boot 경험이 있나요?",
  "이 사람이 주도적으로 개발한 프로젝트는 무엇인가요?","AWS 운영 경험이 있나요?",
  "실무에서 해결한 장애 사례가 있나요?","게임 서버 개발 경험을 설명해 주세요.",
  "이 개발자의 강점은 무엇인가요?","어떤 회사에서 일했나요?","가장 복잡했던 프로젝트는 무엇인가요?",
  "서비스를 처음부터 맡아 출시해 본 적도 있을까요?"})
 void arbitraryQuestionsReachGeneratorWithFullPublicFacts(String question){
  var result=chat.ask(new Request(question,false),true);
  assertThat(result.code()).isEqualTo("SUCCESS");assertThat(result.answer()).isEqualTo(mock.answer);
  assertThat(mock.questionReceived).isEqualTo(question);assertThat(mock.received).hasSize(12).allMatch(Knowledge::isPublished);
  assertThat(result.projects()).extracting(ProjectLink::url).containsExactly(
   "/projects/project-one","/projects/project-two","/projects/project-three","/projects/project-four","/projects/project-five","/projects/project-six");
 }
 @Test void fullPromptFitsExistingConservativeTokenBudget() throws Exception {
  var facts=context.getBean(KnowledgeService.class).search("소개");
  var payload=context.getBean(GeminiProvider.class).payload("가".repeat(300),facts);
  int bytes=new tools.jackson.databind.ObjectMapper().writeValueAsBytes(payload).length;
  System.out.println("Full public knowledge prompt UTF-8 bytes: "+bytes);
  assertThat(bytes).isLessThanOrEqualTo(16000);
 }
 @Test void unpublishedKnowledgeNeverReachesGenerator(){
  var jdbc=new org.springframework.jdbc.core.JdbcTemplate(context.getBean(javax.sql.DataSource.class));
  jdbc.update("insert into portfolio_knowledge(category,title,content,source_ref,published,created_at,updated_at) values (?,?,?,?,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)","private","Private","Private internal fact","private");
  try {ask(false);assertThat(mock.received).hasSize(12).allMatch(Knowledge::isPublished);}
  finally {jdbc.update("delete from portfolio_knowledge where published=false");}
 }
 @Test void syntheticContactIsAvailableAndOtherAddressesStayBlocked(){
  mock.answer="Demo contact: "+PublicContact.EMAIL;
  var result=ask(false);
  assertThat(result.code()).isEqualTo("SUCCESS");
  assertThat(result.answer()).contains("demo@example.test");
  assertThat(mock.received).anyMatch(k->k.getId()==-1L && k.getContent().contains(PublicContact.EMAIL));
  assertThat(result.projects()).allMatch(link->link.url().matches("/projects/project-(one|two|three|four|five|six)"));
 }
 @Test void generatedSensitiveOrInternalOutputNeverReturnedOrStored(){
  mock.answer="person@example.org";var result=ask(true);
  assertThat(result.code()).isEqualTo("RESPONSE_REJECTED");assertThat(result.answer()).doesNotContain(mock.answer);
  assertThat(result.projects()).isEmpty();assertThat(result.conversationStored()).isFalse();
  assertThat(history.findAll().getFirst().getAnswer()).isNull();
 }
 @Test void publishedJpaKnowledgeAndGeneratedAnswer(){
  var found=context.getBean(KnowledgeService.class).search("DEMO PROJECT 기술");assertThat(found).isNotEmpty().allMatch(Knowledge::isPublished);
  var result=ask(false);assertThat(result.code()).isEqualTo("SUCCESS");assertThat(result.answer()).isEqualTo(mock.answer);assertThat(found).noneMatch(k->k.getContent().equals(result.answer()));assertThat(result.projects()).allMatch(l->l.url().startsWith("/projects/"));
 }
 @Test void noConsentStoresOnlyMetadataAndActualTokens(){
  var result=ask(false);assertThat(result.conversationStored()).isFalse();var saved=history.findAll().getFirst();
  assertThat(saved.getQuestion()).isNull();assertThat(saved.getAnswer()).isNull();assertThat(saved.getStatus()).isEqualTo("SUCCESS");assertThat(saved.getPromptTokens()).isEqualTo(12);
  assertThat(usage.summary().get("totalQuestions")).isEqualTo(1L);assertThat(rows.findAll().getFirst().getCompletionTokens()).isEqualTo(4L);
 }
 @Test void consentStoresExpiresAndFaqCategories(){
  assertThat(ask(true).conversationStored()).isTrue();var saved=history.findAll().getFirst();assertThat(saved.getQuestion()).contains("DEMO PROJECT");assertThat(saved.getExpiresAt()).isEqualTo(clock.now.plus(Duration.ofDays(30)));assertThat(history.faq()).hasSize(1);
 }
 @Test void privacyBlocksProviderAndOriginalStorage(){
  var r=chat.ask(new Request("이메일 person@example.org DEMO PROJECT",true),true);assertThat(r.code()).isEqualTo("PRIVACY_BLOCKED");assertThat(mock.calls).isZero();assertThat(history.findAll().getFirst().getQuestion()).isNull();assertThat(rows.findAll().getFirst().getPromptTokens()).isNull();
 }
 @Test void injectionNeverCallsProvider(){
  assertThat(chat.ask(new Request("system prompt",true),true).code()).isEqualTo("PRIVACY_BLOCKED");assertThat(mock.calls).isZero();
 }

 @Test void arbitraryModelOutputCannotEscapePublicFacts(){mock.invalid=true;var r=ask(false);assertThat(r.code()).isEqualTo("RESPONSE_PARSE_ERROR");assertThat(r.answer()).doesNotContain("99999");}
 @Test void quotaFailureRecordedWithoutRetryOrTokens(){mock.fail=true;mock.quota=true;assertThat(ask(false).code()).isEqualTo("GEMINI_RATE_LIMITED");assertThat(mock.calls).isEqualTo(1);assertThat(history.findAll().getFirst().getStatus()).isEqualTo("RATE_LIMITED");assertThat(usage.summary().get("geminiQuotaExceededCount")).isEqualTo(1L);assertThat(ask(false).code()).isEqualTo("GEMINI_RATE_LIMITED");assertThat(mock.calls).isEqualTo(1);}
 @Test void providerErrorIsDistinct(){mock.fail=true;assertThat(ask(false).code()).isEqualTo("API_ERROR");assertThat(usage.summary().get("apiErrorCount")).isEqualTo(1L);}
 @Test void rejectedDailyRequestIsCountedWithoutAdmissionOrApiCall(){
  for(int i=0;i<50;i++)usage.admit(true);
  var result=ask(true);
  assertThat(result.code()).isEqualTo("APP_DAILY_LIMITED");
  assertThat(result.answer()).isEqualTo("오늘의 AI 질문 한도가 소진되었습니다.\n내일 다시 이용해 주세요.");
  assertThat(result.conversationStored()).isFalse();assertThat(mock.calls).isZero();
  var row=rows.findAll().getFirst();assertThat(row.getRequestCount()).isEqualTo(51);
  assertThat(row.getAdmittedCount()).isEqualTo(50);assertThat(row.getApiRequestCount()).isZero();
  assertThat(usage.summary().get("rateLimitedCount")).isEqualTo(1L);
  assertThat(history.findAll().getFirst().getQuestion()).isNull();
 }
 @Test void rejectedSessionRequestIsCountedWithoutSpendingDailyAdmission(){
  var result=chat.ask(new Request("DEMO PROJECT",true),120);
  assertThat(result.code()).isEqualTo("SESSION_RATE_LIMITED");
  assertThat(result.answer()).isEqualTo("잠시 질문이 많아졌어요.\n잠시 후 다시 시도해 주세요.");
  assertThat(result.limits().retryAfterSeconds()).isEqualTo(120);assertThat(mock.calls).isZero();
  var row=rows.findAll().getFirst();assertThat(row.getRequestCount()).isEqualTo(1);
  assertThat(row.getAdmittedCount()).isZero();assertThat(row.getApiRequestCount()).isZero();
  assertThat(usage.summary().get("rateLimitedCount")).isEqualTo(1L);
 }
 @Test void midnightDuringProcessingDoesNotClaimDailyExhaustion(){
  var day=usage.admit(true).date();clock.now=Instant.parse("2026-10-19T07:00:00Z");
  var reservation=usage.reserveApiDetailed(day);
  assertThat(reservation.allowed()).isFalse();assertThat(reservation.code()).isEqualTo("GEMINI_RATE_LIMITED");
  assertThat(reservation.retryAfterSeconds()).isEqualTo(1);
  assertThat(rows.findAll().getFirst().getApiRequestCount()).isZero();
 }
 @Test void concurrentDailyAdmissionsNeverExceedFifty() throws Exception {
  try(var pool=Executors.newFixedThreadPool(8)){var jobs=new ArrayList<Callable<Boolean>>();for(int i=0;i<70;i++)jobs.add(()->usage.admit(true).allowed());long admitted=0;for(var r:pool.invokeAll(jobs))if(r.get())admitted++;assertThat(admitted).isEqualTo(50);}
  assertThat(rows.findAll().getFirst().getRequestCount()).isEqualTo(70);assertThat(usage.admit(true).allowed()).isFalse();
 }
 @Test void concurrentApiReservationsNeverExceedPacing() throws Exception {
  var day=usage.admit(true).date();try(var pool=Executors.newFixedThreadPool(8)){var jobs=new ArrayList<Callable<Boolean>>();for(int i=0;i<8;i++)jobs.add(()->usage.reserveApi(day));long allowed=0;for(var r:pool.invokeAll(jobs))if(r.get())allowed++;assertThat(allowed).isEqualTo(1);}
 }
 @Test void newSessionsAndSpoofedProxyHeadersCannotBypassGlobalDailyCap() throws Exception {
  for(int i=0;i<50;i++)assertThat(usage.admit(true).allowed()).isTrue();
  for(String ip:List.of("1.2.3.4","127.0.0.1","8.8.8.8")) {
   // Fresh client, no session cookie, attacker-controlled forwarding headers.
   var result=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/api/portfolio-chat"))
    .header("Origin","http://localhost:5173").header("Content-Type","application/json")
    .header("X-Forwarded-For",ip).header("Forwarded","for="+ip)
    .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"DEMO PROJECT\"}")).build(),HttpResponse.BodyHandlers.ofString());
   assertThat(result.statusCode()).isEqualTo(429);assertThat(result.body()).contains("APP_DAILY_LIMITED");
  }
  assertThat(mock.calls).isZero();assertThat(rows.findAll().getFirst().getApiRequestCount()).isZero();
 }
 @Test void expiredHistoryDeletedWithoutDeletingUsageOrKnowledge(){ask(true);long before=context.getBean(KnowledgeRepository.class).count();clock.now=clock.now.plus(Duration.ofDays(30));assertThat(usage.purge()).isEqualTo(1);assertThat(history.count()).isZero();assertThat(rows.count()).isEqualTo(1);assertThat(context.getBean(KnowledgeRepository.class).count()).isEqualTo(before);}
 @Test void dailyStatsZeroFillAndPacificMidnight(){ask(false);assertThat(usage.daily(7)).hasSize(7);assertThat(usage.daily(7).getLast().get("questions")).isEqualTo(1L);clock.now=Instant.parse("2026-10-19T07:00:00Z");assertThat(usage.summary().get("todayQuestions")).isEqualTo(0L);}
 @Test void browserSessionHasRollingFiveRequestLimit(){
  var session=org.mockito.Mockito.mock(jakarta.servlet.http.HttpSession.class);var attributes=new HashMap<String,Object>();
  org.mockito.Mockito.when(session.getAttribute(org.mockito.Mockito.anyString())).thenAnswer(a->attributes.get(a.getArgument(0)));
  org.mockito.Mockito.doAnswer(a->{attributes.put(a.getArgument(0),a.getArgument(1));return null;}).when(session).setAttribute(org.mockito.Mockito.anyString(),org.mockito.Mockito.any());
  var limiter=new ChatSessionLimiter(clock);for(int i=0;i<5;i++)assertThat(limiter.allow(session)).isTrue();assertThat(limiter.allow(session)).isFalse();clock.now=clock.now.plusSeconds(600);assertThat(limiter.allow(session)).isTrue();
 }
 @Test void requestPayloadHasNoToolsHistoryOrKeys(){var p=context.getBean(GeminiProvider.class).payload("DEMO PROJECT",context.getBean(KnowledgeService.class).search("DEMO PROJECT"));assertThat(p).doesNotContainKeys("tools","cachedContent","apiKey");assertThat(p.get("generationConfig").toString()).contains("maxOutputTokens=1024");assertThat(p.get("contents").toString()).doesNotContain("test-only-not-a-real-key");}
 @Test void localAdminRejectsBrowserAndProxyAndHistoryIsNotPublic() throws Exception {
  var client=HttpClient.newHttpClient();
  for(String header:List.of("Origin","X-Forwarded-For","Forwarded","Sec-Fetch-Site")){
   var r=client.send(HttpRequest.newBuilder(URI.create(base+"/api/ai-admin/summary")).header(header,"http://localhost:5173").GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).isIn(401,403);
  }
  var r=client.send(HttpRequest.newBuilder(URI.create(base+"/api/ai-admin/summary")).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).isEqualTo(200);
  r=client.send(HttpRequest.newBuilder(URI.create(base+"/api/ai-admin/history")).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).isEqualTo(404);
  r=client.send(HttpRequest.newBuilder(URI.create(base+"/api/portfolio-chat")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"DEMO PROJECT\"}")).build(),HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).isEqualTo(403);
 }
 @Test void httpConsentDefaultsOffSessionAndValidation() throws Exception {
  var cookies=new CookieManager();cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);var client=HttpClient.newBuilder().cookieHandler(cookies).build();
  for(int i=0;i<6;i++) {
   clock.now=clock.now.plusSeconds(5);
   var r=client.send(HttpRequest.newBuilder(URI.create(base+"/api/portfolio-chat")).header("Origin","http://localhost:5173").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"우주여행 음식\"}")).build(),HttpResponse.BodyHandlers.ofString());
   assertThat(r.statusCode()).isEqualTo(i<5?200:429);assertThat(r.body()).doesNotContain("test-only-not-a-real-key");
   if(i==5){assertThat(r.body()).contains("SESSION_RATE_LIMITED","retryAfterSeconds\":575");assertThat(r.headers().firstValue("Retry-After")).contains("575");assertThat(mock.calls).isEqualTo(5);}
  }
  assertThat(history.findAll()).allMatch(h->h.getQuestion()==null && h.getAnswer()==null);
  var r=client.send(HttpRequest.newBuilder(URI.create(base+"/api/portfolio-chat")).header("Origin","http://localhost:5173").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"question\":\""+"a".repeat(301)+"\"}")).build(),HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).isEqualTo(400);assertThat(r.body()).contains("INVALID_QUESTION");
 }
 @Test void unconfirmedOrPaidModelNeverEnables(){assertThat(ChatTestSettings.create(true,false,"key","gemini-3.5-flash-lite",50,50,5,20000).available()).isFalse();assertThat(ChatTestSettings.create(true,true,"key","paid-model",50,50,5,20000).available()).isFalse();assertThat(ChatTestSettings.create(true,true,"","gemini-3.5-flash-lite",50,50,5,20000).available()).isFalse();}
}
