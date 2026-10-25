# 공개본 최신화 결과 (2026-10-26)

## 비교 및 반영

모바일 헤더, 섹션 내비게이션, 방문·클릭·상세 조회·다운로드 이벤트, 챗봇 상태·입력 차단·오류 처리와 간소화한 사이트 소개는 공개본에 이미 구현되어 있어 유지했습니다. 운영본의 Hero·Skills·프로젝트 스토리는 실제 경력·성과가 포함되어 있어 공개본의 합성 문구를 유지했습니다.

새로 반영한 기능은 Contact 이메일 표시·mailto·복사·실패 안내, 헤더와 Contact의 공통 Markdown 다운로드 버튼, 다운로드 후 모바일 메뉴 정리, Markdown 연락처, 기기 유형 저장, 기본 비활성화된 IP 토큰과 만료 정리, 합성 연락처를 위한 챗봇 지식·개인정보 검사 예외입니다. 입력·응답에서 다른 이메일과 민감정보를 차단하는 기존 동작은 유지합니다.

`demo@example.test`는 실제 연락처가 아닙니다. UI·Markdown·서버 리소스에서 같은 합성 이메일을 사용합니다. 실제 이메일로 교체하려면 소유자가 별도로 공개 주소를 결정해야 합니다.

## 유지 및 제외

UI Demo 1~6, project-one~project-six 경로, Developer Portfolio 예제, 자체 SVG 3개, V3 합성 지식 11개, 빌드·테스트 전용 CI를 유지했습니다. 기존 V1~V5 마이그레이션은 수정하지 않았습니다. V6는 열·인덱스만 추가하고 UNKNOWN으로 과거 기기를 표시합니다. 운영 지식 갱신 V7은 복사하지 않았습니다.

회사·고객사 상세 자료와 성과, 실제 인물 사진·회사 이미지, 이력서·비공개 문서, 운영 환경변수·DB 인증·배포 설정·workflow·스크립트·기존 운영 Git 이력은 제외했습니다. AWS 배포, 운영 DB 변경, 실제 Gemini 호출 및 운영 저장소 수정은 수행하지 않았습니다.

## README

실제 GitHub PUBLIC 상태에 맞게 과거 Private 유지 문구를 정리했습니다. 기존 기술 스택과 로컬 실행·검증 명령은 유지하고 이메일 공유 구조, 기기 분류, V6와 운영 V7의 차이, IP 토큰 기본 비활성화·보존 범위, 추가 테스트를 설명했습니다.

## 검증

- Frontend `npm run test:all`: 전체 통과.
- Frontend `npm run build:release`: 콘텐츠 검사·TypeScript·Vite·빌드 전후 정적 파일 검사 통과.
- Backend JDK 21 `gradlew.bat test build --no-daemon`: 142개 테스트, 실패·오류·스킵 0, 빌드 성공.
- Markdown Blob 생성·다운로드 시작·공통 버튼 이벤트·연락처·비공개 필드 제외·정리 동작 확인.
- 통계 이벤트 중복 방지·기기 분류·HTTP 저장·만료 정리·마이그레이션 보존 확인.
- 챗봇 상태·입력 차단·쿼터·시간 초과·오류·합성 연락처·민감정보 차단 확인.
- 격리 H2와 모의 HTTP/Provider만 사용. 실제 MySQL, 운영 DB, Gemini와 AWS는 사용하지 않음.

## 보안 및 Git

전체 공개 추적 파일·새 파일·스테이징을 키·토큰·개인키·인증 URL·개인/회사 데이터·운영 경로 패턴으로 검사하고 후보를 수동 검토했습니다. 검출 후보는 빈 설정 예시, 합성 테스트 값, 차단용 마커입니다. 모호한 공개 자료는 포함하지 않았습니다. SVG·합성 데이터·기존 CI는 원본 공개 커밋과 동일합니다. JAR에 운영 프로필이 없고 합성 연락처가 포함됨을 확인했습니다.

origin은 `https://github.com/sooyoung3322/developer-portfolio-public.git`, 브랜치는 main입니다. 기존 공개 루트 커밋을 부모로 하는 일반 커밋만 추가하며 운영 Git 이력을 가져오지 않습니다. force push를 사용하지 않습니다. Push 결과는 GitHub main과 최종 작업 보고에서 확인합니다.

## 남은 확인

실제 브라우저의 시각 배치·클립보드 권한 및 OS 저장 완료는 모의 이벤트 검증 범위 밖입니다. MySQL 실기동과 실제 외부 AI 호출은 수행하지 않았습니다. 실제 연락처 교체·기기별 조회 API 추가·운영 배포는 별도 작업입니다.

## 수정 파일

- `README.md`
- `docs/FILES.md`
- `docs/PUBLIC-EDITION.md`
- `docs/SYNC-REPORT.md`
- `docs/VERIFICATION.md`
- `portfolio-backend/build.gradle.kts`
- `portfolio-backend/src/main/java/com/portfolio/backend/analytics/AnalyticsController.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/analytics/AnalyticsEvent.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/analytics/AnalyticsMetadata.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/analytics/AnalyticsRepository.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/analytics/AnalyticsRetention.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/analytics/AnalyticsService.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/analytics/DeviceType.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/chat/GeminiProvider.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/chat/Knowledge.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/chat/KnowledgeService.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/chat/PrivacyGuard.java`
- `portfolio-backend/src/main/java/com/portfolio/backend/chat/PublicContact.java`
- `portfolio-backend/src/main/resources/db/migration/V6__analytics_metadata.sql`
- `portfolio-backend/src/test/java/com/portfolio/backend/FlywayV5UpgradeTests.java`
- `portfolio-backend/src/test/java/com/portfolio/backend/ProductionProfileTests.java`
- `portfolio-backend/src/test/java/com/portfolio/backend/analytics/AnalyticsIntegrationTests.java`
- `portfolio-backend/src/test/java/com/portfolio/backend/analytics/AnalyticsMetadataTests.java`
- `portfolio-backend/src/test/java/com/portfolio/backend/chat/ChatIntegrationTests.java`
- `portfolio-backend/src/test/java/com/portfolio/backend/chat/PublicContactTests.java`
- `portfolio-frontend/package.json`
- `portfolio-frontend/scripts/contact-check.cjs`
- `portfolio-frontend/scripts/markdown-check.cjs`
- `portfolio-frontend/scripts/ui-check.cjs`
- `portfolio-frontend/src/App.tsx`
- `portfolio-frontend/src/components/ContactEmail.tsx`
- `portfolio-frontend/src/components/MarkdownDownloadButton.tsx`
- `portfolio-frontend/src/data/profile.ts`
- `portfolio-frontend/src/data/public-contact.json`
- `portfolio-frontend/src/portfolioMarkdown.ts`
- `portfolio-frontend/src/styles/global.css`
