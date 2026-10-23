# Developer Portfolio — Source Edition

React와 Spring Boot로 구현한 개인 포트폴리오의 소스 확인용 저장소입니다. 채용 담당자와 개발 면접관이 UI, API, 데이터 처리 및 테스트 코드를 로컬에서 확인할 수 있도록 구성했습니다.

운영용 저장소와 이력·인증·배포를 분리했습니다. 회사 이미지와 상세 자료는 포함하지 않습니다. **UI Demo 1~6은 합성 데이터**이며 실제 프로젝트나 경력·성과를 재현하지 않습니다. 운영 사이트와 이 공개용 소스의 콘텐츠는 다릅니다. 이 저장소는 현재 Private로 유지하며 Public 전환은 별도 결정입니다.

운영 사이트: [sooyoung.xyz](https://sooyoung.xyz) — 운영 사이트의 현재 가용성·TLS 상태를 이 저장소의 CI가 검증하지는 않습니다.

## 개발 목적과 주요 기능

- 반응형 헤더·모바일 메뉴, 스크롤에 따른 활성 메뉴, 상세 라우팅
- 프로젝트 이미지 확대 dialog, 이전·다음 프로젝트 탐색
- 공유 데이터로 생성하는 Markdown 다운로드
- 방문·프로젝트 클릭·상세 조회·Markdown 다운로드 이벤트 수집
- 이벤트 ID 중복 방지, 통계 실패와 다운로드/탐색 분리
- AI 질문 UI, 추천 질문, 상태 확인, 비활성화 시 UI와 전송 함수 차단
- 선택적 대화 저장 동의, 입력 검증·개인정보 패턴 검사·요청 제한
- 테스트 Provider와 H2로 외부 서비스·운영 DB 없이 검증

## 기술 스택

| 영역 | 구성 |
| --- | --- |
| Frontend | React 19, TypeScript 5.7, Vite 6, React Router 7, Tailwind CSS 4 |
| Backend | Java 21, Spring Boot 4.1.1, Spring Security, Spring Data JPA |
| 데이터 | MySQL 드라이버, Flyway, 테스트용 H2 |
| 검증 | JUnit 5, Mockito, Node 기반 렌더링·이벤트 모의 테스트 |

## 구조

```text
portfolio-frontend/
  src/components/       UI·챗봇·네비게이션
  src/data/             합성 UI 예제·공개본 프로필
  src/portfolioMarkdown.ts
  scripts/              콘텐츠·개인파일 차단·회귀 테스트
  public/images/        자체 작성 SVG 안내 이미지
  config/               비공개 파일 차단 정책
portfolio-backend/
  src/main/java/        analytics·chat·config·health
  src/main/resources/   공통 설정·로컬 예시·Flyway
  src/test/             합성 데이터·H2·모의 외부 API
.github/workflows/ci.yml  빌드·테스트만 실행
docs/PUBLIC-EDITION.md    원본에서 제외한 자료의 범위
```

## 로컬 실행

JDK 21, Node.js 22, npm이 필요합니다. 아래 명령은 저장소 루트 기준이며 PowerShell에서는 Gradle 실행 파일을 `gradlew.bat`로 사용합니다.

```powershell
cd portfolio-backend
./gradlew.bat bootRun
```

기본 프로필은 DB 자동 구성을 제외하며 `http://localhost:8080/api/health`를 제공합니다. 기본 상태에서 AI는 비활성화되고 통계 저장은 제공하지 않습니다.

별도 터미널:

```powershell
cd portfolio-frontend
npm ci
npm run dev
```

프론트엔드 기본 주소는 `http://localhost:5173`입니다. 개발 시 API 기본 주소는 `http://localhost:8080`이며 `.env.example`의 `VITE_API_BASE_URL`로 바꿀 수 있습니다. `VITE_` 변수는 공개되므로 비밀 값을 넣지 않습니다.

### 선택: 로컬 MySQL

운영 DB가 아닌 폐기 가능한 개발 DB만 사용합니다. `application-local.example.yml`을 `application-local.yml`로 복사한 뒤 아래 환경변수를 현재 터미널에 설정하고 `bootRun --args='--spring.profiles.active=local'`로 실행합니다.

| 변수 | 의미 |
| --- | --- |
| DB_URL | 로컬 개발 DB JDBC URL |
| DB_USERNAME / DB_PASSWORD | 개발 DB 계정. 저장소에 저장하지 않음 |
| AI_CHAT_ENABLED | 기본 false |
| GEMINI_FREE_TIER_CONFIRMED | 기본 false |
| GEMINI_API_KEY | 기본 빈 값. 서버 측에서만 설정 |
| GEMINI_MODEL / GEMINI_FREE_TIER_MODEL | 명시적으로 검토한 모델 |
| AI_DAILY_LIMIT / GEMINI_FREE_RPD / GEMINI_FREE_RPM / GEMINI_FREE_TPM | 애플리케이션 한도 설정. 실제 제공자 무료량 보장 아님 |

Spring Boot는 `.env`를 자동 로드하지 않습니다. DB 기동 시 Flyway가 스키마와 **합성 예제**를 적용합니다. 이 공개본의 V3는 운영 데이터가 아니므로 운영 DB와 마이그레이션 이력을 공유하면 안 됩니다. 운영 전용 프로필은 포함하지 않으며, 같은 이름의 YAML이 테스트 리소스에만 존재하는 것은 보안 회귀 테스트용입니다.

실제 Gemini 연동은 키·모델·쿼터 조건을 별도로 확인한 경우에만 사용합니다. 본 저장소의 테스트는 실제 Gemini를 호출하지 않습니다.

## 테스트와 빌드

```powershell
cd portfolio-frontend
npm ci
npm run test:all
npm run build:release
cd ../portfolio-backend
./gradlew.bat test build --no-daemon
```

프론트 테스트는 실제 컴포넌트 렌더링과 DOM/이벤트 모의 검증입니다. 실제 브라우저의 레이아웃·포커스·파일 저장 완료 검증을 대신하지 않습니다. 백엔드 기본 테스트는 격리 H2와 모의 HTTP/Provider를 사용합니다.

빌드는 public 복사 전과 dist 생성 후 비공개 파일을 검사합니다. 원본 이력서·키·환경 파일·DB 덤프·압축파일은 공개 정적 경로에 넣지 않습니다. Markdown은 DOCX를 읽지 않고 명시된 데이터 필드만 사용합니다.

## 배포 구조의 개요

정적 프론트엔드와 Spring Boot API를 구분하고, 운영 환경에서는 HTTPS 리버스 프록시가 필요한 API로 연결하는 구조입니다. 운영용 인증·서버 설정·배포 스크립트는 이 저장소에 없습니다. CI는 빌드·테스트만 실행하며 AWS 배포, GitHub App 인증, 운영 시크릿, 아티팩트 업로드 단계를 포함하지 않습니다.

## AI 활용 및 검증 경험

ChatGPT와 OpenAI Codex로 요구사항을 정리하고 구현·리팩토링 작업을 작은 단위로 진행했습니다. 생성된 코드를 그대로 신뢰하지 않고 기존 회귀 테스트, 타입 검사, 빌드와 예외·비활성 상태 검증으로 확인했습니다. 공개본 구성 과정에서도 회사 원자료를 제거하고, 합성 데이터로 실제 코드의 동작을 검증합니다.

자세한 제외 범위는 [PUBLIC-EDITION.md](docs/PUBLIC-EDITION.md)를 참고하세요. 이 저장소에 회사 자료의 사용 권한이나 별도 재사용 라이선스를 부여하는 문서는 포함하지 않습니다. Public 전환 전 소유자가 공개 범위와 라이선스 의사를 확인해야 합니다.
