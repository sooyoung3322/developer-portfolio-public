export type ChatNotice = { code: string; text: string; tone: 'warning' | 'error'; daily: boolean }

// Only server-confirmed daily protection codes may lock the composer.
export function chatNotice(code: string): ChatNotice {
  const daily = code === 'APP_DAILY_LIMITED' || code === 'GEMINI_RPD_LIMITED'
  if (daily) return { code, daily, tone: 'warning', text: '일일 AI 질문 한도에 도달했습니다.\n한도가 초기화된 뒤 다시 이용해 주세요.' }
  if (['SESSION_RATE_LIMITED', 'GEMINI_RPM_LIMITED', 'GEMINI_TPM_LIMITED'].includes(code)) {
    return { code, daily, tone: 'warning', text: '짧은 시간에 처리할 수 있는 질문이나 토큰 한도에 도달했습니다.\n잠시 후 다시 질문해 주세요.' }
  }
  if (code === 'GEMINI_RATE_LIMITED') return { code, daily, tone: 'warning', text: '현재 AI 서비스 이용량이 많아 답변하기 어렵습니다. 잠시 후 다시 이용해 주세요.' }
  if (code === 'DISABLED') return { code, daily, tone: 'error', text: '현재 AI 질문 기능을 이용할 수 없습니다.\n프로젝트 상세 페이지에서 개발 내용을 확인해 주세요.' }
  if (code === 'INVALID_QUESTION') return { code, daily, tone: 'error', text: '질문은 1~300자로 입력해 주세요.' }
  if (code === 'PRIVACY_BLOCKED') return { code, daily, tone: 'error', text: '개인정보나 비공개 정보를 제외하고 공개된 경력과 프로젝트에 대해 질문해 주세요.' }
  if (code === 'NETWORK_ERROR') return { code, daily, tone: 'error', text: '서버 응답을 받지 못했습니다.\n인터넷 연결을 확인하고 잠시 후 다시 시도해 주세요.' }
  if (code === 'KNOWLEDGE_ERROR') return { code, daily, tone: 'error', text: '답변에 필요한 포트폴리오 자료를 불러오지 못했습니다.\n잠시 후 다시 질문해 주세요.' }
  return { code, daily, tone: 'error', text: 'AI 답변을 처리하지 못했습니다.\n잠시 후 다시 질문해 주세요.' }
}

export type ChatMessage = { role: 'user' | 'assistant'; text: string; projects?: { title: string; url: string }[]; notice?: ChatNotice }

export function appendNotice(messages: ChatMessage[], notice: ChatNotice): ChatMessage[] {
  // Move a repeated notice below the latest question, keeping all conversation text.
  return [...messages.filter(message => message.notice?.text !== notice.text), { role: 'assistant', text: notice.text, notice }]
}
