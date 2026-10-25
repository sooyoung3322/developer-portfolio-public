import { trackMarkdownDownload } from '../analytics'
import { downloadPortfolioMarkdown } from '../portfolioMarkdown'

export function MarkdownDownloadButton({ className, onDownloaded }: { className: string; onDownloaded?: () => void }) {
  return <button type="button" className={className} onClick={() => {
    downloadPortfolioMarkdown(trackMarkdownDownload)
    onDownloaded?.()
  }}>포트폴리오 .md 다운로드 <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5"/></svg></button>
}
