import { useEffect, useRef } from 'react'

/** Decorative progress; never participates in content visibility or navigation. */
export function ProjectProgress() {
  const line = useRef<HTMLDivElement>(null)
  useEffect(() => {
    let frame = 0
    let disposed = false
    const update = () => {
      frame = 0
      // Gallery dialogs temporarily fix the body; retain the reading position.
      if (document.body.style.position === 'fixed') return
      const height = document.documentElement.scrollHeight - window.innerHeight
      const progress = height > 0 ? Math.min(1, Math.max(0, window.scrollY / height)) : 0
      line.current?.style.setProperty('transform', `scaleX(${progress})`)
    }
    const schedule = () => {
      if (!disposed && !frame) frame = requestAnimationFrame(update)
    }
    schedule()
    window.addEventListener('scroll', schedule, { passive: true })
    window.addEventListener('resize', schedule)
    window.addEventListener('pageshow', schedule)
    document.addEventListener('load', schedule, true)
    const observer = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(schedule) : null
    observer?.observe(document.documentElement)
    observer?.observe(document.body)
    return () => {
      disposed = true
      cancelAnimationFrame(frame)
      observer?.disconnect()
      window.removeEventListener('scroll', schedule)
      window.removeEventListener('resize', schedule)
      window.removeEventListener('pageshow', schedule)
      document.removeEventListener('load', schedule, true)
    }
  }, [])
  return <div ref={line} className="project-progress" aria-hidden="true"/>
}
