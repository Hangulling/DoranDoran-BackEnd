# Signup API (추가 필드 적용)

## 엔드포인트
- `POST /api/users`  
- `POST /api/users/register` (동일 처리, 메시지만 다름)

## 요청 바디 (application/json)
| 필드 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| email | string | Y | 이메일 형식 |
| firstName | string | Y | 1~50자 |
| lastName | string | Y | 1~50자 |
| name | string | Y | 1~50자, 비었으면 firstName + lastName 사용 |
| password | string | Y | 8~100자, 영문/숫자 포함 |
| picture | string | N | URL 등 제한 없음 |
| info | string | N | 최대 100자 |
| birthDate | string | Y | `yyyy-MM-dd` 형식 |
| signupQuestion | string | Y | 최대 255자 |
| signupAnswer | string | Y | 최대 30자, 첫 글자 공백 불가 (`^(?!\\s).{1,30}$`) |

## 처리 흐름
1) 이메일 중복 확인  
2) Auth 서비스의 이메일 인증 여부 확인  
3) 비밀번호 정책 검증 및 암호화  
4) `birthDate`를 `LocalDate`로 파싱 후 저장, 질문/답변 저장  
5) 사용자 생성 후 이메일 인증 데이터 삭제, `UserCreatedEvent` 발행

## 응답 예시 (성공 200)
```json
{
  "success": true,
  "data": {
    "id": "uuid-string",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "name": "John Doe",
    "picture": "https://example.com/avatar.png",
    "info": "hello",
    "birthDate": "1990-01-01",
    "signupQuestion": "가장 좋아하는 색은?",
    "signupAnswer": "Blue",
    "status": "ACTIVE",
    "role": "ROLE_USER",
    "coachCheck": false,
    "exitModalDoNotShowAgain": false,
    "isOnboard": false,
    "createdAt": "...",
    "updatedAt": "..."
  },
  "message": "회원가입이 성공적으로 완료되었습니다."
}
```

## 주요 오류 코드 (400 등)
- `EMAIL_ALREADY_EXISTS`
- `INVALID_REQUEST` (미인증 이메일, 형식/밸리데이션 실패)
- `USER_NOT_FOUND` (내부 조회 실패 시)

## DB 스키마 정보
- `birth_date`: `DATE NOT NULL DEFAULT '1900-01-01'`
- `signup_question`: `VARCHAR(255) NOT NULL DEFAULT '질문이 설정되지 않았습니다.'`
- `signup_answer`: `VARCHAR(30) NOT NULL DEFAULT '답변이 설정되지 않았습니다.'`

## 비고
- DB 컬럼은 NOT NULL 제약조건과 기본값이 설정되어 있습니다.
- 애플리케이션 레벨에서도 @NotBlank 등으로 필수 검증을 수행합니다.
- OAuth 사용자 생성 시에는 엔티티의 @Builder.Default 값이 자동으로 적용됩니다.

---

# 비밀번호 찾기 API

## 1. 비밀번호 재설정 코드 요청

### 엔드포인트
- `POST /api/auth/password/reset/request-code`

### 요청 바디 (application/json)
| 필드 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| email | string | Y | 이메일 형식 |

### 처리 흐름
1) 이메일로 사용자 조회  
2) 사용자 없음 → `USER_NOT_FOUND` 반환  
3) OAuth 사용자 체크 → OAuth 사용자인 경우 `OAUTH_USER_CANNOT_RESET_PASSWORD` 반환  
4) 기존 코드 무효화 (재발송 시)  
5) 6자리 랜덤 코드 생성  
6) Redis에 코드 저장 (5분 TTL)  
7) Google SMTP로 이메일 발송

### 응답 예시 (성공 200)
```json
{
  "success": true,
  "data": null,
  "message": "비밀번호 재설정 코드가 이메일로 발송되었습니다."
}
```

### 주요 오류 코드 (400 등)
- `USER_NOT_FOUND`: 이메일에 해당하는 사용자를 찾을 수 없음
- `OAUTH_USER_CANNOT_RESET_PASSWORD`: 소셜 로그인 계정은 비밀번호 재설정 불가
- `INTERNAL_SERVER_ERROR`: 이메일 발송 실패 등 내부 오류

---

## 2. 비밀번호 재설정 코드 검증

### 엔드포인트
- `POST /api/auth/password/reset/verify-code`

### 요청 바디 (application/json)
| 필드 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| email | string | Y | 이메일 형식 |
| code | string | Y | 6자리 숫자 |

### 처리 흐름
1) Redis에서 코드 조회  
2) 코드 만료 확인 → 만료된 경우 `VERIFICATION_CODE_EXPIRED` 반환  
3) 코드 일치 확인 → 불일치 시 `INVALID_VERIFICATION_CODE` 반환  
4) 검증 성공

### 응답 예시 (성공 200)
```json
{
  "success": true,
  "data": null,
  "message": "인증 코드가 확인되었습니다."
}
```

### 주요 오류 코드 (400 등)
- `VERIFICATION_CODE_EXPIRED`: 인증 코드가 만료되었음 (5분 초과)
- `INVALID_VERIFICATION_CODE`: 인증 코드가 올바르지 않음
- `INTERNAL_SERVER_ERROR`: 내부 오류

---

## 3. 비밀번호 재설정 실행

### 엔드포인트
- `POST /api/auth/password/reset/execute`

### 요청 바디 (application/json)
| 필드 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| email | string | Y | 이메일 형식 |
| code | string | Y | 6자리 숫자 (검증된 코드) |
| newPassword | string | Y | 8~100자, 영문/숫자 포함 |

### 처리 흐름
1) 코드 재검증 (만료/불일치 확인)  
2) 비밀번호 정책 검증 (최소 8자, 영문/숫자 포함)  
3) User 서비스에서 비밀번호 업데이트  
4) 코드 무효화 (사용 완료 처리)  
5) 인증 이벤트 로깅

### 응답 예시 (성공 200)
```json
{
  "success": true,
  "data": null,
  "message": "비밀번호가 성공적으로 재설정되었습니다."
}
```

### 주요 오류 코드 (400 등)
- `VERIFICATION_CODE_EXPIRED`: 인증 코드가 만료되었음
- `INVALID_VERIFICATION_CODE`: 인증 코드가 올바르지 않음
- `INVALID_PASSWORD_FORMAT`: 비밀번호 형식이 올바르지 않음 (8자 이상, 영문/숫자 포함)
- `INTERNAL_SERVER_ERROR`: 내부 오류

---

## 비밀번호 찾기 전체 플로우

1. **코드 요청**: 사용자가 이메일 입력 → `POST /api/auth/password/reset/request-code`
   - 이메일로 6자리 인증 코드 발송 (5분 유효)
   
2. **코드 검증** (선택적): 사용자가 코드 입력 → `POST /api/auth/password/reset/verify-code`
   - 코드 유효성 확인
   
3. **비밀번호 재설정**: 새 비밀번호 입력 → `POST /api/auth/password/reset/execute`
   - 코드 재검증 + 비밀번호 업데이트

## 비고
- 인증 코드는 5분간 유효하며, 재발송 시 이전 코드는 자동으로 무효화됩니다.
- OAuth 로그인 계정(Google 등)은 비밀번호 재설정이 불가능합니다.
- 이메일은 Google SMTP를 통해 발송되며, 기존 이메일 인증과 동일한 스타일을 사용합니다.

---

# 이메일 찾기 API

## 엔드포인트
- `POST /api/users/find-email`

## 요청 바디 (application/json)
| 필드 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| firstName | string | Y | 1~50자 |
| lastName | string | Y | 1~50자 |
| birthDate | string | Y | `yyyy-MM-dd` 형식 |
| signupQuestion | string | Y | 최대 255자 |
| signupAnswer | string | Y | 최대 30자, 첫 글자 공백 불가 (`^(?!\\s).{1,30}$`) |

## 처리 흐름
1) `birthDate`를 `LocalDate`로 파싱  
2) Repository에서 사용자 조회 (firstName, lastName, birthDate, signupQuestion, signupAnswer 모두 일치)  
3) 사용자 없음 → `USER_NOT_FOUND` 반환  
4) 사용자 있음 → 이메일 마스킹 처리 후 반환

## 응답 예시 (성공 200)
```json
{
  "success": true,
  "data": {
    "email": "u***@ex***.com"
  },
  "message": "이메일을 찾았습니다."
}
```

## 주요 오류 코드 (400, 404 등)
- `USER_NOT_FOUND`: 입력하신 정보와 일치하는 사용자를 찾을 수 없음
- `INVALID_REQUEST`: 입력값 검증 실패 (생년월일 형식 오류, 필수 필드 누락 등)
- `INTERNAL_SERVER_ERROR`: 내부 오류

## 이메일 마스킹 규칙
- **로컬 파트** (@ 앞): 첫 글자만 표시, 나머지는 `***`로 마스킹
  - 예: `user` → `u***`
  - 예: `test` → `t***`
  
- **도메인 파트** (@ 뒤): 첫 2글자만 표시, 나머지는 `***`로 마스킹, TLD는 유지
  - 예: `example.com` → `ex***.com`
  - 예: `gmail.com` → `gm***.com`
  - 예: `company.co.kr` → `co***.co.kr`

- **전체 예시**:
  - `user@example.com` → `u***@ex***.com`
  - `test@gmail.com` → `t***@gm***.com`
  - `admin@company.co.kr` → `a***@co***.co.kr`

## 비고
- 보안을 위해 백엔드에서 이메일을 마스킹하여 반환합니다.
- 실제 이메일 주소는 네트워크를 통해 전송되지 않습니다.
- 모든 입력 정보(firstName, lastName, birthDate, signupQuestion, signupAnswer)가 정확히 일치해야 합니다.



