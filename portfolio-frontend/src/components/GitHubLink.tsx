export function GitHubLink({ href, label, iconOnly = false }: { href: string; label: string; iconOnly?: boolean }) {
  return <a className={`github-link${iconOnly ? ' github-icon-link' : ''}`} href={href} target="_blank" rel="noopener noreferrer" aria-label={`${label} (새 탭에서 열림)`} title={label}>
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M9 19c-4.3 1.3-4.3-2.5-6-3m12 6v-3.9a3.4 3.4 0 0 0-.9-2.7c3-.3 6.2-1.5 6.2-7a5.4 5.4 0 0 0-1.5-3.7 5 5 0 0 0-.1-3.7s-1.2-.4-3.9 1.4a13.4 13.4 0 0 0-7 0C5.1.6 3.9 1 3.9 1a5 5 0 0 0-.1 3.7A5.4 5.4 0 0 0 2.3 8.4c0 5.5 3.2 6.7 6.2 7A3.4 3.4 0 0 0 7.6 18V22"/></svg>
    {!iconOnly && <span>{label} ↗</span>}
  </a>
}
