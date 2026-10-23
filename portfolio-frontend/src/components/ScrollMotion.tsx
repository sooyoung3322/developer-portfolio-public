import { useLayoutEffect } from 'react'
import { useLocation } from 'react-router-dom'

/** Enhance existing content only after an observer is available; default is visible. */
export function ScrollMotion() {
  const { pathname } = useLocation()

  useLayoutEffect(() => {
    // Detail content must always render visibly, independently of observers.
    if (pathname !== "/") return
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)')
    if (!('IntersectionObserver' in window)) return

    const elements: HTMLElement[] = []
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('motion-visible')
      })
    }, { threshold: 0, rootMargin: '0px 0px -24px 0px' })
    // Separate entry and reset boundaries prevent flicker near viewport edges.
    // Reset only when the entire element is at least 120px off screen.
    const resetObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting || preference.matches || entry.target.contains(document.activeElement)) return
        entry.target.classList.remove('motion-visible')
      })
    }, { threshold: 0, rootMargin: '120px 0px' })

    const prepare = (selector: string, direction: string, stagger = 0) => {
      document.querySelectorAll<HTMLElement>(selector).forEach((element, index) => {
        element.dataset.motion = direction
        element.style.setProperty('--motion-delay', `${stagger * index}ms`)
        elements.push(element)
        observer.observe(element)
      })
    }

    prepare('.hero > .eyebrow, .hero h1, .hero-bottom', 'up', 100)
    prepare('.section-heading', 'up')
    prepare('.profile-photo, .detail-body > aside', 'left')
    prepare('.about-content > .about-copy', 'right')
    prepare('.timeline', 'timeline')
    document.querySelectorAll('.timeline-item').forEach(item => {
      const parts = item.querySelectorAll<HTMLElement>('.timeline-date, .timeline-content h3, .timeline-role, .timeline-content ul')
      parts.forEach((element, index) => {
        element.dataset.motion = index === 0 ? 'left' : 'up'
        element.style.setProperty('--motion-delay', `${index * 100}ms`)
        elements.push(element)
        observer.observe(element)
      })
    })
    // Stagger within each row, keeping later rows responsive to their own entry.
    document.querySelectorAll<HTMLElement>('.project-card').forEach((element, index) => {
      element.dataset.motion = 'up'
      element.style.setProperty('--motion-delay', `${(index % 2) * 100}ms`)
      elements.push(element)
      observer.observe(element)
    })
    prepare('.skills-grid > .about-copy', 'up', 100)
    prepare('.closing > p, .contact-actions', 'fade', 100)
    // Keep the detail title, description and hero visible without observer callbacks.
    // Route scroll resets and the two observers can otherwise leave them hidden.
    prepare('.detail-sections > section > .eyebrow, .detail-sections h2, .detail-sections p.muted, .project-gallery, .project-pagination', 'fade')

    const revealAll = () => elements.forEach(element => element.classList.add('motion-visible'))
    elements.forEach(element => resetObserver.observe(element))
    const onPreferenceChange = () => {
      if (preference.matches) revealAll()
      else {
        elements.forEach(element => {
          const rect = element.getBoundingClientRect()
          if ((rect.bottom < -120 || rect.top > window.innerHeight + 120) && !element.contains(document.activeElement)) {
            element.classList.remove('motion-visible')
          }
        })
      }
    }
    onPreferenceChange()
    const onFocus = (event: FocusEvent) => {
      if (!(event.target instanceof Element)) return
      let target = event.target.closest<HTMLElement>('[data-motion]')
      while (target) {
        target.classList.add('motion-visible')
        target = target.parentElement?.closest<HTMLElement>('[data-motion]') ?? null
      }
    }
    preference.addEventListener('change', onPreferenceChange)
    document.addEventListener('focusin', onFocus)
    window.addEventListener('beforeprint', revealAll)
    return () => {
      observer.disconnect()
      resetObserver.disconnect()
      preference.removeEventListener('change', onPreferenceChange)
      document.removeEventListener('focusin', onFocus)
      window.removeEventListener('beforeprint', revealAll)
      elements.forEach(element => {
        delete element.dataset.motion
        element.classList.remove('motion-visible')
        element.style.removeProperty('--motion-delay')
      })
    }
  }, [pathname])

  return null
}
