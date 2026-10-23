package com.portfolio.backend.chat;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class GeminiProviderTests {
 ChatSettings settings=ChatTestSettings.create(true,true,"test-only","gemini-3.5-flash-lite",50,50,5,250000);
 Knowledge evidence(){var k=mock(Knowledge.class);when(k.getId()).thenReturn(1L);when(k.getTitle()).thenReturn("Project");when(k.getContent()).thenReturn("Public fact");return k;}
 @SuppressWarnings("unchecked") HttpClient mockHttp(int status,String body) throws Exception {
  var http=mock(HttpClient.class);var response=(HttpResponse<InputStream>)mock(HttpResponse.class);
  when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
  when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);return http;
 }
 @Test void actualResponseTokensAndValidatedJsonParsed() throws Exception {
  var http=mockHttp(200,"{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{\\\"answer\\\":\\\"Generated answer\\\",\\\"evidenceIds\\\":[1]}\"}]}}],\"usageMetadata\":{\"promptTokenCount\":42,\"candidatesTokenCount\":9}}");
  var result=new GeminiProvider(settings,http).generate("DEMO PROJECT",List.of(evidence()));assertThat(result.evidenceIds()).containsExactly(1L);assertThat(result.promptTokens()).isEqualTo(42);assertThat(result.completionTokens()).isEqualTo(9);
  verify(http,times(1)).send(argThat(r->r.uri().getHost().equals("generativelanguage.googleapis.com") && r.uri().getPath().endsWith("/gemini-3.5-flash-lite:generateContent") && r.headers().firstValue("x-goog-api-key").orElse("").equals("test-only")),any(HttpResponse.BodyHandler.class));
 }
 @Test void newModelUsesMinimalThinkingWithExistingStructuredAnswerContract(){
  var payload=new GeminiProvider(settings,mock(HttpClient.class)).payload("question",List.of(evidence()));
  var generation=(Map<?,?>)payload.get("generationConfig");
  assertThat(generation.get("thinkingConfig")).isEqualTo(Map.of("thinkingLevel","minimal"));
  assertThat(generation.get("maxOutputTokens")).isEqualTo(1024);
  assertThat(generation.get("responseMimeType")).isEqualTo("application/json");
 }
 @Test void missingUsageRemainsNull() throws Exception {
  var result=new GeminiProvider(settings,mockHttp(200,"{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{\\\"answer\\\":\\\"Generated answer\\\",\\\"evidenceIds\\\":[]}\"}]}}]}")).generate("question",List.of());assertThat(result.promptTokens()).isNull();assertThat(result.completionTokens()).isNull();
 }
 @Test void quotaAndFailureNeverReturnProviderBodyOrRetry() throws Exception {
  var http=mockHttp(429,"sensitive upstream error");assertThatThrownBy(()->new GeminiProvider(settings,http).generate("question",List.of())).isInstanceOf(ChatProvider.Failure.class).satisfies(e->assertThat(((ChatProvider.Failure)e).quota).isTrue()).hasMessage("AI provider unavailable");verify(http,times(1)).send(any(),any());
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(ints={500,502,503})
 void serviceFailuresAreNotDailyOrQuotaErrors(int status) throws Exception {
  var http=mockHttp(status,"private provider diagnostics and key");
  assertThatThrownBy(()->new GeminiProvider(settings,http).generate("question",List.of()))
   .isInstanceOf(ChatProvider.Failure.class).hasMessage("AI provider unavailable")
   .satisfies(e->{var f=(ChatProvider.Failure)e;assertThat(f.code).isEqualTo("API_ERROR");assertThat(f.quota).isFalse();});
  verify(http,times(1)).send(any(),any());
 }
 @Test void networkFailureIsSafeAndNeverRetried() throws Exception {
  var http=mock(HttpClient.class);
  when(http.send(any(),any())).thenThrow(new IOException("private network diagnostics"));
  assertThatThrownBy(()->new GeminiProvider(settings,http).generate("question",List.of()))
   .isInstanceOf(ChatProvider.Failure.class).hasMessage("AI provider unavailable")
   .satisfies(e->assertThat(((ChatProvider.Failure)e).code).isEqualTo("API_ERROR"));
  verify(http,times(1)).send(any(),any());
 }
 @Test void invalidJsonStillPreservesReportedUsage() throws Exception {
  var provider=new GeminiProvider(settings,mockHttp(200,"{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"not json\"}]}}],\"usageMetadata\":{\"promptTokenCount\":17,\"candidatesTokenCount\":3}}"));
  assertThatThrownBy(()->provider.generate("question",List.of())).isInstanceOf(ChatProvider.Failure.class).satisfies(e->{assertThat(((ChatProvider.Failure)e).code).isEqualTo("RESPONSE_PARSE_ERROR");assertThat(((ChatProvider.Failure)e).promptTokens).isEqualTo(17);assertThat(((ChatProvider.Failure)e).completionTokens).isEqualTo(3);});
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"not json","null","[]","{}"})
 void malformedEnvelopesAreParsingErrors(String body) throws Exception {
  var provider=new GeminiProvider(settings,mockHttp(200,body));
  assertThatThrownBy(()->provider.generate("question",List.of())).isInstanceOf(ChatProvider.Failure.class)
   .satisfies(e->assertThat(((ChatProvider.Failure)e).code).isEqualTo("RESPONSE_PARSE_ERROR"));
 }
 @Test void oversizedCorpusNeverCallsHttp() {
  var http=mock(HttpClient.class);var fact=evidence();when(fact.getContent()).thenReturn("x".repeat(16000));
  assertThatThrownBy(()->new GeminiProvider(settings,http).generate("question",List.of(fact)))
   .isInstanceOf(ChatProvider.Failure.class).satisfies(e->assertThat(((ChatProvider.Failure)e).code).isEqualTo("CONTEXT_TOO_LARGE"));
  verifyNoInteractions(http);
 }
 @Test void privacyPatterns(){var p=new PrivacyGuard();for(String value:List.of("010-1234-5678","name@example.org","이름은 김철수","주소: 서울시 강남구","AIza12345678901234567890123456","password=abc"))assertThat(p.sensitive(value)).isTrue();assertThat(p.sensitive("DEMO PROJECT 기술 소개")).isFalse();}
}
