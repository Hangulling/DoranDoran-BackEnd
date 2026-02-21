# Dorandoran Admin Frontend

어드민 전용 프론트엔드 애플리케이션입니다.

## 개요

이 프로젝트는 `dorandoran-frontend`에서 분리된 어드민 전용 프론트엔드입니다. 프롬프트 관리, 채팅 로그 조회, 관리 이력 조회 등의 기능을 제공합니다.

## 시작하기

### 설치

```bash
npm install
```

### 개발 서버 실행

```bash
npm run dev
```

개발 서버는 `http://localhost:3001`에서 실행됩니다.

### 빌드

```bash
npm run build
```

## 환경 변수

`.env` 파일을 생성하고 다음 변수를 설정하세요:

```
VITE_API_BASE_URL=http://localhost:8080
```

## 주요 기능

- 프롬프트 테스트 및 적용
- 프롬프트 버전 관리
- 채팅 로그 조회
- 관리 이력 조회

## 기술 스택

- React 19
- TypeScript
- Vite
- React Router
- Tailwind CSS
- DaisyUI

## 라우팅

- `/login` - 로그인 페이지
- `/` - 대시보드
- `/prompts/test-and-apply` - 프롬프트 테스트 및 적용
- `/prompts/versions` - 프롬프트 버전 관리
- `/chat-logs/user-history` - 사용자 채팅 내역
- `/chat-logs/management-needed` - 관리 필요 내역
- `/history` - 관리 이력 조회
