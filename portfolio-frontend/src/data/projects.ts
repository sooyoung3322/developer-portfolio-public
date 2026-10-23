export interface ProjectImage { src: string; caption: string }
export interface ProjectSection { title: string; paragraphs: string[] }
export interface ProjectRepository { url: string; public: boolean; kind: 'source' | 'case-study' }
export interface ProjectAchievements {
  metrics: { value: string; label: string; description: string }[];
  summary: string; note?: string; customers?: string[]; source?: { url: string; label: string };
}
export interface Project {
  id: string; number: string; title: string; wordmark: string; tone: 'blue' | 'ivory';
  category: string; period: string; team: string; role: string; stack: string[];
  description: string; sections: ProjectSection[]; images: ProjectImage[]; note?: string;
  responsibility?: string; contribution?: string; periodStatus?: string;
  repository?: ProjectRepository; flow?: string[]; flowNote?: string;
  cardRole: string; highlights?: string[]; achievements?: ProjectAchievements;
}
export const projectRepositories = {
  portfolio: { url: 'https://github.com/sooyoung3322/developer-portfolio-public', public: false, kind: 'source' },
} satisfies Record<string, ProjectRepository>

// Synthetic UI fixtures, not descriptions or reconstructions of company projects.
const demos: Project[] = ['one', 'two', 'three', 'four', 'five', 'six'].map((number, index) => ({
  id: 'project-' + number, number: String(index + 1).padStart(2, '0'),
  title: 'UI Demo ' + (index + 1), wordmark: 'DEMO ' + (index + 1),
  tone: index % 2 ? 'ivory' : 'blue', category: '공개본 UI 예제',
  period: '합성 데이터', team: '공개본 예제', role: '실제 회사 자료 미포함',
  cardRole: '화면·라우팅 검증용 데이터', stack: ['UI 데모'],
  description: '회사 원자료를 제외하고 카드·상세·이미지 확대 동작을 보여주는 합성 예제입니다.',
  sections: [
    { title: '공개본 자료 범위', paragraphs: ['이 카드는 실제 회사 프로젝트의 설명이나 성과를 재현하지 않습니다. 원본 이미지와 회사 관련 상세 자료는 포함하지 않았습니다.'] },
    { title: 'UI 확인', paragraphs: ['프로젝트 상세 이동, 이미지 확대, 이전·다음 탐색을 확인할 수 있습니다.'] },
    { title: '합성 안내 이미지', paragraphs: ['두 번째 SVG도 실제 화면 캡처가 아닌 공개본 안내용 도형과 텍스트입니다.'] },
  ],
  images: [
    { src: '/images/project-placeholder.svg', caption: '공개본 안내 이미지' },
    { src: '/images/project-placeholder-detail.svg', caption: '합성 UI 안내 이미지' },
  ],
}))
export const projects: Project[] = [...demos, {
  id: 'developer-portfolio', number: '07', title: 'Developer Portfolio', wordmark: 'folio.', tone: 'ivory',
  category: '개인 개발자 포트폴리오', period: '개인 프로젝트', team: '개인 개발',
  role: '프론트엔드·API·테스트 구현', cardRole: 'React · Spring Boot 구현',
  stack: ['Java 21', 'Spring Boot', 'React 19', 'TypeScript', 'JPA', 'Flyway'],
  description: '프로젝트 UI, 통계 API, AI 질문 UI와 Markdown 다운로드를 구현한 개인 프로젝트입니다.',
  repository: projectRepositories.portfolio,
  highlights: ['Markdown 생성·다운로드 및 이벤트 기록', '방문·클릭·상세 조회 통계', 'AI 상태 안내·전송 차단·선택적 저장 동의'],
  flow: ['React 사용자 입력', 'Spring Boot 검증·요청 제한', '공개 지식과 Provider 인터페이스', '응답 검증과 UI 표시'],
  flowNote: '실제 API 호출은 기본 비활성화이며 테스트는 모의 Provider를 사용합니다.',
  sections: [
    { title: 'UI와 API', paragraphs: ['React Router로 프로젝트 상세 화면을 구성하고 Spring Boot로 통계와 AI 질문 API를 제공합니다.'] },
    { title: 'AI 활용과 검증', paragraphs: ['ChatGPT와 OpenAI Codex로 작업을 나누어 구현·리팩토링하고 기존 회귀 테스트와 빌드를 실행해 변경을 확인했습니다.'] },
  ], images: [{ src: '/images/project-placeholder.svg', caption: '소스 공개본 안내 이미지' }],
}]
