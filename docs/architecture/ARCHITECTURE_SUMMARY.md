# DoranDoran 프로젝트 아키텍처 핵심 요약

**작성 일시**: 2025년 1월  
**프로젝트**: DoranDoran - AI 기반 한국어 학습 챗봇 플랫폼

---

## 📋 목차

1. [백엔드 도메인 설계](#1-백엔드-도메인-설계)
2. [인프라 & 배포 구조](#2-인프라--배포-구조)
3. [챗봇 구조](#3-챗봇-구조)
4. [모니터링 구조](#4-모니터링-구조)

---

## 1. 백엔드 도메인 설계

### 1.1 핵심 설계 원칙

#### **스키마 분리 전략 (Schema-per-Service)**
- **Shared Database 패턴**을 채택하되, **스키마로 완전 분리**하여 서비스 독립성 보장
- PostgreSQL 17 단일 인스턴스에 5개 스키마 분리:
  - `user_schema`: 사용자 정보 및 프로필
  - `auth_schema`: 인증/인가 관련 데이터
  - `chat_schema`: 채팅방, 메시지, 챗봇 메타데이터
  - `store_schema`: 표현 보관함 데이터
  - `billing`: AI 사용량 및 비용 추적

#### **도메인별 엔티티 구조**

**User Domain (user_schema)**
- `app_user`: 사용자 핵심 정보 (UUID, 이메일, OAuth 정보)
- `profiles`: 사용자 프로필 (바이오, 아바타, 설정 JSONB)
- `settings`: 사용자별 설정 (key-value 형태)

**Auth Domain (auth_schema)**
- `refresh_tokens`: JWT 리프레시 토큰 관리 (토큰 회전 지원)
- `login_attempts`: 로그인 시도 기록 (보안 감사)
- `email_verifications`: 이메일 인증 토큰
- `password_reset_tokens`: 비밀번호 재설정 토큰
- `token_blacklist`: 블랙리스트된 토큰 관리
- `auth_events`: 인증 이벤트 로그 (JSONB 메타데이터)

**Chat Domain (chat_schema)**
- `chatbots`: 챗봇 메타데이터 (인격, 프롬프트, 설정 JSONB)
- `chatrooms`: 채팅방 정보 (컨텍스트 데이터 JSONB)
- `messages`: 메시지 내용 (시퀀스 번호, 부모 메시지 참조)
- `intimacy_progress`: 친밀도 진척도 추적 (Level 1-3)
- `user_chatbot_last_interaction`: 사용자-챗봇 마지막 상호작용

**Store Domain (store_schema)**
- `stores`: 표현 보관함 (원문, 교정문, AI 응답 JSONB)

**Billing Domain**
- `ai_usage_events`: AI 사용 이벤트 (토큰 수, 비용)
- `monthly_user_costs`: 월별 사용자 비용 집계

### 1.2 강조 사항

#### **JSONB 활용**
- **유연한 스키마 확장**: `personality`, `settings`, `context_data`, `metadata` 등 JSONB 필드로 유연한 데이터 구조
- **성능 최적화**: PostgreSQL JSONB 인덱싱 및 쿼리 최적화 활용

#### **UUID 기반 식별자**
- 모든 주요 엔티티는 UUID를 PK로 사용하여 분산 환경에서의 고유성 보장
- 시퀀스 번호는 메시지 순서 추적용으로만 사용

#### **소프트 삭제 (Soft Delete)**
- `is_deleted`, `deleted_at` 필드로 데이터 복구 가능
- `is_archived`로 아카이브 기능 지원

#### **친밀도 레벨 시스템**
- Level 1: 격식체 (존댓말)
- Level 2: 부드러운 존댓말
- Level 3: 반말
- `intimacy_progress` 테이블로 사용자별 진척도 추적

---

## 2. 인프라 & 배포 구조

### 2.1 핵심 인프라 구성

#### **AWS EC2 기반 배포**
- **인스턴스 타입**: t3.medium (2 vCPU, 4GB RAM)
- **서버 IP**: 3.21.177.186
- **운영체제**: Amazon Linux 2
- **보안 그룹**: 포트별 세밀한 접근 제어 (22, 80, 443, 8080-8085)

#### **Docker 컨테이너화**
- **모든 서비스 컨테이너화**: 독립적인 Dockerfile 및 docker-compose 구성
- **Docker Network**: `dorandoran-msa-network`로 서비스 간 통신
- **컨테이너 네이밍**: `dorandoran-{service-name}` 규칙

#### **리버스 프록시 (Nginx)**
- **SSL/TLS**: Let's Encrypt 자동 인증서 관리
- **도메인 라우팅**:
  - `api.doran-chat.com` → API Gateway (8080)
  - `chat.doran-chat.com` → Chat Service (8083)
  - `auth.doran-chat.com` → Auth Service (8081)
- **SSE/WebSocket 지원**: 긴 타임아웃 설정 (600초), 버퍼링 비활성화

### 2.2 배포 아키텍처

#### **서비스 포트 구성**
- API Gateway: 8080
- Auth Service: 8081
- User Service: 8082
- Chat Service: 8083
- Store Service: 8084
- Batch Service: 8085

#### **데이터 계층**
- **PostgreSQL 17**: Shared Database, 5개 스키마 분리
- **Redis 7**: 캐싱, 세션 관리, Rate Limiting

#### **배포 자동화**
- PowerShell/Shell 배포 스크립트로 서비스별 개별 배포 지원
- Health Check 자동화 (`/actuator/health` 엔드포인트)
- Docker Compose 기반 일괄 배포

### 2.3 강조 사항

#### **단일 EC2 인스턴스 전략**
- 비용 최적화를 위해 단일 인스턴스에 모든 서비스 배포
- Docker 컨테이너로 서비스 격리 및 독립성 보장
- 향후 확장 시 ECS/EKS로 마이그레이션 가능한 구조

#### **Shared Database 패턴**
- 스키마 분리로 서비스 독립성 유지
- 트랜잭션 일관성 보장 (동일 데이터베이스)
- 향후 Database-per-Service로 분리 가능한 구조

#### **보안 계층화**
- AWS Security Group: 네트워크 레벨 보안
- Nginx: 리버스 프록시 및 SSL/TLS 종료
- API Gateway: Rate Limiting, IP 블랙리스트 (29개 IP 차단)
- JWT 인증: 서비스 레벨 인증/인가

---

## 3. 챗봇 구조

### 3.1 Multi-Agent AI 아키텍처

#### **핵심 설계 원칙**
- **병렬 처리 최적화**: Phase 1에서 3개 Agent 동시 실행
- **실시간 피드백**: SSE 스트리밍으로 즉각적인 응답 제공
- **친밀도 기반 맞춤화**: Level 1-3 단계별 대화 스타일 조정

#### **Agent 구성**

**Phase 1: 병렬 처리 (동시 실행)**
1. **IntimacyAgent**: 친밀도 분석 및 교정
   - 사용자 입력의 친밀도 레벨 판단 (1-3)
   - 교정 피드백 제공
   - `intimacy_progress` 테이블 업데이트

2. **VocabularyAgent**: 어휘 추출 및 난이도 분석
   - 어려운 단어 최대 1개 추출
   - 난이도 분석
   - VocabularyAgentResponse 반환

3. **ConversationAgent**: 대화 생성 (SSE 스트리밍)
   - 자연스러운 대화 응답 생성
   - 실시간 스트리밍 (`conversation_chunk` 이벤트)
   - 친밀도 레벨에 맞는 톤 조정

**Phase 2: 순차 처리**
4. **TranslationAgent**: 번역 처리
   - VocabularyAgent 결과를 받아 번역
   - 영어 번역 및 발음기호 제공
   - `vocabulary_translated` 이벤트 전송

#### **SSE 이벤트 타입**
- `intimacy_analysis`: 친밀도 분석 결과
- `vocabulary_extracted`: 어휘 추출 결과
- `vocabulary_translated`: 번역 결과
- `conversation_chunk`: 대화 응답 스트림 (실시간)
- `conversation_complete`: 대화 완료
- `aggregated_complete`: 전체 결과 집계

### 3.2 데이터 흐름

#### **메시지 처리 파이프라인**
```
사용자 입력
  ↓
ChatController.sendMessage()
  ↓
ChatService.sendMessage() → DB 저장
  ↓
MultiAgentOrchestrator.processUserMessage()
  ↓
[병렬 실행]
├── IntimacyAgent → OpenAI API
├── VocabularyAgent → OpenAI API
└── ConversationAgent → OpenAI Streaming API
  ↓
[순차 실행]
VocabularyAgent 결과 → TranslationAgent → OpenAI API
  ↓
SSEManager를 통한 실시간 이벤트 전송
  ↓
프론트엔드 EventSource 수신
```

### 3.3 강조 사항

#### **성능 최적화**
- **병렬 처리**: 3개 Agent 동시 실행으로 응답 시간 단축
- **스트리밍**: ConversationAgent는 실시간 스트리밍으로 사용자 경험 향상
- **캐싱**: Redis를 통한 프롬프트 및 컨텍스트 캐싱

#### **확장 가능한 구조**
- 새로운 Agent 추가 용이 (Orchestrator 패턴)
- Agent별 독립적인 프롬프트 관리 (PromptService)
- 다양한 AI 모델 지원 (OpenAI, 향후 Claude 등)

#### **컨텍스트 관리**
- `chatrooms.context_data` JSONB로 대화 컨텍스트 저장
- `messages` 테이블의 `parent_message_id`로 대화 트리 구조
- `sequence_number`로 메시지 순서 보장

---

## 4. 모니터링 구조

### 4.1 모니터링 스택 구성

#### **메트릭 수집 (Metrics)**
- **Spring Actuator**: 각 서비스의 `/actuator/prometheus` 엔드포인트
- **Micrometer**: JVM, HTTP, 데이터베이스 메트릭 자동 수집
- **Prometheus**: 메트릭 저장소 (TSDB, 30일 보관)
- **Exporters**: 
  - Postgres Exporter (Port: 9187): DB 연결 수, 쿼리 성능
  - Redis Exporter (Port: 9121): 메모리, 명령어 통계

#### **로그 수집 (Logs)**
- **Logback**: JSON 로그 포맷으로 구조화된 로깅
- **Promtail**: Docker 컨테이너 로그 수집 에이전트
- **Loki**: 로그 저장소 (Port: 3100), 인덱싱 및 쿼리 지원

#### **시각화 (Visualization)**
- **Grafana**: 통합 대시보드 (Port: 3000)
  - 서비스별 대시보드
  - JVM 메트릭 (메모리, GC, 스레드)
  - HTTP 메트릭 (요청 수, 지연시간, 에러율)
  - 데이터베이스 메트릭 (연결 수, 쿼리 성능)

#### **알림 (Alerting)**
- **AlertManager**: 알림 관리 (Port: 9093)
  - 중복 제거
  - 그룹화
  - 라우팅 규칙
- **Prometheus Alert Rules**: 임계값 기반 알림
  - 서비스 다운 감지
  - 높은 에러율 감지
  - 높은 지연시간 감지
- **알림 채널**: Slack, Email (향후 PagerDuty 지원)

### 4.2 모니터링 메트릭

#### **핵심 메트릭**
- **서비스 헬스**: `/actuator/health` 엔드포인트 상태
- **HTTP 메트릭**: 요청 수, 지연시간 (p50, p95, p99), 에러율
- **JVM 메트릭**: 힙 메모리, GC 시간, 스레드 수
- **데이터베이스 메트릭**: 연결 수, 쿼리 성능, 트랜잭션 수
- **Redis 메트릭**: 메모리 사용량, 명령어 통계, 키 수

#### **로그 구조**
- **JSON 포맷**: 구조화된 로그로 파싱 및 검색 용이
- **서비스 식별**: 각 로그에 서비스 이름 포함
- **컨텍스트 정보**: MDC (Mapped Diagnostic Context) 활용

### 4.3 강조 사항

#### **완전한 관찰 가능성 (Observability)**
- **메트릭**: 시스템 성능 및 상태 모니터링
- **로그**: 상세한 이벤트 추적 및 디버깅
- **트레이싱**: 향후 분산 트레이싱 추가 가능 (Jaeger, Zipkin)

#### **프로액티브 모니터링**
- **알림 규칙**: 문제 발생 전 조기 경고
- **대시보드**: 실시간 시스템 상태 시각화
- **트렌드 분석**: 장기적인 성능 트렌드 추적

#### **비용 효율적 구성**
- 단일 EC2 인스턴스에 모든 모니터링 스택 배포
- Docker 컨테이너로 리소스 격리
- 30일 메트릭 보관으로 적절한 데이터 보존

---

## 📊 전체 아키텍처 요약

### 핵심 특징

1. **마이크로서비스 아키텍처**: 5개 독립 서비스 (Auth, User, Chat, Store, Batch)
2. **Multi-Agent AI**: 4개 AI Agent를 통한 지능형 대화 처리
3. **실시간 통신**: SSE 스트리밍으로 즉각적인 피드백 제공
4. **완전한 모니터링**: Prometheus + Grafana + Loki로 관찰 가능성 보장
5. **컨테이너화**: Docker 기반 배포로 확장성 및 이식성 확보

### 기술 스택

- **백엔드**: Spring Boot 3.3.4, Java 21, PostgreSQL 17, Redis 7
- **AI**: OpenAI API (GPT-4, GPT-3.5)
- **인프라**: AWS EC2, Docker, Nginx
- **모니터링**: Prometheus, Grafana, Loki, AlertManager

### 확장 계획

- **수평 확장**: ECS/EKS로 마이그레이션하여 서비스별 독립 스케일링
- **Database-per-Service**: 향후 서비스별 독립 데이터베이스 분리
- **추가 AI 모델**: Claude, Gemini 등 다양한 AI 모델 지원
- **분산 트레이싱**: Jaeger 또는 Zipkin 도입

---

**문서 버전**: 1.0  
**최종 업데이트**: 2025년 1월


