import { useEffect, useRef, useState, type ReactNode } from 'react'
import { ChatAccessContext, chatSuggestions } from './ChatAccess'
import { Link } from 'react-router-dom'
import './portfolio-chat.css'
import { appendNotice, chatNotice, type ChatMessage } from './chat-notices'

type Reply = { answer: string; code: string; projects: { title: string; url: string }[]; limits: { limited: boolean; retryAfterSeconds: number }; conversationStored: boolean }
const base = (import.meta.env.PROD ? '' : (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080')).replace(/\/$/, '')

export function PortfolioChat({ children }: { children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const returnFocus = useRef<HTMLElement | null>(null)
  const input = useRef<HTMLTextAreaElement>(null)
  const end = useRef<HTMLDivElement>(null)
  const inFlight = useRef(false)
  const request = useRef<AbortController | null>(null)
  const [open, setOpen] = useState(false)
  const [availability, setAvailability] = useState<'checking' | 'enabled' | 'disabled' | 'offline' | 'error' | 'timeout'>('checking')
  const available = useRef(false)
  const updateAvailability = (value: typeof availability) => {
    // The ref blocks stale event handlers before React renders the new status.
    available.current = value === 'enabled'
    setAvailability(value)
  }
  const [statusAttempt, setStatusAttempt] = useState(0)
  const enabled = availability === 'enabled'
  const availabilityMessage = {
    checking: 'AI 질문 기능을 확인하고 있습니다.',
    enabled: '',
    disabled: '현재 AI 질문 기능을 이용할 수 없습니다. 경력과 프로젝트 정보는 포트폴리오에서 확인하실 수 있습니다.',
    offline: '현재 서버에 연결할 수 없어 AI 질문 기능을 이용할 수 없습니다. 잠시 후 다시 시도해 주세요.',
    error: 'AI 질문 기능의 상태를 확인하지 못했습니다. 잠시 후 다시 확인해 주세요.',
    timeout: 'AI 질문 기능의 상태 확인 시간이 초과되었습니다. 잠시 후 다시 확인해 주세요.',
  }[availability]
  const [dailyUntil, setDailyUntil] = useState(0)
  const dailyLocked = dailyUntil > Date.now()
  useEffect(() => {
    if (!dailyUntil || dailyUntil === Number.MAX_SAFE_INTEGER) return
    const timer = window.setTimeout(() => setDailyUntil(0), Math.max(0, dailyUntil - Date.now()))
    return () => clearTimeout(timer)
  }, [dailyUntil])
  const showNotice = (code: string) => setMessages(previous => appendNotice(previous, chatNotice(code)))
  const [question, setQuestion] = useState('')
  const [consent, setConsent] = useState(false)
  const [busy, setBusy] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [status, setStatus] = useState('')
  useEffect(() => () => request.current?.abort(), [])
  useEffect(() => {
    if (!open) return
    updateAvailability('checking')
    dialog.current?.showModal()
    const controller = new AbortController()
    let active = true
    let timedOut = false
    let responded = false
    const statusTimeout = window.setTimeout(() => { timedOut = true; controller.abort() }, 10000)
    fetch(`${base}/api/portfolio-chat/status`, { signal: controller.signal })
      .then(r => { responded = true; if (!r.ok) throw new Error(); return r.json() })
      .then((data: { enabled: boolean }) => {
        if (!active || controller.signal.aborted) return
        if (typeof data.enabled !== 'boolean') throw new Error()
        updateAvailability(data.enabled ? 'enabled' : 'disabled'); setStatus('')
      })
      .catch(() => { if (active) { updateAvailability(timedOut ? 'timeout' : responded ? 'error' : 'offline'); setStatus('') } })
      .finally(() => clearTimeout(statusTimeout))
    return () => { active = false; clearTimeout(statusTimeout); controller.abort() }
  }, [open, statusAttempt])
  useEffect(() => { if (open && enabled) input.current?.focus() }, [open, enabled])
  useEffect(() => { end.current?.scrollIntoView({ block: 'nearest' }) }, [messages, busy])
  const openChat = (suggestedQuestion?: string) => {
    returnFocus.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    if (suggestedQuestion && !inFlight.current) setQuestion(suggestedQuestion)
    if (!open) updateAvailability('checking')
    setOpen(true)
  }
  const close = () => {
    dialog.current?.close()
    setOpen(false)
    returnFocus.current?.focus({ preventScroll: true })
  }
  const send = async () => {
    const text = question.trim()
    if (!text || text.length > 300 || inFlight.current || dailyLocked || !available.current) return
    inFlight.current = true
    setBusy(true)
    setStatus('')
    setMessages(previous => [...previous, { role: 'user', text }])
    const controller = new AbortController()
    request.current = controller
    const timeout = window.setTimeout(() => controller.abort(), 30000)
    try {
      const response = await fetch(`${base}/api/portfolio-chat`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        signal: controller.signal,
        body: JSON.stringify({ question: text, saveConversation: consent }),
      })
      const data: Reply = await response.json()
      if (typeof data.code !== 'string') throw new Error()
      if (!response.ok || !['SUCCESS', 'NO_CONTEXT'].includes(data.code)) {
        const notice = chatNotice(data.code)
        setMessages(previous => appendNotice(previous, notice))
        if (notice.daily) {
          const seconds = data.limits?.retryAfterSeconds
          // Only use a bounded, server-provided reset delay; never infer a provider reset.
          setDailyUntil(typeof seconds === 'number' && Number.isFinite(seconds) && seconds > 0 && seconds <= 90000 ? Date.now() + seconds * 1000 : Number.MAX_SAFE_INTEGER)
        }
        if (data.code === 'DISABLED') updateAvailability('disabled')
        return
      }
      if (typeof data.answer !== 'string' || !Array.isArray(data.projects)) throw new Error()
      setQuestion('')
      const projectLinks = data.projects.filter(project =>
        typeof project?.title === 'string'
        && typeof project.url === 'string'
        && /^\/projects\/(project-one|project-two|project-three|project-four|project-five|project-six)$/.test(project.url),
      )
      setMessages(previous => [...previous, { role: 'assistant', text: data.answer, projects: projectLinks }])
      setStatus(data.conversationStored ? '동의에 따라 이번 질문·답변을 최대 30일 보관합니다.' : '이번 질문·답변 원문은 저장하지 않았습니다.')
    } catch {
      updateAvailability('offline')
      showNotice('NETWORK_ERROR')
      setStatus('')
    } finally {
      clearTimeout(timeout)
      inFlight.current = false
      setBusy(false)
      request.current = null
    }
  }
  return <ChatAccessContext.Provider value={openChat}>
    {children}
    <dialog ref={dialog} id="portfolio-chat" className="portfolio-chat" aria-labelledby="portfolio-chat-title" onCancel={event => { event.preventDefault(); close() }} onClose={() => setOpen(false)} onClick={event => { if (event.target === event.currentTarget) { const rect = event.currentTarget.getBoundingClientRect(); if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) close() } }}>
      <div className="portfolio-chat-heading"><div><p className="eyebrow">PORTFOLIO / AI GUIDE</p><h2 id="portfolio-chat-title">포트폴리오에 대해 궁금한 점이 있나요?</h2></div><button type="button" onClick={close} aria-label="AI 채팅 닫기">×</button></div>
      <p className="portfolio-chat-notice">제 경력과 프로젝트, 사용 기술에 대해 질문해 보세요. 포트폴리오에 정리된 내용을 바탕으로 답변합니다.</p>
      <p className="portfolio-chat-scope">확인되지 않은 경력·프로젝트 정보는 추측하지 않습니다. AI 답변에 오류가 있을 수 있으니 중요한 내용은 프로젝트 상세와 이력서에서 확인해 주세요.</p>
      {!enabled && <div className={`portfolio-chat-availability${availability === 'checking' ? ' is-checking' : ''}`} role="status"><p>{availabilityMessage}</p>{availability !== 'checking' && <button type="button" onClick={() => { updateAvailability('checking'); setStatusAttempt(value => value + 1) }}>다시 확인</button>}</div>}
      <div className="portfolio-chat-messages" role="log" aria-live="polite" aria-relevant="additions text">
        {!messages.length && enabled && <div className="portfolio-chat-suggestions"><p className="portfolio-chat-suggestion-help">추천 질문을 선택하면 입력창에 채워집니다.</p>{chatSuggestions.map(text => <button key={text} type="button" disabled={busy || dailyLocked || enabled !== true} onClick={() => { if (!available.current || inFlight.current || dailyLocked) return; setQuestion(text); input.current?.focus() }}>{text} ↗</button>)}</div>}
        {messages.map((message, index) => <div key={index} className={`portfolio-chat-message is-${message.role}${message.notice ? ` portfolio-chat-alert is-${message.notice.tone}` : ''}`}><p className="eyebrow">{message.role === 'user' ? 'YOU' : 'AI / PORTFOLIO GUIDE'}</p><p>{message.notice && <span className="portfolio-chat-alert-icon" aria-hidden="true">{message.notice.tone === 'warning' ? '\u26a0' : '\u24d8'}</span>}{message.text}</p>{message.projects?.map(project => <Link key={project.url} to={project.url} onClick={close}>{project.title} · 프로젝트 보기 ↗</Link>)}</div>)}
        {busy && <p className="portfolio-chat-loading" role="status">공개 자료를 확인하고 있습니다<span aria-hidden="true">···</span></p>}<div ref={end}/>
      </div>
      <form onSubmit={event => { event.preventDefault(); void send() }}>
        <div className="portfolio-chat-consent-area">
          <div className="portfolio-chat-consent-row">
            <label className="portfolio-chat-consent"><input type="checkbox" checked={consent} disabled={busy} onChange={event => setConsent(event.target.checked)} aria-describedby="portfolio-chat-consent-help"/> 질문·답변 최대 30일 저장 동의 (선택)</label>
            <button type="button" className="portfolio-chat-details-trigger" popoverTarget="portfolio-chat-details" aria-label="질문·답변 저장 및 AI 이용 안내 자세히 보기">이용 안내 <span aria-hidden="true">ⓘ</span></button>
          </div>
          <p id="portfolio-chat-consent-help" className="portfolio-chat-consent-help">저장에 동의하지 않아도 채팅을 이용할 수 있습니다.</p>
        </div>
        <div id="portfolio-chat-details" className="portfolio-chat-details" popover="auto" role="region" aria-labelledby="portfolio-chat-details-title">
          <div className="portfolio-chat-details-heading"><h3 id="portfolio-chat-details-title">저장 및 AI 이용 안내</h3><button type="button" popoverTarget="portfolio-chat-details" popoverTargetAction="hide" aria-label="이용 안내 닫기">×</button></div>
          <p className="portfolio-chat-details-intro">질문·답변 저장은 선택 사항입니다. 저장에 동의하지 않아도 채팅을 이용할 수 있습니다.</p>
          <div className="portfolio-chat-details-body">
            <section><h4>질문·답변 저장</h4><p>저장에 동의하고 서버의 저장 조건을 충족한 경우에만 입력한 질문과 AI 답변 원문을 최대 30일 보관합니다.</p><p>저장한 내용은 AI 답변 품질 확인과 서비스 개선에 사용합니다. 이번 대화의 원문 저장 여부는 답변 아래에 안내합니다.</p></section>
            <section><h4>Google Gemini API 전송</h4><p>AI 답변을 생성할 때 질문이 Google Gemini API로 전송됩니다. 이는 이 사이트의 원문 저장 동의와 별개입니다.</p><p>무료 티어에서는 입력 데이터가 Google 서비스 개선에 사용될 수 있습니다.</p></section>
            <section><h4>AI 답변 확인</h4><p>공개 포트폴리오 자료를 바탕으로 답변하지만, 내용에 오류가 있을 수 있습니다. 관련 프로젝트에서 근거를 확인해 주세요.</p></section>
            <section><h4>별도로 집계하는 운영 통계</h4><p>질문·답변 원문 저장 동의와 별개로 이용 횟수와 토큰 사용량 등 운영 통계를 집계합니다.</p></section>
            <section><h4>입력 시 주의 사항</h4><p>개인정보나 민감정보는 입력하지 마세요. 자동 탐지만으로 모든 개인정보를 걸러낼 수는 없습니다.</p></section>
          </div>
        </div>
        <label htmlFor="portfolio-chat-question" className="eyebrow">질문 · {question.length}/300</label>
        <div className="portfolio-chat-compose"><textarea ref={input} id="portfolio-chat-question" maxLength={300} rows={2} placeholder="프로젝트나 개발 경험을 질문해 주세요." value={question} disabled={busy || dailyLocked || enabled !== true} onChange={event => setQuestion(event.target.value)} onKeyDown={event => { if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) { event.preventDefault(); void send() } }} aria-describedby="portfolio-chat-status"/><button type="submit" disabled={busy || dailyLocked || enabled !== true || !question.trim()}>전송 ↗</button></div>
        <p id="portfolio-chat-status" className="portfolio-chat-status" role="status">{status || (enabled ? '질문 1~300자 · Enter로 전송 · Shift+Enter로 줄바꿈' : '')}</p>
      </form>
    </dialog>
  </ChatAccessContext.Provider>
}
