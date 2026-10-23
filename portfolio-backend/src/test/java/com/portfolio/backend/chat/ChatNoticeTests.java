package com.portfolio.backend.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatNoticeTests {
 @Test @SuppressWarnings("unchecked") void disabledResponseIsSafeAndNeverConsumesUsage(){
  var settings=ChatTestSettings.create(false,true,"synthetic-key","gemini-3.5-flash-lite",50,50,5,250000);
  var services=(ObjectProvider<ChatService>)mock(ObjectProvider.class);
  var service=mock(ChatService.class);when(services.getIfAvailable()).thenReturn(service);
  var limiter=mock(ChatSessionLimiter.class);var request=mock(jakarta.servlet.http.HttpServletRequest.class);
  var response=new ChatController(settings,services,limiter).ask(new ChatDtos.Request("DEMO PROJECT",true),request);
  assertThat(response.getStatusCode().value()).isEqualTo(503);
  assertThat(response.getBody().code()).isEqualTo("DISABLED");
  assertThat(response.getBody().answer()).isEqualTo("현재 AI 챗봇을 준비 중입니다.\n이용에 양해 부탁드립니다.");
  assertThat(response.getBody().conversationStored()).isFalse();verifyNoInteractions(service,limiter,request);
 }
 @Test void unexpectedServerFailureDoesNotExposeExceptionOrClaimDailyLimit(){
  var response=new ChatErrorHandler().failure(new IllegalStateException("private password and api key"));
  assertThat(response.getBody().code()).isEqualTo("SERVER_ERROR");
  assertThat(response.getBody().answer()).isEqualTo("AI 서비스 연결에 문제가 발생했습니다.\n잠시 후 다시 시도해 주세요.");
  assertThat(response.getBody().limits().limited()).isFalse();
 }
}
