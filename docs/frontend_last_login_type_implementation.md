# 마지막 로그인 방식 표시 기능 구현 가이드

## 개요
사용자가 마지막으로 사용한 로그인 방식을 localStorage에 저장하고, 로그인 페이지에서 이를 읽어와 UI에 반영하는 기능입니다. 서버와의 통신 없이 클라이언트 측에서만 처리합니다.

## API 엔드포인트 정보

### 1. 이메일 로그인
- **엔드포인트**: `POST /api/auth/login`
- **요청 바디**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **성공 응답 (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600,
      "user": {
        "id": "uuid-string",
        "email": "user@example.com",
        ...
      }
    },
    "message": "로그인에 성공했습니다."
  }
  ```

### 2. OAuth 로그인 (Google)
- **엔드포인트**: `POST /api/auth/oauth/login`
- **요청 바디**:
  ```json
  {
    "provider": "GOOGLE",
    "idToken": "google-id-token-string"
  }
  ```
- **성공 응답 (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600,
      "user": {
        "id": "uuid-string",
        "email": "user@gmail.com",
        ...
      }
    },
    "message": "OAuth 로그인에 성공했습니다."
  }
  ```

## 구현 단계

### STEP 1: 로그인 성공 시 localStorage에 저장

#### 1-1. 이메일 로그인 성공 시
로그인 API 호출 후 응답이 성공(200 OK)이고 `success: true`인 경우:

```javascript
// 예시: 로그인 API 호출 함수
async function handleEmailLogin(email, password) {
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });
    
    const result = await response.json();
    
    if (result.success && response.status === 200) {
      // 로그인 성공 시 localStorage에 저장
      localStorage.setItem('LAST_LOGIN_TYPE', 'EMAIL');
      
      // 토큰 저장 및 리다이렉트 등 후속 처리
      // ...
    }
  } catch (error) {
    console.error('로그인 실패:', error);
  }
}
```

#### 1-2. OAuth 로그인 (Google) 성공 시
OAuth 로그인 API 호출 후 응답이 성공(200 OK)이고 `success: true`인 경우:

```javascript
// 예시: Google OAuth 로그인 함수
async function handleGoogleLogin(idToken) {
  try {
    const response = await fetch('/api/auth/oauth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        provider: 'GOOGLE',
        idToken: idToken,
      }),
    });
    
    const result = await response.json();
    
    if (result.success && response.status === 200) {
      // OAuth 로그인 성공 시 localStorage에 저장
      localStorage.setItem('LAST_LOGIN_TYPE', 'GOOGLE');
      
      // 토큰 저장 및 리다이렉트 등 후속 처리
      // ...
    }
  } catch (error) {
    console.error('OAuth 로그인 실패:', error);
  }
}
```

#### 1-3. 로그아웃 시 처리 (선택사항)
로그아웃 시 localStorage 값을 삭제할지 유지할지는 UX 요구사항에 따라 결정합니다.

**옵션 A: 로그아웃 시 삭제**
```javascript
async function handleLogout() {
  // 로그아웃 API 호출
  // ...
  
  // localStorage에서 삭제
  localStorage.removeItem('LAST_LOGIN_TYPE');
}
```

**옵션 B: 로그아웃 시 유지 (권장)**
- 사용자가 다음 로그인 시에도 마지막 방식을 표시
- localStorage 값은 유지

### STEP 2: 로그인 페이지 렌더링 시 UI 반영

로그인 페이지(`/login`) 컴포넌트가 마운트될 때 localStorage 값을 읽어와 UI를 변경합니다.

#### 2-1. React 예시

```tsx
import { useEffect, useState } from 'react';

function LoginPage() {
  const [lastLoginType, setLastLoginType] = useState<string | null>(null);
  
  useEffect(() => {
    // 페이지 로드 시 localStorage에서 읽어오기
    const lastMethod = localStorage.getItem('LAST_LOGIN_TYPE');
    setLastLoginType(lastMethod);
  }, []);
  
  return (
    <div className="login-container">
      {/* Google 로그인 버튼 */}
      <div className="google-login-section">
        <button onClick={handleGoogleLogin} className="google-login-btn">
          Google로 로그인
          {lastLoginType === 'GOOGLE' && (
            <span className="badge">최근 이용</span>
          )}
        </button>
      </div>
      
      {/* 이메일 로그인 섹션 */}
      <div className="email-login-section">
        {lastLoginType === 'EMAIL' && (
          <p className="info-text">이전에 이메일로 로그인하셨습니다.</p>
        )}
        <input type="email" placeholder="이메일" />
        <input type="password" placeholder="비밀번호" />
        <button onClick={handleEmailLogin}>이메일로 로그인</button>
      </div>
    </div>
  );
}
```

#### 2-2. Vue 예시

```vue
<template>
  <div class="login-container">
    <!-- Google 로그인 버튼 -->
    <div class="google-login-section">
      <button @click="handleGoogleLogin" class="google-login-btn">
        Google로 로그인
        <span v-if="lastLoginType === 'GOOGLE'" class="badge">최근 이용</span>
      </button>
    </div>
    
    <!-- 이메일 로그인 섹션 -->
    <div class="email-login-section">
      <p v-if="lastLoginType === 'EMAIL'" class="info-text">
        이전에 이메일로 로그인하셨습니다.
      </p>
      <input type="email" placeholder="이메일" v-model="email" />
      <input type="password" placeholder="비밀번호" v-model="password" />
      <button @click="handleEmailLogin">이메일로 로그인</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const lastLoginType = ref<string | null>(null);
const email = ref('');
const password = ref('');

onMounted(() => {
  // 페이지 로드 시 localStorage에서 읽어오기
  lastLoginType.value = localStorage.getItem('LAST_LOGIN_TYPE');
});
</script>
```

#### 2-3. 순수 JavaScript 예시

```javascript
// 로그인 페이지 로드 시
document.addEventListener('DOMContentLoaded', function() {
  const lastMethod = localStorage.getItem('LAST_LOGIN_TYPE');
  
  if (lastMethod === 'GOOGLE') {
    // Google 버튼 옆에 "최근 이용" 뱃지 추가
    const googleButton = document.querySelector('.google-login-btn');
    if (googleButton) {
      const badge = document.createElement('span');
      badge.className = 'badge';
      badge.textContent = '최근 이용';
      googleButton.appendChild(badge);
    }
  } else if (lastMethod === 'EMAIL') {
    // 이메일 입력창 근처에 안내 문구 추가
    const emailSection = document.querySelector('.email-login-section');
    if (emailSection) {
      const infoText = document.createElement('p');
      infoText.className = 'info-text';
      infoText.textContent = '이전에 이메일로 로그인하셨습니다.';
      emailSection.insertBefore(infoText, emailSection.firstChild);
    }
  }
});
```

## UI 디자인 가이드

### 뱃지 스타일 예시 (CSS)

```css
.badge {
  display: inline-block;
  padding: 2px 8px;
  margin-left: 8px;
  background-color: #54bdb4;
  color: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.info-text {
  color: #666;
  font-size: 14px;
  margin-bottom: 16px;
  padding: 8px 12px;
  background-color: #f5f5f5;
  border-radius: 4px;
}
```

## 저장 값 상수 정의 (권장)

코드의 일관성을 위해 상수로 정의하는 것을 권장합니다:

```typescript
// constants/auth.ts
export const LAST_LOGIN_TYPE = {
  EMAIL: 'EMAIL',
  GOOGLE: 'GOOGLE',
} as const;

export type LastLoginType = typeof LAST_LOGIN_TYPE[keyof typeof LAST_LOGIN_TYPE];
```

사용 예시:
```typescript
import { LAST_LOGIN_TYPE } from '@/constants/auth';

// 저장
localStorage.setItem('LAST_LOGIN_TYPE', LAST_LOGIN_TYPE.EMAIL);

// 읽기
const lastMethod = localStorage.getItem('LAST_LOGIN_TYPE');
if (lastMethod === LAST_LOGIN_TYPE.GOOGLE) {
  // ...
}
```

## 주의사항

1. **localStorage는 브라우저별로 독립적**
   - 사용자가 다른 브라우저에서 로그인하면 값이 다를 수 있습니다.

2. **시크릿 모드/프라이빗 브라우징**
   - 일부 브라우저에서는 시크릿 모드 종료 시 localStorage가 삭제될 수 있습니다.

3. **값이 없는 경우**
   - 첫 방문자이거나 localStorage가 비어있는 경우 `null`을 반환하므로, 이 경우 기본 UI를 표시합니다.

4. **타입 안정성 (TypeScript)**
   - TypeScript를 사용하는 경우, 저장 가능한 값에 대한 타입을 정의하는 것을 권장합니다.

## 테스트 시나리오

1. **이메일 로그인 → 페이지 새로고침**
   - 이메일 로그인 성공 후 localStorage에 'EMAIL' 저장 확인
   - 로그인 페이지 접속 시 이메일 섹션에 안내 문구 표시 확인

2. **Google 로그인 → 페이지 새로고침**
   - Google 로그인 성공 후 localStorage에 'GOOGLE' 저장 확인
   - 로그인 페이지 접속 시 Google 버튼에 "최근 이용" 뱃지 표시 확인

3. **로그인 방식 변경**
   - 이메일로 로그인 → 로그아웃 → Google로 로그인
   - localStorage 값이 'GOOGLE'로 업데이트되는지 확인

4. **localStorage 비어있는 경우**
   - localStorage를 삭제한 후 로그인 페이지 접속
   - 기본 UI가 표시되는지 확인

