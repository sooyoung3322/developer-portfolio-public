import entries from './data/content-review.json'
import { projects } from './data/projects'

export function reviewFor(projectId: string, field: string) {
  const project = projects.find(project => project.id === projectId)
  const entry = entries.find(entry => entry.projectId === projectId && entry.field === field)
  if (!entry || !project) return undefined
  if (field === 'period' && project.periodStatus) return entry
  const value = project[field as 'team' | 'period' | 'stack']
  const missing = Array.isArray(value) ? !value.length : !value.trim() || /미기재|확인 필요|추후 수정|임시|TODO|FIXME|TBD|준비 중/i.test(value)
  if (entry.status === 'resolved' && entry.resolution.trim() && !missing) return undefined
  if (field !== 'stack' && !missing) return undefined
  return entry
}
