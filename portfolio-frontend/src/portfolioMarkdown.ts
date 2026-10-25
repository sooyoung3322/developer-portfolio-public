import { experiences, profile, skills } from './data/profile'
import { projects, type Project } from './data/projects'

export const portfolioFilename = 'Portfolio.md'

// Export only fields rendered publicly. Never serialize entire project objects:
// repositories marked private, review metadata and internal documents are excluded.
const text = (value: string) => value.replace(/<[^>]*>/g, '').replace(/([\\`*_{}\[\]<>#|])/g, '\\$1')
const link = (label: string, url: string) => /^https:\/\//.test(url)
  ? `[${text(label)}](${url.replace(/\(/g, '%28').replace(/\)/g, '%29').replace(/\s/g, '%20')})` : text(label)

export function generatePortfolioMarkdown(projectList: readonly Project[] = projects): string {
  const lines: string[] = [`# ${text(profile.name)} — ${text(profile.title)}`, '', '## 소개', '', ...profile.introduction.flatMap(item => [text(item), '']), '## 기술 스택', '']
  const section = (title: string, paragraphs: string[]) => {
    if (paragraphs.length) lines.push(title, '', ...paragraphs.flatMap(item => [item, '']))
  }
  const bullets = (items: string[]) => items.map(item => `- ${text(item)}`).join('\n')
  for (const group of skills) section(`### ${text(group.title)}`, [bullets(group.items)])
  lines.push('## 경력', '')
  for (const job of experiences) section(`### ${text(job.company)}`, [text(`${job.period} · ${job.duration}`), text(job.role), bullets(job.tasks)])
  section('## 프로젝트 목록', [bullets(projectList.map(project => `${project.title} — ${project.period} · ${project.description}`))])
  lines.push('## 프로젝트', '')
  for (const project of projectList) {
    lines.push(`### ${text(project.title)}`, '')
    section('#### 프로젝트 개요', [text(project.description)])
    section('#### 개발 기간', [text(project.period), ...[project.periodStatus, project.note].filter((item): item is string => !!item).map(text)])
    section('#### 담당 역할', [bullets([project.role, project.team, project.responsibility, project.contribution].filter((item): item is string => !!item))])
    section('#### 기술 스택', project.stack.length ? [bullets(project.stack)] : [])
    section('#### 핵심 구현', project.highlights?.length ? [bullets(project.highlights)] : [])
    section('#### 주요 처리 흐름', project.flow?.length ? [project.flow.map((step, index) => `${index + 1}. ${text(step)}`).join('\n'), ...(project.flowNote ? [text(project.flowNote)] : [])] : [])
    if (project.achievements) {
      const result = project.achievements
      section('#### 성과', [text(result.summary), ...(result.metrics.length ? [bullets(result.metrics.map(metric => `${metric.label}: ${metric.value} — ${metric.description}`))] : []), ...(result.customers?.length ? [`그 외 고객사 계약:\n\n${bullets(result.customers)}`] : []), ...(result.note ? [text(result.note)] : []), ...(result.source ? [link(result.source.label, result.source.url)] : [])])
    }
    // Preserve original story headings, including troubleshooting and AI use,
    // instead of guessing categories from project IDs or duplicating the prose.
    for (const story of project.sections) section(`#### ${text(story.title)}`, story.paragraphs.map(text))
    if (project.repository?.public) section('#### 공개 GitHub', [link(project.repository.kind === 'case-study' ? 'GitHub 기술 사례 문서' : 'GitHub 저장소', project.repository.url)])
  }
  section('## AI 활용 경험', skills.filter(group => group.eyebrow === 'AI & AX DEVELOPMENT').map(group => bullets(group.items)))
  section('## 연락처', [`이메일: [${profile.email}](mailto:${profile.email})`, link('GitHub — ' + profile.name, profile.github)])
  return lines.join('\n').trimEnd() + '\n'
}

export function downloadPortfolioMarkdown(onStarted: () => void = () => {}): void {
  const blob = new Blob([generatePortfolioMarkdown()], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = portfolioFilename
  document.body.appendChild(anchor)
  try {
    anchor.click()
    // Saving completion is not observable. Only report successful initiation.
    window.setTimeout(() => {
      try {
        onStarted()
      } catch { /* analytics must not affect the download */ }
    }, 0)
  } finally {
    anchor.remove()
    // Allow browsers time to start consuming the Blob before releasing it.
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
  }
}
