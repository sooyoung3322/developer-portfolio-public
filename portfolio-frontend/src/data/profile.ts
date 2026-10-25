import publicContact from './public-contact.json'

export const profile = {
  email: publicContact.email,
  name: 'sooyoung3322',
  title: 'Backend Developer',
  introduction: ['Java와 Spring Boot를 중심으로 웹 서비스를 개발합니다.', '이 저장소는 포트폴리오의 UI, API, 테스트를 확인하기 위한 공개용 소스 구성입니다.'],
  github: 'https://github.com/sooyoung3322',
}
export const experiences = [{
  company: '개인 포트폴리오 개발',
  period: '개인 프로젝트',
  duration: '구현 및 검증',
  role: 'React · Spring Boot',
  tasks: ['포트폴리오 UI와 REST API 구현', 'Markdown 다운로드와 통계 이벤트 처리', '모의 외부 API와 격리 DB를 활용한 회귀 테스트'],
}]
export const skills = [
  { eyebrow: 'BACKEND & DATA', title: 'Java / Spring Boot', items: ['Java 21 · Spring Security · Spring Data JPA', 'MySQL · Flyway · H2 테스트'] },
  { eyebrow: 'FRONTEND & WEB', title: 'React / TypeScript', items: ['React 19 · TypeScript · Vite', 'React Router · Tailwind CSS'] },
  { eyebrow: 'AI & AX DEVELOPMENT', title: 'AI 활용과 검증', items: ['ChatGPT · OpenAI Codex를 활용한 구현과 리팩토링', '테스트·빌드 결과로 변경 확인', '실제 외부 API 호출 없이 모의 응답으로 검증'] },
]
