# API 변경사항 문서 (2026-01-25)

## 개요

이 문서는 2026년 1월 25일자 API 변경사항을 기록합니다.

## 주요 변경사항

### 1. 탈퇴 후 30일 재가입 제한 기능 추가

#### 변경 배경
- 탈퇴한 사용자가 즉시 재가입하는 것을 방지
- 서비스 안정성 및 데이터 무결성 보장
- 사용자 재가입에 대한 쿨다운 기간 적용

#### 데이터베이스 스키마 변경

**테이블**: `user_schema.app_user`

**추가된 컬럼**:
```sql
ALTER TABLE user_schema.app_user 
ADD COLUMN inactive_at TIMESTAMP WITHOUT TIME ZONE NULL;
```

- **컬럼명**: `inactive_at`
- **타입**: `TIMESTAMP WITHOUT TIME ZONE`
- **NULL 허용**: YES
- **설명**: 사용자가 탈퇴(INACTIVE 상태로 변경)된 시간을 기록

#### 엔티티 변경

**파일**: `user/src/main/java/com/dorandoran/user/entity/User.java`

**추가된 필드**:
```java
@Column(name = "inactive_at", nullable = true)
private LocalDateTime inactiveAt;
```

**수정된 메서드**:
```java
public void updateStatus(UserStatus status) {
    this.status = status;
    // INACTIVE로 변경 시 탈퇴 시간 기록
    if (status == UserStatus.INACTIVE && this.inactiveAt == null) {
        this.inactiveAt = LocalDateTime.now();
    }
    // ACTIVE로 변경 시 탈퇴 시간 초기화
    if (status == UserStatus.ACTIVE) {
        this.inactiveAt = null;
    }
}
```

#### Repository 변경

**파일**: `user/src/main/java/com/dorandoran/user/repository/UserRepository.java`

**추가된 메서드**:
```java
/**
 * 이메일과 상태로 사용자 조회 (탈퇴한 사용자 확인용)
 */
Optional<User> findByEmailAndStatus(String email, User.UserStatus status);
```

#### API 변경사항

##### 1. 회원가입 API (`POST /api/users`)

**변경 전**:
- 탈퇴한 사용자(INACTIVE 상태)는 즉시 재가입 가능

**변경 후**:
- 탈퇴한 사용자가 30일 이내에 재가입 시도 시 오류 반환
- 탈퇴 후 30일이 지나야 재가입 가능

**요청 예시**:
```http
POST /api/users
Content-Type: application/json

{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "password": "password123",
  "birthDate": "1990-01-01",
  ...
}
```

**성공 응답** (30일 경과 후):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    ...
  },
  "message": "사용자가 성공적으로 생성되었습니다."
}
```

**실패 응답** (30일 미경과):
```json
{
  "success": false,
  "message": "탈퇴 후 30일이 지나야 재가입할 수 있습니다. (남은 기간: 15일)",
  "errorCode": "INVALID_REQUEST"
}
```

**에러 코드**:
- `INVALID_REQUEST`: 탈퇴 후 30일 미경과 시
- `EMAIL_ALREADY_EXISTS`: ACTIVE 상태의 사용자가 이미 존재할 때

##### 2. OAuth 회원가입 API (`POST /api/auth/oauth/login`)

**변경 전**:
- 탈퇴한 사용자(INACTIVE 상태)는 즉시 재가입 가능

**변경 후**:
- 탈퇴한 사용자가 30일 이내에 재가입 시도 시 오류 반환
- 탈퇴 후 30일이 지나야 재가입 가능

**요청 예시**:
```http
POST /api/auth/oauth/login
Content-Type: application/json

{
  "provider": "GOOGLE",
  "idToken": "...",
  ...
}
```

**실패 응답** (30일 미경과):
```json
{
  "success": false,
  "message": "탈퇴 후 30일이 지나야 재가입할 수 있습니다. (남은 기간: 15일)",
  "errorCode": "INVALID_REQUEST"
}
```

##### 3. 회원탈퇴 API (`DELETE /api/users/{userId}`)

**변경 전**:
- 사용자 상태만 INACTIVE로 변경
- 탈퇴 시간 기록 없음

**변경 후**:
- 사용자 상태를 INACTIVE로 변경
- `inactive_at` 컬럼에 탈퇴 시간 자동 기록
- 이미 탈퇴한 사용자 재요청 시 성공 처리 (멱등성 보장)

**요청 예시**:
```http
DELETE /api/users/{userId}
Authorization: Bearer {token}
```

**동작**:
1. 사용자 상태를 `INACTIVE`로 변경
2. `inactive_at`에 현재 시간(`LocalDateTime.now()`) 기록
3. 이미 `INACTIVE` 상태인 경우 그냥 성공 처리 (멱등성)

#### 비즈니스 로직

**재가입 제한 로직**:
```java
// 1. 탈퇴한 사용자 확인
Optional<User> inactiveUser = userRepository.findByEmailAndStatus(email, User.UserStatus.INACTIVE);

if (inactiveUser.isPresent()) {
    User user = inactiveUser.get();
    if (user.getInactiveAt() != null) {
        // 2. 탈퇴 시간으로부터 30일 경과 여부 확인
        LocalDateTime thirtyDaysLater = user.getInactiveAt().plusDays(30);
        if (thirtyDaysLater.isAfter(LocalDateTime.now())) {
            // 3. 30일 미경과 시 오류 반환
            long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), thirtyDaysLater);
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, 
                String.format("탈퇴 후 30일이 지나야 재가입할 수 있습니다. (남은 기간: %d일)", remainingDays));
        }
    }
}
```

**탈퇴 처리 로직**:
```java
// 사용자 상태를 INACTIVE로 변경하면 자동으로 inactiveAt이 설정됨
user.updateStatus(User.UserStatus.INACTIVE);
// → inactiveAt에 현재 시간 자동 기록
```

#### 영향받는 API 목록

1. **POST /api/users** - 일반 회원가입
2. **POST /api/auth/oauth/login** - OAuth 회원가입 (신규 사용자 자동 생성 시)
3. **DELETE /api/users/{userId}** - 회원탈퇴 (동작 변경 없음, 내부 로직만 변경)

#### 마이그레이션 가이드

**기존 데이터 처리**:
- 기존에 탈퇴한 사용자(`status = 'INACTIVE'`)의 `inactive_at`은 `NULL`로 유지됨
- `inactive_at`이 `NULL`인 경우 30일 제한 체크를 건너뜀 (기존 사용자는 재가입 가능)
- 새로운 탈퇴부터 30일 제한이 적용됨

**주의사항**:
- 기존 탈퇴 사용자에게도 제한을 적용하려면 `updated_at`을 참고하여 `inactive_at`을 업데이트하는 마이그레이션 스크립트 필요
- 현재는 기존 데이터에 대한 마이그레이션 없이 새로 탈퇴하는 사용자부터 적용

#### 테스트 시나리오

1. **정상 재가입 (30일 경과 후)**
   - 사용자 A가 탈퇴
   - 30일 후 재가입 시도
   - ✅ 성공

2. **재가입 제한 (30일 미경과)**
   - 사용자 B가 탈퇴
   - 15일 후 재가입 시도
   - ❌ 실패: "탈퇴 후 30일이 지나야 재가입할 수 있습니다. (남은 기간: 15일)"

3. **기존 탈퇴 사용자 (inactive_at = NULL)**
   - 기존에 탈퇴한 사용자 C (inactive_at = NULL)
   - 재가입 시도
   - ✅ 성공 (기존 데이터는 제한 없음)

4. **ACTIVE 사용자 재가입 시도**
   - ACTIVE 상태의 사용자 D가 재가입 시도
   - ❌ 실패: "이미 사용 중인 이메일입니다."

#### 관련 파일

- `user/src/main/java/com/dorandoran/user/entity/User.java`
- `user/src/main/java/com/dorandoran/user/service/UserService.java`
- `user/src/main/java/com/dorandoran/user/repository/UserRepository.java`
- 데이터베이스: `user_schema.app_user` 테이블

---

### 2. 마케팅 수신 동의 여부 저장 추가

#### 변경 배경
- 회원가입 페이지에서 마케팅 수신 동의 체크박스 값을 서버로 전달
- 이전에는 프론트엔드에서만 관리되어 서버에 저장되지 않음

#### 데이터베이스 스키마 변경

**테이블**: `user_schema.app_user`

**추가된 컬럼**:
```sql
ALTER TABLE user_schema.app_user
ADD COLUMN marketing_opt_in BOOLEAN DEFAULT false NOT NULL;

ALTER TABLE user_schema.app_user
ADD COLUMN marketing_consent BOOLEAN DEFAULT false NOT NULL;

ALTER TABLE user_schema.app_user
ADD COLUMN marketing_consent_at TIMESTAMP WITHOUT TIME ZONE NULL;
```

- **컬럼명**: `marketing_opt_in`
- **타입**: `BOOLEAN`
- **기본값**: `false`
- **설명**: 마케팅 수신 동의 여부 (회원가입 시)

- **컬럼명**: `marketing_consent`
- **타입**: `BOOLEAN`
- **기본값**: `false`
- **설명**: 마케팅 수신 동의 여부 (선택 약관)

- **컬럼명**: `marketing_consent_at`
- **타입**: `TIMESTAMP WITHOUT TIME ZONE`
- **NULL 허용**: YES
- **설명**: 마케팅 수신 동의 시각 (동의하지 않으면 NULL)

#### 엔티티 변경

**파일**: `user/src/main/java/com/dorandoran/user/entity/User.java`

**추가된 필드**:
```java
@Column(name = "marketing_opt_in", nullable = false)
@Builder.Default
private boolean marketingOptIn = false;

@Column(name = "marketing_consent", nullable = false)
@Builder.Default
private boolean marketingConsent = false;

@Column(name = "marketing_consent_at")
private LocalDateTime marketingConsentAt;
```

**추가된 메서드**:
```java
/**
 * 마케팅 수신 동의 업데이트
 */
public void updateMarketingConsent(boolean marketingConsent) {
    this.marketingConsent = marketingConsent;
    this.marketingConsentAt = marketingConsent ? LocalDateTime.now() : null;
}
```

#### DTO 변경

**파일**: `shared/src/main/java/com/dorandoran/shared/dto/CreateUserRequest.java`

**추가된 필드**:
```java
Boolean marketingOptIn
```

#### 서비스 변경

**파일**: `user/src/main/java/com/dorandoran/user/service/UserService.java`

**회원가입 처리 시 반영**:
```java
.marketingOptIn(Boolean.TRUE.equals(request.marketingOptIn()))
```

**마케팅 동의 업데이트 메서드 추가**:
```java
/**
 * 마케팅 수신 동의 업데이트 (선택 약관)
 */
@Transactional
public User updateMarketingConsent(UUID userId, boolean marketingConsent) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));

    // 엔티티 메서드에서 동의 시각까지 함께 처리
    user.updateMarketingConsent(marketingConsent);
    return userRepository.save(user);
}
```

#### API 변경사항

##### 회원가입 API (`POST /api/users`)

**요청 예시**:
```http
POST /api/users
Content-Type: application/json

{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "password": "password123",
  "birthDate": "1990-01-01",
  "signupQuestion": "가장 좋아하는 색은?",
  "signupAnswer": "Blue",
  "marketingOptIn": true
}
```

#### 마이그레이션 가이드

**기존 데이터 처리**:
- 기존 사용자 레코드는 `marketing_opt_in = false`, `marketing_consent = false`로 기본값 적용
- 기존 사용자 레코드는 `marketing_consent_at = NULL`로 설정됨

#### 비즈니스 로직

**마케팅 동의 업데이트 로직**:
```java
// 마케팅 동의를 true로 설정하면 동의 시각이 자동으로 기록됨
user.updateMarketingConsent(true);
// → marketingConsent = true, marketingConsentAt = LocalDateTime.now()

// 마케팅 동의를 false로 설정하면 동의 시각이 NULL로 초기화됨
user.updateMarketingConsent(false);
// → marketingConsent = false, marketingConsentAt = null
```

#### 관련 파일

- `shared/src/main/java/com/dorandoran/shared/dto/CreateUserRequest.java`
- `user/src/main/java/com/dorandoran/user/entity/User.java`
- `user/src/main/java/com/dorandoran/user/service/UserService.java`
- 데이터베이스: `user_schema.app_user` 테이블

---

## 변경 이력

- **2026-01-25**: 탈퇴 후 30일 재가입 제한 기능 추가
  - 데이터베이스 스키마 변경 (`inactive_at` 컬럼 추가)
  - 엔티티 및 서비스 로직 변경
  - 회원가입 API에 30일 제한 체크 로직 추가
- **2026-01-25**: 마케팅 수신 동의 여부 저장 추가
  - 데이터베이스 스키마 변경 (`marketing_opt_in`, `marketing_consent`, `marketing_consent_at` 컬럼 추가)
  - 회원가입 요청 DTO에 `marketingOptIn` 추가
  - 회원가입 서비스 로직에 동의 여부 저장 반영
  - 마케팅 동의 업데이트 메서드 추가 (`updateMarketingConsent`)
