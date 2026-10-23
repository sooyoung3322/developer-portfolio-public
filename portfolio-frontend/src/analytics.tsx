import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { projects } from './data/projects'

const base = (import.meta.env.PROD ? '' : (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080')).replace(/\/$/, '')
const sent = new Set<string>()
const documentVisitId = crypto.randomUUID()
let anonymousId: string | undefined
function browserId(): string {
  if (anonymousId) return anonymousId
  try {
    const saved = localStorage.getItem('portfolio.analytics.browser')
    if (saved && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(saved)) anonymousId = saved
    else {
      anonymousId = crypto.randomUUID()
      localStorage.setItem('portfolio.analytics.browser', anonymousId)
    }
  } catch { anonymousId ??= crypto.randomUUID() }
  return anonymousId
}
async function record(path: string, eventId: string) {
  const key = `${path}:${eventId}`
  if (sent.has(key)) return
  sent.add(key) // synchronous: includes StrictMode effects and in-flight requests
  try {
    await fetch(`${base}/api/analytics/${path}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ browserId: browserId(), eventId }), credentials: 'omit',
      keepalive: true, signal: AbortSignal.timeout(5000),
    })
  } catch { /* analytics never blocks navigation; no effect-driven retries */ }
}
export function trackMarkdownDownload(): void {
  // Each successful download initiation is a separate interaction.
  void record('portfolio-markdown-download', crypto.randomUUID())
}
export function AnalyticsTracking() {
  const { pathname, key } = useLocation()
  useEffect(() => {
    void record('visits', documentVisitId)
    const entryId = viewId(key)
    const slug = /^\/projects\/([^/]+)\/?$/.exec(pathname)?.[1]
    if (slug && projects.some(project => project.id === slug)) {
      void record(`projects/${slug}/views`, entryId)
    }
  }, [pathname, key])
  useEffect(() => {
    // One delegated listener covers cards, pagination and chat project links.
    const clicked = (event: MouseEvent) => {
      if (event.button !== 0 || !(event.target instanceof Element)) return
      const link = event.target.closest('a[href]')
      if (!link) return
      const url = new URL(link.getAttribute('href')!, window.location.href)
      const slug = /^\/projects\/([^/]+)\/?$/.exec(url.pathname)?.[1]
      if (url.origin === location.origin && slug && projects.some(project => project.id === slug)) {
        void record(`projects/${slug}/clicks`, crypto.randomUUID())
      }
    }
    document.addEventListener('click', clicked, true)
    return () => document.removeEventListener('click', clicked, true)
  }, [])
  return null
}
let lastNavigation: string | undefined
let currentViewId = crypto.randomUUID()
function viewId(key: string) {
  if (lastNavigation !== key) {
    lastNavigation = key
    currentViewId = crypto.randomUUID()
  }
  return currentViewId
}
