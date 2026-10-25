import { ChatButton, ChatIntroduction } from './components/ChatAccess'
import { GitHubLink } from './components/GitHubLink'
import { PortfolioChat } from './components/PortfolioChat'
import { SiteContentReview } from './components/ContentReview'
import { createPortal } from 'react-dom'
import { SiteTechnologyModal } from './components/SiteTechnologyModal'
import { AnalyticsTracking } from './analytics'
import { BackToTop } from './components/BackToTop'
import { ProjectProgress } from './components/ProjectProgress'
import { ScrollMotion } from './components/ScrollMotion'
import { useSectionNavigation } from './components/useSectionNavigation'
import { useEffect, useRef, useState } from 'react'
import { Link, Route, Routes, useLocation, useParams } from 'react-router-dom'
import { Arrow, ProjectCard, ProjectVisual, SectionHeading, StackTags } from './components/ui'
import { projects, type ProjectImage } from './data/projects'
import { experiences, profile, skills } from './data/profile'
import { MarkdownDownloadButton } from './components/MarkdownDownloadButton'
import { ContactEmail } from './components/ContactEmail'

function ScrollToTop() {
  const { pathname, hash } = useLocation()
  useEffect(() => {
    if (hash && (pathname !== '/' || !document.getElementById(hash.slice(1))?.matches('main > section[id]'))) {
      document.getElementById(hash.slice(1))?.scrollIntoView()
    } else if (!hash) window.scrollTo({ top: 0, behavior: 'instant' })
  }, [pathname, hash])
  return null
}

function Header() {
  const { pathname } = useLocation()
  const activeSection = useSectionNavigation()
  const [scrolled, setScrolled] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const header = useRef<HTMLElement>(null)
  const menuToggle = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (!menuOpen) return
    const closeOutside = (event: PointerEvent) => {
      if (event.target instanceof Node && !header.current?.contains(event.target)) setMenuOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !document.querySelector('dialog[open]')) {
        setMenuOpen(false)
        menuToggle.current?.focus({ preventScroll: true })
      }
    }
    const desktop = window.matchMedia('(min-width: 1025px)')
    const closeOnDesktop = () => { if (desktop.matches) setMenuOpen(false) }
    document.addEventListener('pointerdown', closeOutside)
    document.addEventListener('keydown', closeOnEscape)
    desktop.addEventListener('change', closeOnDesktop)
    return () => {
      document.removeEventListener('pointerdown', closeOutside)
      document.removeEventListener('keydown', closeOnEscape)
      desktop.removeEventListener('change', closeOnDesktop)
    }
  }, [menuOpen])
  useEffect(() => {
    const update = () => {
      setScrolled(window.scrollY > 24)
    }
    let frame = 0
    const schedule = () => {
      if (frame) return
      frame = requestAnimationFrame(() => { frame = 0; update() })
    }
    update()
    window.addEventListener('scroll', schedule, { passive: true })
    window.addEventListener('resize', schedule)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('scroll', schedule)
      window.removeEventListener('resize', schedule)
    }
  }, [pathname])
  const menuProps = (id: string) => {
    const active = pathname.startsWith('/projects') ? id === 'work' : pathname === '/' && activeSection === id
    return { className: active ? 'active' : '', 'aria-current': active ? 'location' as const : undefined }
  }
  return <header ref={header} className={`site-header container${scrolled ? ' is-scrolled' : ''}`}>
<div className="header-identity"><Link className="brand" to="/" aria-label="포트폴리오 홈">folio<span>.</span></Link><SiteTechnologyModal/></div>
    <button ref={menuToggle} type="button" className="mobile-menu-toggle" aria-label={menuOpen ? '메뉴 닫기' : '메뉴 열기'} aria-expanded={menuOpen} aria-controls="header-navigation" onClick={() => setMenuOpen(open => !open)}>
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d={menuOpen ? 'm6 6 12 12M18 6 6 18' : 'M4 6h16M4 12h16M4 18h16'}/></svg>
    </button>
    <nav id="header-navigation" className={menuOpen ? 'is-open' : undefined} aria-label="주 메뉴"
      onClick={event => {
        if (event.target instanceof Element && event.target.closest('a')) {
          setMenuOpen(false)
          if (menuOpen) menuToggle.current?.focus({ preventScroll: true })
        }
      }}
      onBlur={event => {
        if (!header.current?.contains(event.relatedTarget)) setMenuOpen(false)
      }}><Link to="/#about" {...menuProps('about')}>About</Link><Link to="/#experience" {...menuProps('experience')}>Experience</Link><Link to="/#work" {...menuProps('work')}>Projects</Link><Link to="/#skills" {...menuProps('skills')}>Skills</Link><Link to="/#contact" {...menuProps('contact')}>Contact</Link><ChatButton className="header-chat-link">AI 질문</ChatButton><GitHubLink href={profile.github} label="sooyoung3322 GitHub 프로필" iconOnly/><MarkdownDownloadButton className="header-download" onDownloaded={() => { setMenuOpen(false); if (menuOpen) menuToggle.current?.focus({ preventScroll: true }) }}/></nav></header>
}

function Home() {
  return <main id="main">
    <section className="hero container">
      <p className="eyebrow"><span className="blue-dot"/> PERSONAL PORTFOLIO</p>
      <h1>UI와 API를 구현하고,<br/><span className="serif">테스트로 동작을 확인합니다.</span></h1>
      <div className="hero-bottom"><p>sooyoung3322 · Java / Spring Boot / React<br/><span className="muted">React와 Spring Boot로 UI와 API를 구현하고 테스트했습니다.</span></p><Link className="text-link" to="/#work">프로젝트 살펴보기 <Arrow/></Link></div>
      <ChatIntroduction/>
      <div className="hero-index"><span>BACKEND / CLOUD / OPERATIONS</span><span>SCROLL TO EXPLORE ↓</span></div>
    </section>
    <section id="about" className="about container section">
      <SectionHeading number="01" eyebrow="ABOUT" title="개발자 소개."/>
      <div className="about-content"><img className="profile-photo" src="/images/profile-placeholder.svg" alt="공개 소스 안내 이미지" width="360" height="463"/><div className="about-copy"><p className="eyebrow">INTRODUCTION</p><h3>백엔드 개발자<br/>{profile.name}입니다.</h3><p className="muted">{profile.introduction.map((paragraph, index) => <span key={paragraph}>{index > 0 && <br/>}{paragraph}</span>)}</p><span className="outline-label">Java / Spring Boot / React</span></div></div>
    </section>
    <section id="experience" className="container section">
      <SectionHeading number="02" eyebrow="EXPERIENCE" title="주요 경력."/>
      <ol className="timeline">{experiences.map(job => <li key={job.company} className="timeline-item"><div className="timeline-date"><p>{job.period}</p><span>{job.duration}</span></div><div className="timeline-content"><h3>{job.company}</h3><p className="timeline-role">{job.role}</p><ul>{job.tasks.map(task => <li key={task}>{task}</li>)}</ul></div></li>)}</ol>
    </section>
    <section id="work" className="container section work"><SectionHeading number="03" eyebrow="SELECTED WORK" title="참여한 프로젝트."><p className="muted">프로젝트별 담당 업무와<br/>구현 내용을 확인할 수 있습니다.</p></SectionHeading><div className="project-grid">{projects.map(project => <ProjectCard key={project.id} project={project}/>)}</div></section>
    <section id="skills" className="container section">
      <SectionHeading number="04" eyebrow="SKILLS" title="사용한 기술과 개발 경험."/>
      <div className="skills-grid">
        {skills.map(group => <div key={group.eyebrow} className="about-copy"><p className="eyebrow">{group.eyebrow}</p><h3>{group.title}</h3><ul className="muted skills-details">{group.items.map(item => <li key={item}>{item}</li>)}</ul></div>)}
      </div>
    </section>
    <section id="contact" className="closing container"><p className="eyebrow">CONTACT / THANK YOU FOR READING</p><p>끝까지 읽어주셔서<br/><span className="serif">감사합니다.</span></p><p className="contact-description muted">프로젝트와 개발 경험에 대해 더 궁금한 점이 있다면 편하게 연락해 주세요.</p><ContactEmail/><div className="contact-actions"><MarkdownDownloadButton className="contact-download"/></div><details className="contact-analytics"><summary>방문 통계 안내</summary><p>방문·프로젝트 클릭·상세 조회·다운로드 횟수와 기기 유형을 집계합니다. 브라우저에 임의 식별자를 저장하고 서버에는 해시값을 보관해 중복 방문을 구분합니다. IP 통계가 활성화된 경우 원본 IP 대신 날짜별 HMAC 토큰을 저장하며, 30일이 지나면 삭제 대상이 됩니다. 통계 이벤트와 브라우저 식별자 해시는 별도 만료 없이 유지됩니다.</p></details></section>
  </main>
}

function ImageGallery({ images, previewIndex }: { images: ProjectImage[]; previewIndex: number }) {
  const [selected, setSelected] = useState<number | null>(null)
  const dialog = useRef<HTMLDialogElement>(null)
  const isOpen = selected !== null
  useEffect(() => {
    if (!isOpen) return
    const element = dialog.current
    const focused = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const { scrollX, scrollY } = window
    const original = document.body.style.cssText
    const scrollbar = window.innerWidth - document.documentElement.clientWidth
    const padding = parseFloat(getComputedStyle(document.body).paddingRight)
    Object.assign(document.body.style, { position: 'fixed', top: `-${scrollY}px`, left: `-${scrollX}px`, width: '100%', overflow: 'hidden', paddingRight: `${padding + scrollbar}px` })
    element?.showModal()
    return () => {
      element?.close()
      document.body.style.cssText = original
      window.scrollTo({ left: scrollX, top: scrollY, behavior: 'instant' })
      focused?.focus({ preventScroll: true })
    }
  }, [isOpen])
  return <>
    <div className="project-gallery">{images.map((item, index) => index === previewIndex && <figure key={item.src}><button type="button" className="gallery-trigger" onClick={() => setSelected(index)} aria-label={`${item.caption} 확대`}><img src={item.src} alt={item.caption} loading="lazy"/><span>크게 보기 ↗</span></button><figcaption>{item.caption}</figcaption></figure>)}</div>
    {isOpen && createPortal(<dialog ref={dialog} className="image-dialog" aria-label="프로젝트 이미지 확대" onCancel={event => { event.preventDefault(); setSelected(null) }} onClick={event => { if (event.target === event.currentTarget) { const rect = event.currentTarget.getBoundingClientRect(); if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) setSelected(null) } }}>
      {selected !== null && <><div className="dialog-controls"><span>{selected + 1} / {images.length}</span><button type="button" autoFocus onClick={() => setSelected(null)}>닫기 ×</button></div><img src={images[selected].src} alt={images[selected].caption}/><p>{images[selected].caption}</p>{images.length > 1 && <div className="dialog-controls"><button type="button" onClick={() => setSelected((selected + images.length - 1) % images.length)}>← 이전 이미지</button><button type="button" onClick={() => setSelected((selected + 1) % images.length)}>다음 이미지 →</button></div>}</>}
    </dialog>, document.body)}
  </>
}

function ProjectDetail() {
  const { id } = useParams()
  const index = projects.findIndex(item => item.id === id)
  const project = projects[index]
  if (!project) return <NotFound/>
  const previous = projects[(index + projects.length - 1) % projects.length]
  const next = projects[(index + 1) % projects.length]
  return <main id="main" className="container detail">
    <ProjectProgress key={project.id}/>
    <Link className="back-link" to="/#work">← 모든 프로젝트</Link>
    <div className="detail-heading"><p className="eyebrow">SELECTED WORK / {project.number}</p><h1>{project.title}</h1><p className="muted">{project.category} · {project.description}</p></div>
    <ProjectVisual project={project}/>
    <div className="detail-body"><aside><p className="eyebrow">PROJECT NOTES</p><dl>
      <div><dt>진행 기간</dt><dd>{project.period}{project.periodStatus && <span className="project-period-status">{project.periodStatus}</span>}</dd></div>
      {project.team && <div><dt>개발 형태</dt><dd>{project.team}</dd></div>}
      {project.responsibility && <div><dt>담당 역할</dt><dd>{project.role}</dd></div>}
      {project.contribution && <div><dt>개인 기여</dt><dd>{project.contribution}</dd></div>}
      <div><dt>담당 범위</dt><dd>{project.responsibility ?? project.role}</dd></div>
      {project.stack.length > 0 && <div><dt>기술 스택</dt><dd><StackTags stack={project.stack}/></dd></div>}
    </dl>{project.note && <p className="source-note">{project.note}</p>}{project.repository?.public && <div className="project-external"><p className="eyebrow">EXTERNAL LINKS</p><GitHubLink href={project.repository.url} label={project.repository.kind === 'case-study' ? 'GitHub 기술 사례 문서' : 'GitHub 저장소'}/></div>}</aside>
    <div className="detail-sections">{project.achievements && <section className="project-achievements" aria-labelledby="achievements-title">
      <p className="eyebrow">KEY RESULTS</p><h2 id="achievements-title">주요 성과</h2>
      <p className="muted">{project.achievements.summary}</p>
      <dl className="achievement-metrics">{project.achievements.metrics.map(metric => <div key={metric.label}><dt>{metric.label}</dt><dd><strong>{metric.value}</strong><p>{metric.description}</p></dd></div>)}</dl>
      {project.achievements.customers && <div className="achievement-customers"><h3>그 외 고객사 계약</h3><ul>{project.achievements.customers.map(customer => <li key={customer}>{customer}</li>)}</ul></div>}
      <p className="source-note">{project.achievements.note}</p>
      {project.achievements.source && <a className="text-link achievement-source" href={project.achievements.source.url} target="_blank" rel="noopener noreferrer">{project.achievements.source.label} <Arrow/></a>}
    </section>}{!!(project.highlights?.length || project.flow?.length) && <section className="project-flow" aria-label={project.flow?.length ? "데이터 흐름" : "핵심 구현"}>{!!project.highlights?.length && <div className={project.flow?.length ? "implementation-summary" : undefined}><p className="eyebrow">IMPLEMENTATION</p><h2>핵심 구현</h2><ul>{project.highlights.map(item => <li key={item}>{item}</li>)}</ul></div>}{!!project.flow?.length && <><p className="eyebrow">DATA FLOW / OVERVIEW</p><h2>주요 데이터 흐름</h2><ol>{project.flow.map(step => <li key={step}>{step}</li>)}</ol>{project.flowNote && <p className="source-note">{project.flowNote}</p>}</>}</section>}{project.sections.map((section, sectionIndex) => <section key={section.title}><p className="eyebrow">{String(sectionIndex + 1).padStart(2, '0')} / PROJECT STORY</p><h2>{section.title}</h2>{section.paragraphs.map(paragraph => <p key={paragraph} className="muted">{paragraph}</p>)}{sectionIndex % 2 === 0 && project.images[sectionIndex / 2] && <ImageGallery key={`${project.id}-${sectionIndex}`} images={project.images} previewIndex={sectionIndex / 2}/>}</section>)}</div></div>
    <nav className="project-pagination" aria-label="프로젝트 이동"><Link to={`/projects/${previous.id}`}><p className="eyebrow"><span className="pagination-arrow" aria-hidden="true">←</span> PREVIOUS PROJECT</p><h2>{previous.title}</h2></Link><Link to={`/projects/${next.id}`}><p className="eyebrow">NEXT PROJECT <span className="pagination-arrow" aria-hidden="true">→</span></p><h2>{next.title}</h2></Link></nav>
    <Link className="text-link detail-return" to="/#work">프로젝트 목록으로 돌아가기 <Arrow/></Link>
  </main>
}

function NotFound() { return <main id="main" className="container not-found"><p className="eyebrow">404</p><h1>페이지를 찾을 수 없습니다.</h1><Link className="text-link" to="/">홈으로 돌아가기 <Arrow/></Link></main> }

export default function App() {
  return <><a href="#main" className="skip-link">본문 바로가기</a><AnalyticsTracking/><ScrollToTop/><ScrollMotion/><BackToTop/><PortfolioChat><Header/>{import.meta.env.DEV && <SiteContentReview/>}<Routes><Route path="/" element={<Home/>}/><Route path="/projects/:id" element={<ProjectDetail/>}/><Route path="*" element={<NotFound/>}/></Routes><footer className="container site-footer"><Link className="brand" to="/">folio<span>.</span></Link><span>PORTFOLIO — FIRST EDITION</span><span>BACKEND / CLOUD / OPERATIONS</span></footer></PortfolioChat></>
}
