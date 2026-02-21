# 프롬프트 관리가 user_schema에 있는 이유

## 1. 설계 배경

프롬프트 관리 시스템(`prompt_versions`, `prompt_actives`)이 `user_schema`에 위치한 이유는 다음과 같습니다.

## 2. 주요 설계 이유

### 2.1 관리자 기능의 집중화 (Admin Features Centralization)

**User Service가 관리자 기능의 중심입니다:**

```java
// user/src/main/java/com/dorandoran/user/admin/
- PromptController.java          // 프롬프트 관리 API
- ReviewTicketController.java     // 관리 필요 내역
- AdminAuditLogService.java      // 관리 감사 로그
```

**관련 Entity들도 모두 user_schema에 위치:**
- `user_schema.prompt_versions` - 프롬프트 버전 관리
- `user_schema.prompt_actives` - 활성 프롬프트 관리
- `user_schema.admin_audit_logs` - 관리 감사 로그
- `user_schema.review_tickets` - 관리 필요 내역

이렇게 관리자 기능을 한 곳에 모아서:
- **일관된 권한 관리**: 모든 관리 기능이 동일한 인증/인가 체계 사용
- **통합된 감사 로그**: 모든 관리 작업을 한 곳에서 추적
- **관리자 UI 통합**: 프론트엔드에서 관리 기능을 한 곳에서 제공

### 2.2 사용자 권한 관리와의 밀접한 연관성

프롬프트 관리는 **관리자(Admin User)의 작업**입니다:

```sql
-- 프롬프트 버전 생성자
prompt_versions.created_by → app_user.id

-- 프롬프트 활성화한 사용자
prompt_actives.activated_by → app_user.id
```

**외래 키 관계:**
- `prompt_versions.created_by` → `user_schema.app_user.id`
- `prompt_actives.activated_by` → `user_schema.app_user.id`

이러한 관계 때문에:
- **사용자 정보와 함께 관리**: 프롬프트를 누가 생성/활성화했는지 추적 가능
- **권한 검증 용이**: `app_user.role = 'ROLE_ADMIN'` 체크와 함께 사용
- **데이터 일관성**: 사용자 관련 데이터가 한 스키마에 모여있어 트랜잭션 관리 용이

### 2.3 감사 로그(Audit Log)와의 통합

프롬프트 관리 작업은 **감사 로그**에 기록됩니다:

```java
// PromptController.java
adminAuditLogService.logAction(
    ActionType.CREATE,  // 또는 ACTIVATE, ROLLBACK
    TargetType.PROMPT,
    versionId,
    summary,
    beforeJson,
    afterJson,
    adminUserId,
    request
);
```

**감사 로그도 user_schema에 위치:**
- `user_schema.admin_audit_logs` - 모든 관리 작업 기록

같은 스키마에 있으면:
- **트랜잭션 일관성**: 프롬프트 변경과 감사 로그 기록을 하나의 트랜잭션으로 처리
- **조회 성능**: JOIN 없이 같은 스키마에서 모든 정보 조회 가능
- **데이터 무결성**: 외래 키 제약으로 데이터 일관성 보장

### 2.4 서비스 책임 분리 (Service Responsibility Separation)

**User Service의 책임:**
- ✅ 프롬프트 **관리** (CRUD, 버전 관리, 활성화, 롤백)
- ✅ 관리자 권한 검증
- ✅ 감사 로그 기록
- ✅ 프롬프트 테스트 요청 (Chat Service에 위임)

**Chat Service의 책임:**
- ✅ 프롬프트 **사용** (Agent에서 실제 프롬프트 읽기)
- ✅ 프롬프트 테스트 실행 (Agent 호출)
- ✅ 동적 프롬프트 생성 (`PromptService.buildSystemPrompt()`)

**분리 이유:**
- **관리 vs 사용**: 프롬프트를 관리하는 것과 사용하는 것은 다른 책임
- **독립적 배포**: 프롬프트 관리 기능 변경이 Chat Service에 영향 없음
- **확장성**: 향후 다른 서비스에서도 프롬프트를 사용할 수 있도록 분리

### 2.5 데이터 접근 패턴

**프롬프트 관리 작업의 접근 패턴:**
- **주로 관리자만 접근**: 일반 사용자는 프롬프트를 직접 관리하지 않음
- **낮은 빈도, 높은 중요성**: 자주 변경되지 않지만 변경 시 감사 필요
- **관리자 UI 중심**: 웹 관리자 페이지에서만 사용

**프롬프트 사용 작업의 접근 패턴:**
- **모든 사용자가 간접 접근**: Agent를 통해 프롬프트 사용
- **높은 빈도**: 채팅할 때마다 프롬프트 조회
- **성능 중요**: 빠른 조회를 위해 캐싱 필요

이러한 접근 패턴 차이로 인해:
- **관리**: User Service (관리자 중심, 감사 중요)
- **사용**: Chat Service (사용자 중심, 성능 중요)

## 3. 대안과 비교

### 3.1 chat_schema에 두는 경우

**장점:**
- 프롬프트를 사용하는 곳과 가까움
- JOIN 없이 프롬프트 조회 가능

**단점:**
- ❌ 관리자 기능이 Chat Service에 섞임
- ❌ 감사 로그와 분리되어 트랜잭션 관리 복잡
- ❌ 사용자 권한 관리와 분리
- ❌ 다른 서비스에서 프롬프트를 사용할 때 Chat Service 의존성 증가

### 3.2 별도 admin_schema를 만드는 경우

**장점:**
- 관리 기능을 명확히 분리
- 확장성 좋음

**단점:**
- ❌ 스키마가 너무 많아짐 (현재 9개)
- ❌ 사용자 정보와의 외래 키 관계가 복잡해짐
- ❌ 트랜잭션 관리 복잡도 증가

### 3.3 현재 설계 (user_schema)의 장점

✅ **관리자 기능 집중화**: 모든 관리 기능이 한 곳에
✅ **사용자 권한과 통합**: `app_user`와 밀접한 관계
✅ **감사 로그 통합**: 같은 스키마에서 모든 추적 가능
✅ **트랜잭션 일관성**: 하나의 트랜잭션으로 관리 작업 처리
✅ **명확한 책임 분리**: User Service = 관리, Chat Service = 사용

## 4. 현재 구조 요약

```
┌─────────────────────────────────────┐
│         User Service                 │
│  (관리자 기능 중심)                   │
├─────────────────────────────────────┤
│  - PromptController                 │
│  - PromptService                    │
│  - AdminAuditLogService              │
│  - ReviewTicketService              │
└─────────────────────────────────────┘
           │
           │ 관리 (CRUD, 활성화, 롤백)
           │
           ▼
┌─────────────────────────────────────┐
│      user_schema                     │
├─────────────────────────────────────┤
│  - prompt_versions                  │
│  - prompt_actives                   │
│  - admin_audit_logs                │
│  - review_tickets                   │
│  - app_user (FK 참조)               │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│         Chat Service                │
│  (프롬프트 사용 중심)                 │
├─────────────────────────────────────┤
│  - PromptService.buildSystemPrompt()│
│  - Agent들 (프롬프트 사용)           │
│  - PromptTestService                │
└─────────────────────────────────────┘
           │
           │ 사용 (읽기 전용)
           │
           ▼
┌─────────────────────────────────────┐
│      user_schema.prompt_actives     │
│      (또는 파일 기반)                │
└─────────────────────────────────────┘
```

## 5. 결론

프롬프트 관리가 `user_schema`에 있는 것은:
1. **관리자 기능의 자연스러운 집중화**
2. **사용자 권한 관리와의 밀접한 연관성**
3. **감사 로그와의 통합 필요성**
4. **명확한 서비스 책임 분리**

이러한 설계는 **관리 기능의 일관성**, **데이터 무결성**, **확장성**을 모두 만족하는 합리적인 선택입니다.
