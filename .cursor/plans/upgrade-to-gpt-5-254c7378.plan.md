<!-- 254c7378-1b64-4404-8ef3-64fa0cc20b4e 9e537c51-6b91-4f3a-a008-77ac10960795 -->
# GPT-5 모델 업그레이드

## 변경할 파일

### 1. Chat 서비스 설정

- `chat/src/main/resources/application.yml` (line 84)
- 변경: `model: ${OPENAI_MODEL:gpt-4o-mini}` → `model: ${OPENAI_MODEL:gpt-5}`
- 가격 정보 업데이트 (line 87-88):
  - `price-per1k-input: 1.25`
  - `price-per1k-output: 10.00`

- `chat/src/main/resources/application-docker.yml` (line 72)
- 변경: `model: gpt-4o-mini` → `model: gpt-5`

### 2. 개발 환경 설정

- `bin/main/application.properties` (line 48)
- 변경: `openai.model=gpt-4-turbo-preview` → `openai.model=gpt-5`

## 가격 정보 (참고)

GPT-5 모델 비용 (1M 토큰당):

- 입력: $1.25
- 출력: $10.00

## 영향 범위

- Chat 서비스의 모든 AI 응답이 GPT-5를 사용
- 비용이 기존 대비 증가 (gpt-4o-mini 대비 약 5-16배)
- 성능 및 응답 품질 향상 예상