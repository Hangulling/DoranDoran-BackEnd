# 회원가입 API 명세 (igu 기준)

## 1. 개요
- **서비스**: User Service
- **Gateway Base URL**: `http://localhost:8080`
- **Direct Base URL**: `http://localhost:8082`
- **요청 Content-Type**: `application/json`

본 문서는 `igu` 워크트리의 실제 코드(`UserController`, `UserService`, `CreateUserRequest`) 기준으로 작성되었습니다.

---

## 2. 엔드포인트

### 2.1 회원가입(사용자 생성)
`POST /api/users`

일반 회원가입(이메일+비밀번호) 사용자 생성 API입니다.

#### 요청 바디 (CreateUserRequest)
```json
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "name": "John Doe",
  "password": "password1234",
  "picture": "https://example.com/profile.jpg",
  "info": "소개글",
  "birthDate": "1990-01-01",
  "signupQuestion": "내가 가장 좋아하는 색은?",
  "signupAnswer": "파랑",
  "marketingOptIn": true
}
```

#### 필드 정의/제약
- **email**: 필수, 이메일 형식
- **firstName**: 필수, 1~50자
- **lastName**: 필수, 1~50자
- **name**: 필수, 1~50자 (비어있으면 서버 내부에서 `firstName + lastName`으로 보정될 수 있음)
- **password**: 필수, 8~100자
  - 서버 추가 정책: **영문 최소 1자 + 숫자 최소 1자** 포함
- **picture**: 선택
- **info**: 선택, 최대 100자
- **birthDate**: 필수, `yyyy-MM-dd`
- **signupQuestion**: 필수, 최대 255자
- **signupAnswer**: 필수, 최대 30자
  - 첫 글자 공백 불가
- **marketingOptIn**: 선택(Boolean)
  - 회원가입 시 “마케팅 수신 동의(선택 약관)” 값 전달 용도
  - `null`이면 `false`로 저장

#### 처리 규칙(서버 로직)
- **이메일 인증 완료 필수**
  - 이메일 인증이 완료되지 않으면 실패: `"이메일 인증을 먼저 완료해주세요."`
- **탈퇴 후 30일 재가입 제한**
  - 동일 이메일의 `INACTIVE` 사용자가 있고, 탈퇴 시각(`inactiveAt`) 기준 30일이 지나지 않았으면 실패

#### 성공 응답
`200 OK`

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "name": "John Doe",
    "picture": "https://example.com/profile.jpg",
    "info": "소개글",
    "birthDate": "1990-01-01",
    "signupQuestion": "내가 가장 좋아하는 색은?",
    "signupAnswer": "파랑",
    "status": "ACTIVE",
    "role": "ROLE_USER",
    "createdAt": "2026-01-25T12:00:00",
    "updatedAt": "2026-01-25T12:00:00"
  },
  "message": "사용자가 성공적으로 생성되었습니다.",
  "errorCode": null,
  "timestamp": "2026-01-25T12:00:00"
}
```

#### 실패 응답(대표 케이스)
`400 Bad Request`

- 이메일 중복(활성 사용자 존재)
  - `errorCode`: `EMAIL_ALREADY_EXISTS`
- 이메일 인증 미완료
  - `errorCode`: `INVALID_REQUEST`
  - `message`: `"이메일 인증을 먼저 완료해주세요."`
- 비밀번호 정책 위반
  - `errorCode`: `INVALID_REQUEST`
  - `message`: `"비밀번호는 최소 8자 이상이어야 합니다."` 또는 `"비밀번호에는 영문과 숫자가 각각 최소 1자 포함되어야 합니다."`
- 탈퇴 후 30일 미경과 재가입
  - `errorCode`: `INVALID_REQUEST`
  - `message`: `"탈퇴 후 30일이 지나야 재가입할 수 있습니다. (남은 기간: N일)"`

---

### 2.2 회원가입(별도 엔드포인트)
`POST /api/users/register`

`POST /api/users`와 동일한 요청/동작으로 사용자 생성 후,
성공 메시지만 `"회원가입이 성공적으로 완료되었습니다."`로 반환합니다.

---

## 3. 약관동의(현재 구현 범위)
현 시점 `igu` 기준으로 회원가입 요청에 포함되는 “약관동의” 관련 필드는 아래 1개입니다.

- **marketingOptIn**: 마케팅 수신 동의(선택 약관)

그 외 필수 약관(서비스 이용약관/개인정보 처리방침 등) 동의값을 회원가입 요청에 포함해 저장/검증하는 구조는 코드상 존재하지 않습니다.

