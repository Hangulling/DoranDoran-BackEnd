# 관리자(ROLE_ADMIN) 계정 DB 직접 INSERT 가이드

앱은 **bcrypt**로 비밀번호를 해시해 저장합니다. DB에 평문 비밀번호를 넣으면 로그인 시 `passwordEncoder.matches(plain, hash)`에서 일치하지 않습니다.  
**미리 bcrypt 해시를 생성한 뒤, 그 해시를 `password_hash` 컬럼에 넣어야** 로그인할 수 있습니다.

---

## 1. 서버 접속 후 app_user 스키마 확인

SSH 접속 예시 (PowerShell):

```powershell
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" ec2-user@3.21.177.186
```

DB에서 `user_schema.app_user` 구조 확인:

```bash
# RDS 등 DB 접속 정보는 서버의 환경변수 또는 설정에서 확인
psql -h <RDS_HOST> -U doran -d <DB_NAME> -c "\d user_schema.app_user"
```

또는 컬럼만 보고 싶을 때:

```sql
SELECT column_name, data_type, character_maximum_length, column_default, is_nullable
FROM information_schema.columns
WHERE table_schema = 'user_schema' AND table_name = 'app_user'
ORDER BY ordinal_position;
```

현재 스키마 기준 필수·권장 컬럼 요약:

| 컬럼 | 타입 | 비고 |
|------|------|------|
| id | uuid | `gen_random_uuid()` 또는 직접 UUID |
| email | varchar(320) | UNIQUE, NOT NULL |
| first_name | varchar(50) | NOT NULL |
| last_name | varchar(50) | NOT NULL |
| name | varchar(50) | NOT NULL |
| password_hash | varchar(100) | **bcrypt 해시 60자** (여기에 해시만 넣기) |
| role | varchar(20) | **'ROLE_ADMIN'** |
| status | varchar(255) | DEFAULT 'ACTIVE' |
| info | varchar(100) | DEFAULT '' |
| 기타 | … | 나머지는 DEFAULT 있으면 생략 가능 |

---

## 2. bcrypt 해시 생성 (두 가지 방법)

앱은 **Spring Security BCryptPasswordEncoder** (rounds=10, `$2a$10$...`)를 사용합니다.  
아래 둘 중 하나로 만든 해시를 그대로 `password_hash`에 넣으면 됩니다.

### 방법 A: 서버 DB에서 pgcrypto로 생성 (권장)

PostgreSQL `pgcrypto` 확장의 `crypt()` + `gen_salt('bf', 10)`는 Java bcrypt와 호환됩니다.

서버에서 psql로 DB 접속한 뒤:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 아래에서 '원하는비밀번호' 만 바꿔서 실행하면 bcrypt 해시가 한 줄로 나옵니다.
SELECT crypt('원하는비밀번호', gen_salt('bf', 10)) AS password_hash;
```

예시 결과:

```
password_hash
--------------------------------------------------------------
$2a$10$rQnM1.Vx...  (60자 정도의 문자열)
```

이 **한 줄 전체**를 복사해서 아래 INSERT의 `password_hash` 자리에 넣으면 됩니다.

### 방법 B: 로컬에서 Node로 해시 생성

프로젝트 루트(igu)에서:

```bash
npm install -D bcryptjs
node scripts/gen_admin_bcrypt.mjs "원하는비밀번호"
```

출력된 해시를 그대로 INSERT의 `password_hash`에 사용하면 됩니다.

---

## 3. 관리자 계정 INSERT 예시

해시를 얻은 뒤, 아래처럼 INSERT합니다.  
이메일·이름 등은 환경에 맞게 바꾸고, **password_hash에는 위에서 만든 bcrypt 해시 전체**를 넣으세요.

```sql
-- pgcrypto로 해시를 만들었다면, 그 결과를 아래에 그대로 붙여넣기
INSERT INTO user_schema.app_user (
    id,
    email,
    first_name,
    last_name,
    name,
    password_hash,
    role,
    status,
    info
) VALUES (
    gen_random_uuid(),
    'admin@your-domain.com',        -- 관리자 이메일 (중복 불가)
    'Admin',
    'User',
    'Admin User',
    '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',  -- 2번에서 만든 해시로 교체
    'ROLE_ADMIN',
    'ACTIVE',
    ''
);
```

한 번에 해시까지 생성해서 넣고 싶다면 (서버 psql에서):

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO user_schema.app_user (
    id, email, first_name, last_name, name, password_hash, role, status, info
) VALUES (
    gen_random_uuid(),
    'admin@your-domain.com',
    'Admin',
    'User',
    'Admin User',
    crypt('원하는비밀번호', gen_salt('bf', 10)),
    'ROLE_ADMIN',
    'ACTIVE',
    ''
);
```

---

## 4. 확인

- 동일 이메일로 이미 있으면 `app_user_email_idx` UNIQUE 제약으로 INSERT 실패합니다.  
  필요하면 기존 행을 수정하거나 이메일을 바꿔서 다시 INSERT.
- 로그인: Admin 프론트(Koach-Admin 등)에서 위에서 쓴 이메일 + 비밀번호로 로그인하면,  
  `user.role === 'ROLE_ADMIN'`이고 Gateway의 `/api/admin/*` ROLE_ADMIN 검증도 통과합니다.

---

## 5. 요약

- **문제**: DB에 평문 비밀번호를 넣으면 bcrypt 검증에 실패함.
- **해결**:  
  - 서버 DB에서 `pgcrypto`로 `crypt('비밀번호', gen_salt('bf', 10))` 실행해 해시를 만들거나,  
  - 로컬에서 `scripts/gen_admin_bcrypt.mjs`로 해시를 만든 뒤,  
  그 해시를 `user_schema.app_user.password_hash`에 넣고 `role = 'ROLE_ADMIN'`으로 INSERT하면 됩니다.
