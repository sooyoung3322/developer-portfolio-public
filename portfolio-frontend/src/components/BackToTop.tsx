import { useEffect, useRef } from 'react'

export function BackToTop() {
  const button = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const element = button.current
    if (!element) return
    let frame = 0
    let visible: boolean | undefined
    const update = () => {
      frame = 0
      const next = window.scrollY >= 500
      if (next === visible) return
      visible = next
      element.classList.toggle('is-visible', next)
      element.inert = !next
      element.setAttribute('aria-hidden', String(!next))
    }
    const schedule = () => {
      if (!frame) frame = requestAnimationFrame(update)
    }
    update()
    window.addEventListener('scroll', schedule, { passive: true })
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('scroll', schedule)
    }
  }, [])

  return <button ref={button} type="button" className="back-to-top" aria-label="맨 위로 이동" aria-hidden="true" inert onClick={() => {
    window.scrollTo({ top: 0, behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' })
  }}><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m6 12 6-6 6 6M12 6v13"/></svg></button>
}
