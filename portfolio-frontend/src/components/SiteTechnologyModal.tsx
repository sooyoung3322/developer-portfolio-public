import { useEffect, useRef, useState } from 'react'
import { siteSourceUrl, siteTechnology } from '../data/siteTechnology'

export function SiteTechnologyModal() {
  const trigger = useRef<HTMLButtonElement>(null)
  const dialog = useRef<HTMLDialogElement>(null)
  const closeButton = useRef<HTMLButtonElement>(null)
  const [open, setOpen] = useState(false)
  const closing = useRef(false)
  const animate = (entering: boolean) => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return Promise.resolve()
    const frames = [{ opacity: 0, transform: 'scale(.97)' }, { opacity: 1, transform: 'scale(1)' }]
    const options = { duration: 240, easing: 'ease-out', fill: 'both' as const }
    const panel = dialog.current!.animate(entering ? frames : [...frames].reverse(), options)
    const backdrop = dialog.current!.animate([{ opacity: entering ? 0 : 1 }, { opacity: entering ? 1 : 0 }], { ...options, pseudoElement: '::backdrop' })
    return Promise.all([panel.finished, backdrop.finished]).catch(() => {})
  }
  const close = async () => {
    if (closing.current || !dialog.current?.open) return
    closing.current = true
    await animate(false)
    dialog.current?.close()
  }

  useEffect(() => {
    if (!open) return
    const element = dialog.current!
    const previousOverflow = document.body.style.overflow
    const previousPadding = document.body.style.paddingRight
    const scrollbar = window.innerWidth - document.documentElement.clientWidth
    document.body.style.overflow = 'hidden'
    if (scrollbar > 0) document.body.style.paddingRight = `${parseFloat(getComputedStyle(document.body).paddingRight) + scrollbar}px`
    closing.current = false
    element.showModal()
    closeButton.current?.focus({ preventScroll: true })
    void animate(true)
    return () => {
      element.getAnimations().forEach(animation => animation.cancel())
      element.close()
      document.body.style.overflow = previousOverflow
      document.body.style.paddingRight = previousPadding
      trigger.current?.focus({ preventScroll: true })
    }
  }, [open])

  return <>
    <button ref={trigger} type="button" className="site-tech-trigger" aria-haspopup="dialog" aria-controls="site-technology" aria-expanded={open} onClick={() => setOpen(true)}>
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="m8 7-5 5 5 5m8-10 5 5-5 5m-3-14-2 18"/></svg>
      <span>이 사이트는?</span>
    </button>
    <dialog ref={dialog} id="site-technology" className="site-tech-dialog" aria-modal="true" aria-labelledby="site-tech-title" aria-describedby="site-tech-description"
      onCancel={event => { event.preventDefault(); void close() }}
      onClose={() => setOpen(false)}
      onClick={event => {
        if (event.target !== event.currentTarget) return
        const bounds = event.currentTarget.getBoundingClientRect()
        if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) void close()
      }}
      onKeyDown={event => {
        if (event.key !== 'Tab') return
        const items = Array.from(event.currentTarget.querySelectorAll<HTMLElement>('button, a[href]'))
        const first = items[0], last = items[items.length - 1]
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
      }}>
      <button ref={closeButton} type="button" className="site-tech-close" aria-label="기술 소개 닫기" onClick={() => void close()}><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18"/></svg></button>
      <p className="eyebrow site-tech-kicker">BEHIND THIS SITE / DEVELOPMENT NOTES</p>
      <h2 id="site-tech-title">이 사이트는 이렇게 만들었습니다.</h2>
      <p id="site-tech-description">백엔드는 Java와 Spring Boot로, 화면은 React로 구현한 개인 포트폴리오입니다.</p>
      <div className="site-tech-grid">{siteTechnology.map(group => <section key={group.label} aria-label={group.label}>
        <h3 className="eyebrow">{group.label}</h3>
        <ul>{group.technologies.map(technology => <li key={technology}>{technology}</li>)}</ul>
        {group.paragraphs.map(paragraph => <p key={paragraph}>{paragraph}</p>)}
      </section>)}</div>
      {siteSourceUrl && <div className="site-tech-source"><a className="text-link" href={siteSourceUrl} target="_blank" rel="noopener noreferrer">GitHub에서 소스 보기 <span aria-hidden="true">↗</span><span className="site-tech-sr-only">(새 탭에서 열림)</span></a></div>}
    </dialog>
  </>
}
