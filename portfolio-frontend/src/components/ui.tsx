import { ContentReview } from './ContentReview'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { Project } from '../data/projects'

export function Arrow() { return <span aria-hidden="true">↗</span> }

export function SectionHeading({ number, eyebrow, title, children }: { number: string; eyebrow: string; title: string; children?: ReactNode }) {
  return <div className="section-heading"><div><p className="eyebrow"><span>{number}</span> / {eyebrow}</p><h2>{title}</h2></div>{children}</div>
}

export function Placeholder({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`placeholder ${className}`}><span className="placeholder-mark" aria-hidden="true">＋</span>{children}</div>
}

export function ProjectVisual({ project }: { project: Project }) {
  return <div className={`project-visual visual-${project.tone}`}>
    {project.images[0] && <img className="thumbnail-image" src={project.images[0].src} alt="" loading="lazy"/>}
    <div className="visual-top"><span>PROJECT {project.number}</span><span>{project.number === '01' || project.number === '02' ? 'FEATURED PROJECT' : 'SELECTED PROJECT'}</span></div>
    <p className="wordmark">{project.wordmark}</p>
    <div className="visual-bottom"><span>{project.category}</span><span aria-hidden="true">↗</span></div>
  </div>
}

export function StackTags({ stack }: { stack: string[] }) {
  return <ul className="stack-tags" aria-label="기술 스택">{stack.map(tag => <li key={tag}>{tag}</li>)}</ul>
}

export function ProjectCard({ project }: { project: Project }) {
  return <article className="project-card"><Link to={`/projects/${project.id}`} aria-label={`${project.title} 상세 페이지 보기`}>
    <ProjectVisual project={project}/><div className="card-info"><div><p className="eyebrow"><ContentReview projectId={project.id} field="period">{project.period}</ContentReview></p><h3>{project.title}</h3></div><span className="circle-arrow"><Arrow/></span></div>
    <p className="card-role">{project.cardRole}</p><p className="muted card-description">{project.description}</p><StackTags stack={project.stack.slice(0, 4)}/>
  </Link></article>
}
