import type { ReactNode } from 'react'
import { reviewFor } from '../contentReview'
import entries from '../data/content-review.json'
import './content-review.css'

export function ContentReview({ projectId, field, children }: { projectId: string; field: string; children: ReactNode }) {
  if (!import.meta.env.DEV) return <>{children}</>
  const entry = reviewFor(projectId, field)
  if (!entry) return <>{children}</>
  const Tag = field === 'stack' ? 'div' : 'span'
  return <Tag className="content-review" data-review-id={entry.id}>{children} <span className="content-review-badge" tabIndex={0} title={entry.reason} aria-label={`수정 필요: ${entry.reason}`}>수정 필요<span className="content-review-tooltip" role="tooltip">{entry.reason}</span></span></Tag>
}

export function SiteContentReview() {
  if (!import.meta.env.DEV) return null
  const entry = entries.find(item => item.id === 'site-description')!
  const current = document.querySelector('meta[name="description"]')?.getAttribute('content') ?? ''
  if (!/1차 시안/.test(current)) return null
  return <div className="content-review-site"><span className="content-review-badge" tabIndex={0} title={entry.reason}>수정 필요</span> 검색 설명: {current}</div>
}
