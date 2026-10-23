import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'

/** One boundary for anchor landing and scroll tracking, at every breakpoint. */
export function sectionAtPosition(sections: { id: string; top: number }[], marker: number, atBottom: boolean, previous = '') {
  if (atBottom) return sections.at(-1)?.id ?? ''
  const index = sections.map(section => section.top <= marker + 1).lastIndexOf(true)
  const previousIndex = sections.findIndex(section => section.id === previous)
  // A 2px dead band prevents subpixel jitter from toggling adjacent menus.
  const boundary = sections[Math.max(index, previousIndex)]
  if (previousIndex >= 0 && Math.abs(index - previousIndex) === 1 && Math.abs(boundary.top - marker) <= 2) return previous
  return sections[index]?.id ?? ''
}

export function trackSections(onActive: (id: string) => void, anchor = '') {
  const sections = Array.from(document.querySelectorAll<HTMLElement>('main > section[id]'))
  let pending = sections.find(section => section.id === anchor)
  let restingAnchor: { id: string; y: number } | undefined
  let active = ''
  const publish = (id: string) => { active = id; onActive(id) }
  let frame = 0
  let lastY = window.scrollY
  let stableFrames = 0
  const marker = () => parseFloat(getComputedStyle(document.documentElement).scrollPaddingTop) || 0
  const current = (previous = active) => sectionAtPosition(
    sections.map(section => ({ id: section.id, top: section.getBoundingClientRect().top })),
    marker(),
    window.scrollY > 0 && Math.ceil(window.scrollY + window.innerHeight) >= document.documentElement.scrollHeight - 2,
    previous,
  )
  const update = () => {
    frame = 0
    if (pending) {
      const maxScroll = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
      const destination = Math.min(maxScroll, Math.max(0, window.scrollY + pending.getBoundingClientRect().top - marker()))
      stableFrames = Math.abs(window.scrollY - lastY) < 0.1 ? stableFrames + 1 : 0
      lastY = window.scrollY
      // Reaching the clamped destination also handles short final sections.
      // Stable frames release an interrupted/native-clamped scroll without timers.
      if (Math.abs(window.scrollY - destination) <= 1) {
        // A short section near the bottom may never reach the marker. Keep the
        // chosen anchor at rest; the next manual movement resumes position tracking.
        restingAnchor = { id: pending.id, y: window.scrollY }
        pending = undefined
      } else if (stableFrames >= 8) {
        pending = undefined
        active = ''
      } else {
        frame = requestAnimationFrame(update)
        return
      }
    }
    if (restingAnchor && Math.abs(window.scrollY - restingAnchor.y) <= 1) publish(restingAnchor.id)
    else {
      restingAnchor = undefined
      publish(current())
    }
  }
  const schedule = () => { if (!frame) frame = requestAnimationFrame(update) }
  const interrupt = () => {
    if (restingAnchor) { restingAnchor = undefined; schedule() }
    if (!pending) return
    pending = undefined
    // The click highlight is not a position sample. Do not feed it into the
    // boundary dead band after an interrupted smooth scroll.
    active = ''
    window.scrollTo({ top: window.scrollY, behavior: 'instant' })
    schedule()
  }
  const keydown = (event: KeyboardEvent) => {
    if (['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End', ' '].includes(event.key)
      && !(event.target instanceof Element && event.target.closest('input, textarea, select, [contenteditable="true"]'))) interrupt()
  }
  if (pending) {
    publish(pending.id)
    pending.scrollIntoView({ block: 'start' })
    schedule()
  } else update()
  window.addEventListener('scroll', schedule, { passive: true })
  window.addEventListener('resize', schedule)
  window.addEventListener('wheel', interrupt, { passive: true })
  window.addEventListener('touchstart', interrupt, { passive: true })
  window.addEventListener('pointerdown', interrupt, { passive: true })
  window.addEventListener('keydown', keydown)
  // Images/fonts can change section positions without a window resize.
  const observer = new ResizeObserver(schedule)
  const main = document.querySelector('main')
  if (main) observer.observe(main)
  return () => {
    cancelAnimationFrame(frame)
    observer.disconnect()
    window.removeEventListener('scroll', schedule)
    window.removeEventListener('resize', schedule)
    window.removeEventListener('wheel', interrupt)
    window.removeEventListener('touchstart', interrupt)
    window.removeEventListener('pointerdown', interrupt)
    window.removeEventListener('keydown', keydown)
  }
}

export function useSectionNavigation() {
  const { pathname, hash, key } = useLocation()
  const [active, setActive] = useState('')
  useEffect(() => {
    if (pathname !== '/') return
    return trackSections(setActive, hash.slice(1))
  }, [pathname, hash, key])
  return active
}
