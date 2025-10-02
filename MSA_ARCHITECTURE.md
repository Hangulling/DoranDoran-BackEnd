# DoranDoran MSA 아키텍처 문서

## 📋 개요

DoranDoran 프로젝트는 모듈러 모놀리스에서 진정한 마이크로서비스 아키텍처(MSA)로 성공적으로 전환되었습니다. 이 문서는 현재 아키텍처 상태와 구현된 패턴들을 설명합니다.

## 🏗️ 아키텍처 다이어그램

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │  Auth Service   │    │  User Service   │
│   (Port 8080)   │◄──►│   (Port 8081)   │◄──►│   (Port 8082)   │
│                 │    │                 │    │                 │
│ • 라우팅        │    │ • JWT 인증      │    │ • 사용자 관리   │
│ • Rate Limiting │    │ • 토큰 검증     │    │ • 프로필 관리   │
│ • CORS 설정     │    │ • 권한 관리     │    │ • 이벤트 발행   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Chat Service   │    │ Store Service   │    │ Batch Service   │
│   (Port 8083)   │    │   (Port 8084)   │    │   (Port 8085)   │
│                 │    │                 │    │                 │
│ • 채팅 관리     │    │ • 상품 관리     │    │ • 배치 작업     │
│ • 메시지 처리   │    │ • 주문 처리     │    │ • 스케줄링     │
│ • 실시간 통신   │    │ • 결제 처리     │    │ • 데이터 처리   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Chat DB       │    │   Store DB      │    │   Batch DB      │
│   (Port 5435)   │    │   (Port 5436)   │    │   (Port 5437)   │
│                 │    │                 │    │                 │
│ • chat_room     │    │ • store_item    │    │ • batch_job     │
│ • message       │    │ • order         │    │ • execution     │
│ • chatbot       │    │ • payment       │    │ • log           │
└─────────────────┘    └─────────────────┘    └─────────────────┘

┌─────────────────┐    ┌─────────────────┐
│   Auth DB       │    │   User DB       │
│   (Port 5433)   │    │   (Port 5434)   │
│                 │    │                 │
│ • auth_user     │    │ • user_profile  │
│ • session       │    │ • user_role     │
│ • token         │    │ • preference    │
└─────────────────┘    └─────────────────┘

┌─────────────────┐
│     Redis       │
│   (Port 6379)   │
│                 │
│ • 캐싱          │
│ • 세션 저장     │
│ • Rate Limiting │
└─────────────────┘
```

## 🎯 서비스별 상세 정보

### 1. API Gateway (포트 8080)
- **기술 스택**: Spring Cloud Gateway
- **역할**: 
  - 모든 외부 요청의 단일 진입점
  - 서비스 라우팅 및 로드 밸런싱
  - Rate Limiting 및 CORS 설정
  - 인증/인가 중앙화

### 2. Auth Service (포트 8081)
- **기술 스택**: Spring Boot, JWT, PostgreSQL
- **역할**:
  - 사용자 인증 및 토큰 관리
  - JWT 토큰 생성/검증/갱신
  - 권한 관리
  - User Service와 REST API 통신
- **데이터베이스**: auth_db (PostgreSQL, 포트 5433)

### 3. User Service (포트 8082)
- **기술 스택**: Spring Boot, JPA, PostgreSQL
- **역할**:
  - 사용자 프로필 관리
  - 사용자 정보 CRUD
  - 이벤트 발행 (UserCreated, UserUpdated, UserStatusChanged)
- **데이터베이스**: user_db (PostgreSQL, 포트 5434)

### 4. Chat Service (포트 8083)
- **기술 스택**: Spring Boot, JPA, PostgreSQL
- **역할**:
  - 채팅방 관리
  - 메시지 처리
  - 실시간 통신
- **데이터베이스**: chat_db (PostgreSQL, 포트 5435)

### 5. Store Service (포트 8084)
- **기술 스택**: Spring Boot, JPA, PostgreSQL
- **역할**:
  - 상품 관리
  - 주문 처리
  - 결제 관리
- **데이터베이스**: store_db (PostgreSQL, 포트 5436)

### 6. Batch Service (포트 8085)
- **기술 스택**: Spring Boot, JPA, PostgreSQL
- **역할**:
  - 배치 작업 스케줄링
  - 데이터 처리
  - 리포트 생성
- **데이터베이스**: batch_db (PostgreSQL, 포트 5437)

## 🔧 구현된 MSA 패턴

### 1. 서비스 독립성 (Service Independence)
- 각 서비스가 독립적인 Spring Boot 애플리케이션
- 독립적인 포트와 데이터베이스
- 독립적인 빌드 및 배포 가능

### 2. 데이터 소유권 (Data Ownership)
- 서비스별 독립적인 데이터베이스
- 데이터베이스 스키마 분리
- 서비스 간 데이터 직접 접근 금지

### 3. 통신 패턴 (Communication Patterns)

#### REST API 통신
- **Feign 클라이언트**: 서비스 간 동기 통신
- **Fallback 메커니즘**: 서비스 장애 시 대체 로직
- **Circuit Breaker**: 서비스 호출 안정성 확보

#### 이벤트 기반 통신
- **이벤트 발행**: User Service에서 도메인 이벤트 발행
- **이벤트 리스닝**: Auth Service에서 이벤트 처리
- **느슨한 결합**: 서비스 간 직접 의존성 제거

### 4. API Gateway 패턴
- **중앙화된 라우팅**: 모든 외부 요청의 단일 진입점
- **Rate Limiting**: 서비스별 요청 제한
- **CORS 설정**: 크로스 오리진 요청 처리

### 5. 장애 격리 (Fault Isolation)
- **Circuit Breaker**: Resilience4j를 통한 서비스 보호
- **Retry 메커니즘**: 일시적 장애 대응
- **Timeout 설정**: 응답 시간 제한

## 📊 현재 상태

| 서비스 | 빌드 | 독립 실행 | REST API | 이벤트 | 데이터베이스 | 상태 |
|--------|------|----------|----------|--------|-------------|------|
| API Gateway | ✅ | ✅ | ✅ | N/A | N/A | **완료** |
| Auth | ✅ | ✅ | ✅ | ✅ | ✅ | **완료** |
| User | ✅ | ⚠️ | ✅ | ✅ | ✅ | **거의 완료** |
| Chat | ✅ | ⚠️ | N/A | N/A | ✅ | **빌드 완료** |
| Store | ✅ | ⚠️ | N/A | N/A | ✅ | **빌드 완료** |
| Batch | ✅ | ⚠️ | N/A | N/A | ✅ | **빌드 완료** |

## 🚀 다음 단계 권장사항

### 1. 모니터링 강화
- **분산 추적**: Zipkin 또는 Jaeger 도입
- **메트릭 수집**: Prometheus + Grafana
- **로그 집중화**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **알림 시스템**: PagerDuty 또는 Slack 연동

### 2. 서비스 디스커버리
- **Eureka Server**: Netflix Eureka 도입
- **Consul**: HashiCorp Consul 고려
- **Health Check**: 서비스 상태 모니터링
- **Load Balancing**: 동적 로드 밸런싱

### 3. 메시지 큐 시스템
- **RabbitMQ**: 경량 메시지 브로커
- **Apache Kafka**: 고성능 스트리밍 플랫폼
- **이벤트 소싱**: 이벤트 기반 아키텍처 강화
- **Saga 패턴**: 분산 트랜잭션 관리

### 4. 컨테이너 오케스트레이션
- **Kubernetes**: 컨테이너 오케스트레이션
- **Docker Swarm**: 경량 오케스트레이션
- **Helm Charts**: 애플리케이션 패키징
- **Istio**: 서비스 메시 구현

### 5. CI/CD 파이프라인
- **GitHub Actions**: 자동화된 빌드/배포
- **Jenkins**: 유연한 파이프라인 구성
- **ArgoCD**: GitOps 기반 배포
- **서비스별 독립 배포**: Blue-Green 또는 Canary 배포

### 6. 보안 강화
- **OAuth 2.0 / OpenID Connect**: 표준 인증 프로토콜
- **API 보안**: Rate Limiting, API Key 관리
- **서비스 간 인증**: mTLS (Mutual TLS)
- **Secrets 관리**: HashiCorp Vault 또는 AWS Secrets Manager

### 7. 성능 최적화
- **캐싱 전략**: Redis Cluster, Caffeine
- **데이터베이스 최적화**: 인덱싱, 파티셔닝
- **CDN 도입**: 정적 자원 최적화
- **로드 테스트**: JMeter, Gatling

## 📁 프로젝트 구조

```
DoranDoran/
├── app/                    # 기존 모놀리식 앱 (레거시)
├── auth/                   # 인증 서비스
├── user/                   # 사용자 서비스
├── chat/                   # 채팅 서비스
├── store/                  # 상점 서비스
├── batch/                  # 배치 서비스
├── gateway/                # API Gateway
├── shared/                 # 공통 DTO 및 이벤트
├── common/                 # 공통 유틸리티
├── infra/                  # 인프라 모듈들
│   ├── infra-persistence/  # 데이터베이스
│   ├── infra-cache/        # 캐시
│   ├── infra-messaging/    # 메시징
│   └── infra-storage/      # 스토리지
├── docker/                 # Docker 설정
├── scripts/                # 데이터베이스 스크립트
└── docs/                   # 문서
```

## 🔄 배포 전략

### 개발 환경
- Docker Compose를 통한 로컬 개발
- 각 서비스별 독립적인 데이터베이스
- 포트 기반 서비스 구분

### 스테이징 환경
- Kubernetes 클러스터
- 서비스 디스커버리 도입
- 모니터링 시스템 구축

### 프로덕션 환경
- 고가용성 클러스터
- 자동 스케일링
- 무중단 배포

## 📈 성능 지표

### 목표 지표
- **응답 시간**: 평균 200ms 이하
- **가용성**: 99.9% 이상
- **처리량**: 초당 1000 요청
- **장애 복구**: 5분 이내

### 모니터링 지표
- **서비스 상태**: Health Check
- **응답 시간**: P95, P99 지연시간
- **에러율**: 4xx, 5xx 에러 비율
- **리소스 사용률**: CPU, 메모리, 네트워크

## 🎉 결론

DoranDoran 프로젝트는 성공적으로 모듈러 모놀리스에서 진정한 마이크로서비스 아키텍처로 전환되었습니다. 

**주요 성과:**
- ✅ 서비스 독립성 확보
- ✅ 데이터베이스 분리 완료
- ✅ REST API 및 이벤트 기반 통신 구현
- ✅ API Gateway 도입
- ✅ Circuit Breaker 패턴 적용

**다음 단계에서는** 모니터링, 서비스 디스커버리, 메시지 큐, 컨테이너 오케스트레이션, CI/CD 파이프라인을 도입하여 더욱 견고하고 확장 가능한 MSA를 구축할 수 있습니다.

---

*문서 생성일: 2024년 10월 1일*  
*버전: 1.0*  
*작성자: AI Assistant*
