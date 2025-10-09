# 추가 필요한 다이어그램 제안

## 1. 배포 및 인프라 다이어그램

### Docker 컨테이너 아키텍처
```mermaid
graph TB
    subgraph "Docker Host"
        subgraph "Database Containers"
            PostgreSQL[(PostgreSQL<br/>shared-db:5432)]
            Redis[(Redis<br/>redis:6379)]
        end
        
        subgraph "Application Containers"
            AuthContainer[Auth Service<br/>dd-auth-service:8081]
            UserContainer[User Service<br/>dd-user-service:8082]
            ChatContainer[Chat Service<br/>dd-chat-service:8083]
            BatchContainer[Batch Service<br/>dd-batch-service:8085]
            GatewayContainer[API Gateway<br/>dd-api-gateway:8080]
        end
        
        subgraph "Monitoring Containers"
            PrometheusContainer[Prometheus<br/>dd-prometheus:9090]
            GrafanaContainer[Grafana<br/>dd-grafana:3000]
        end
    end

    subgraph "External Services"
        OpenAI[OpenAI API]
    end

    %% Container Dependencies
    AuthContainer --> PostgreSQL
    UserContainer --> PostgreSQL
    ChatContainer --> PostgreSQL
    BatchContainer --> PostgreSQL
    
    AuthContainer --> Redis
    UserContainer --> Redis
    ChatContainer --> Redis
    GatewayContainer --> Redis
    
    ChatContainer --> OpenAI
    
    GatewayContainer --> AuthContainer
    GatewayContainer --> UserContainer
    GatewayContainer --> ChatContainer
    GatewayContainer --> BatchContainer
    
    PrometheusContainer --> AuthContainer
    PrometheusContainer --> UserContainer
    PrometheusContainer --> ChatContainer
    PrometheusContainer --> BatchContainer
    
    GrafanaContainer --> PrometheusContainer
```

### Kubernetes 배포 아키텍처 (향후 확장)
```mermaid
graph TB
    subgraph "Kubernetes Cluster"
        subgraph "Namespace: dorandoran"
            subgraph "Database Layer"
                PostgreSQLPod[PostgreSQL Pod]
                RedisPod[Redis Pod]
            end
            
            subgraph "Application Layer"
                AuthDeployment[Auth Deployment<br/>Replicas: 2]
                UserDeployment[User Deployment<br/>Replicas: 2]
                ChatDeployment[Chat Deployment<br/>Replicas: 3]
                BatchDeployment[Batch Deployment<br/>Replicas: 1]
                GatewayDeployment[Gateway Deployment<br/>Replicas: 2]
            end
            
            subgraph "Monitoring Layer"
                PrometheusDeployment[Prometheus Deployment]
                GrafanaDeployment[Grafana Deployment]
            end
        end
    end

    subgraph "External Services"
        LoadBalancer[Load Balancer]
        OpenAI[OpenAI API]
    end

    LoadBalancer --> GatewayDeployment
    ChatDeployment --> OpenAI
```

## 2. 보안 아키텍처 다이어그램

### 보안 계층 구조
```mermaid
graph TB
    subgraph "Security Layers"
        subgraph "Network Security"
            Firewall[Firewall]
            LoadBalancer[Load Balancer]
            WAF[Web Application Firewall]
        end
        
        subgraph "Application Security"
            Gateway[API Gateway]
            RateLimiter[Rate Limiter]
            JWTAuth[JWT Authentication]
            RBAC[Role-Based Access Control]
        end
        
        subgraph "Data Security"
            Encryption[Data Encryption]
            Hashing[Password Hashing]
            TokenBlacklist[Token Blacklist]
        end
        
        subgraph "Infrastructure Security"
            ContainerSecurity[Container Security]
            SecretManagement[Secret Management]
            NetworkPolicies[Network Policies]
        end
    end

    subgraph "External Threats"
        DDoS[DDoS Attacks]
        Injection[SQL Injection]
        XSS[XSS Attacks]
        CSRF[CSRF Attacks]
    end

    DDoS --> Firewall
    Injection --> WAF
    XSS --> WAF
    CSRF --> JWTAuth
```

### 인증/인가 플로우
```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Auth
    participant User
    participant Redis

    Client->>Gateway: API 요청 + JWT
    Gateway->>Auth: JWT 검증
    Auth->>Redis: 토큰 블랙리스트 확인
    Redis-->>Auth: 토큰 상태
    Auth->>User: 사용자 정보 조회
    User-->>Auth: 사용자 정보
    Auth->>Auth: 권한 검증
    Auth-->>Gateway: 인증 성공/실패
    Gateway-->>Client: 응답
```

## 3. 모니터링 및 로깅 아키텍처

### 모니터링 스택
```mermaid
graph TB
    subgraph "Application Layer"
        Auth[Auth Service]
        User[User Service]
        Chat[Chat Service]
        Batch[Batch Service]
        Gateway[API Gateway]
    end

    subgraph "Metrics Collection"
        Actuator[Spring Actuator]
        Micrometer[Micrometer]
        Prometheus[Prometheus]
    end

    subgraph "Logging"
        Logback[Logback]
        ELK[ELK Stack]
        Fluentd[Fluentd]
    end

    subgraph "Visualization"
        Grafana[Grafana]
        Kibana[Kibana]
    end

    subgraph "Alerting"
        AlertManager[Alert Manager]
        Slack[Slack Notifications]
        Email[Email Alerts]
    end

    Auth --> Actuator
    User --> Actuator
    Chat --> Actuator
    Batch --> Actuator
    Gateway --> Actuator

    Actuator --> Micrometer
    Micrometer --> Prometheus

    Auth --> Logback
    User --> Logback
    Chat --> Logback
    Batch --> Logback
    Gateway --> Logback

    Logback --> ELK
    ELK --> Kibana

    Prometheus --> Grafana
    Prometheus --> AlertManager
    AlertManager --> Slack
    AlertManager --> Email
```

### 메트릭 수집 구조
```mermaid
graph LR
    subgraph "Application Metrics"
        HTTP[HTTP Requests]
        DB[Database Queries]
        Cache[Cache Operations]
        AI[AI API Calls]
    end

    subgraph "System Metrics"
        CPU[CPU Usage]
        Memory[Memory Usage]
        Disk[Disk I/O]
        Network[Network I/O]
    end

    subgraph "Business Metrics"
        Users[Active Users]
        Messages[Messages Sent]
        AIUsage[AI Token Usage]
        Errors[Error Rate]
    end

    HTTP --> Prometheus
    DB --> Prometheus
    Cache --> Prometheus
    AI --> Prometheus
    CPU --> Prometheus
    Memory --> Prometheus
    Disk --> Prometheus
    Network --> Prometheus
    Users --> Prometheus
    Messages --> Prometheus
    AIUsage --> Prometheus
    Errors --> Prometheus
```

## 4. 데이터 흐름 다이어그램

### 사용자 메시지 처리 데이터 흐름
```mermaid
flowchart TD
    A[사용자 메시지] --> B[API Gateway]
    B --> C[JWT 검증]
    C --> D[Chat Service]
    D --> E[메시지 저장]
    E --> F[Multi-Agent 처리]
    
    F --> G[IntimacyAgent]
    F --> H[VocabularyAgent]
    F --> I[ConversationAgent]
    
    G --> J[OpenAI API]
    H --> J
    I --> J
    
    J --> K[AI 응답]
    K --> L[SSE 스트림]
    L --> M[클라이언트]
    
    G --> N[친밀도 분석 결과]
    H --> O[어휘 추출 결과]
    I --> P[대화 응답]
    
    N --> Q[진척도 업데이트]
    O --> R[번역 처리]
    P --> S[실시간 스트림]
    
    Q --> T[데이터베이스 저장]
    R --> T
    S --> T
```

### 이벤트 기반 데이터 흐름
```mermaid
flowchart TD
    A[사용자 상태 변경] --> B[User Service]
    B --> C[UserStatusChangedEvent 발행]
    C --> D[Spring Event Bus]
    
    D --> E[Auth Service]
    D --> F[Chat Service]
    
    E --> G[인증 정책 업데이트]
    F --> H[채팅방 접근 제한]
    
    G --> I[Redis 업데이트]
    H --> J[채팅방 상태 변경]
    
    I --> K[캐시 동기화]
    J --> L[사용자 알림]
```

## 5. 성능 및 확장성 다이어그램

### 로드 밸런싱 전략
```mermaid
graph TB
    subgraph "Load Balancer Layer"
        LB[Load Balancer]
    end

    subgraph "API Gateway Cluster"
        Gateway1[Gateway Instance 1]
        Gateway2[Gateway Instance 2]
        Gateway3[Gateway Instance 3]
    end

    subgraph "Service Clusters"
        subgraph "Auth Service Cluster"
            Auth1[Auth Instance 1]
            Auth2[Auth Instance 2]
        end
        
        subgraph "User Service Cluster"
            User1[User Instance 1]
            User2[User Instance 2]
        end
        
        subgraph "Chat Service Cluster"
            Chat1[Chat Instance 1]
            Chat2[Chat Instance 2]
            Chat3[Chat Instance 3]
        end
    end

    LB --> Gateway1
    LB --> Gateway2
    LB --> Gateway3

    Gateway1 --> Auth1
    Gateway1 --> User1
    Gateway1 --> Chat1

    Gateway2 --> Auth2
    Gateway2 --> User2
    Gateway2 --> Chat2

    Gateway3 --> Auth1
    Gateway3 --> User1
    Gateway3 --> Chat3
```

### 캐싱 전략
```mermaid
graph TB
    subgraph "Application Layer"
        Auth[Auth Service]
        User[User Service]
        Chat[Chat Service]
    end

    subgraph "Cache Layer"
        Redis1[Redis Master]
        Redis2[Redis Replica 1]
        Redis3[Redis Replica 2]
    end

    subgraph "Cache Types"
        SessionCache[Session Cache]
        UserCache[User Data Cache]
        ChatCache[Chat Context Cache]
        AICache[AI Response Cache]
    end

    Auth --> SessionCache
    User --> UserCache
    Chat --> ChatCache
    Chat --> AICache

    SessionCache --> Redis1
    UserCache --> Redis1
    ChatCache --> Redis1
    AICache --> Redis1

    Redis1 --> Redis2
    Redis1 --> Redis3
```

## 6. 장애 복구 및 백업 다이어그램

### 장애 복구 전략
```mermaid
graph TB
    subgraph "Primary Region"
        subgraph "Database"
            PrimaryDB[(Primary PostgreSQL)]
            StandbyDB[(Standby PostgreSQL)]
        end
        
        subgraph "Application"
            PrimaryApp[Primary Services]
        end
    end

    subgraph "Secondary Region"
        subgraph "Database"
            ReplicaDB[(Replica PostgreSQL)]
        end
        
        subgraph "Application"
            StandbyApp[Standby Services]
        end
    end

    subgraph "Backup Strategy"
        DailyBackup[Daily Backups]
        WeeklyBackup[Weekly Backups]
        MonthlyBackup[Monthly Backups]
    end

    PrimaryDB --> StandbyDB
    PrimaryDB --> ReplicaDB
    PrimaryApp --> StandbyApp

    PrimaryDB --> DailyBackup
    DailyBackup --> WeeklyBackup
    WeeklyBackup --> MonthlyBackup
```

## 7. 개발 및 CI/CD 파이프라인

### CI/CD 파이프라인
```mermaid
graph LR
    A[Code Commit] --> B[GitHub]
    B --> C[GitHub Actions]
    C --> D[Build & Test]
    D --> E[Security Scan]
    E --> F[Docker Build]
    F --> G[Push to Registry]
    G --> H[Deploy to Staging]
    H --> I[Integration Tests]
    I --> J[Deploy to Production]
    J --> K[Health Check]
    K --> L[Monitoring]
```

## 8. 비용 및 리소스 관리

### 리소스 사용량 모니터링
```mermaid
graph TB
    subgraph "Resource Monitoring"
        CPU[CPU Usage]
        Memory[Memory Usage]
        Storage[Storage Usage]
        Network[Network Usage]
        AI[AI API Costs]
    end

    subgraph "Cost Optimization"
        AutoScaling[Auto Scaling]
        ResourceRightSizing[Resource Right-sizing]
        CacheOptimization[Cache Optimization]
        AIUsageOptimization[AI Usage Optimization]
    end

    subgraph "Budget Management"
        BudgetAlerts[Budget Alerts]
        CostForecasting[Cost Forecasting]
        ResourceRecommendations[Resource Recommendations]
    end

    CPU --> AutoScaling
    Memory --> ResourceRightSizing
    Storage --> CacheOptimization
    AI --> AIUsageOptimization

    AutoScaling --> BudgetAlerts
    ResourceRightSizing --> CostForecasting
    CacheOptimization --> ResourceRecommendations
    AIUsageOptimization --> BudgetAlerts
```

## 권장사항

### 1. 즉시 구현 권장
- **모니터링 다이어그램**: 운영 환경에서 필수
- **보안 아키텍처**: 보안 강화를 위해 필요
- **배포 다이어그램**: 인프라 관리에 필수

### 2. 중기 구현 권장
- **성능 및 확장성 다이어그램**: 트래픽 증가 대비
- **장애 복구 다이어그램**: 안정성 향상
- **CI/CD 파이프라인**: 개발 효율성 향상

### 3. 장기 구현 권장
- **Kubernetes 배포**: 클라우드 네이티브 전환
- **비용 관리**: 운영 비용 최적화
- **고급 모니터링**: AI/ML 기반 예측 모니터링
