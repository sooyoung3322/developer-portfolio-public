import { createContext, useContext, type ReactNode } from 'react'

export const chatSuggestions = ['어떤 백엔드 프로젝트를 담당했나요?', 'Spring Boot 실무 경험을 알려주세요.', 'AWS 서버 운영 경험이 있나요?', 'DEMO PROJECT 프로젝트에서는 어떤 역할을 했나요?']
export const ChatAccessContext = createContext<(question?: string) => void>(() => {})

export function ChatButton({ children, question, className = 'chat-open-button' }: { children: ReactNode; question?: string; className?: string }) {
  const open = useContext(ChatAccessContext)
  return <button type="button" className={className} aria-haspopup="dialog" aria-controls="portfolio-chat" onClick={() => open(question)}>{children}<span aria-hidden="true">↗</span></button>
}

export function ChatIntroduction() {
  return <div className="chat-introduction" aria-label="AI 포트폴리오 안내">
    <div className="chat-introduction-heading"><div><p className="eyebrow">AI / PORTFOLIO GUIDE</p><p>포트폴리오에 대해 궁금한 점이 있나요?</p></div><ChatButton>AI에게 질문하기</ChatButton></div>
    <p className="muted chat-introduction-copy">제 경력과 프로젝트, 사용 기술에 대해 질문할 수 있는 기능입니다. 이용 가능 여부는 질문 창에서 확인해 주세요.</p>
  </div>
}
