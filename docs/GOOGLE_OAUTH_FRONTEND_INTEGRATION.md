# Google OAuth 2.0 프론트엔드 연동 가이드

## 개요

이 문서는 프론트엔드에서 Google OAuth 2.0 로그인 버튼을 구현하고 백엔드와 연동하는 방법을 설명합니다.

## 사전 준비

### Google OAuth 2.0 Client ID
- Client ID: `899982530420-48m7i8blbgl9cvebb52bp4c44egd3l7t.apps.googleusercontent.com`
- 이 Client ID는 백엔드에서도 사용됩니다.

## 구현 방법

### 1. Google Sign-In JavaScript 라이브러리 추가

HTML 파일의 `<head>` 섹션에 Google Sign-In 스크립트를 추가합니다:

```html
<script src="https://accounts.google.com/gsi/client" async defer></script>
```

### 2. Google 로그인 버튼 추가

로그인 페이지에 Google 로그인 버튼을 추가합니다:

```html
<div id="g_id_onload"
     data-client_id="899982530420-48m7i8blbgl9cvebb52bp4c44egd3l7t.apps.googleusercontent.com"
     data-callback="handleGoogleSignIn">
</div>
<div class="g_id_signin" 
     data-type="standard"
     data-size="large"
     data-theme="outline"
     data-text="sign_in_with"
     data-shape="rectangular"
     data-logo_alignment="left">
</div>
```

### 3. 콜백 함수 구현

Google 로그인 성공 시 호출되는 콜백 함수를 구현합니다:

```javascript
function handleGoogleSignIn(response) {
    // response.credential이 Google ID Token입니다
    const idToken = response.credential;
    
    // 백엔드로 ID Token 전달
    fetch('/api/auth/oauth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            provider: 'google',
            idToken: idToken
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // 로그인 성공
            const { accessToken, refreshToken, user } = data.data;
            
            // 토큰 저장 (localStorage 또는 cookie)
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            
            // 사용자 정보 저장
            localStorage.setItem('user', JSON.stringify(user));
            
            // 메인 페이지로 리디렉션
            window.location.href = '/';
        } else {
            // 로그인 실패
            alert('로그인에 실패했습니다: ' + data.message);
        }
    })
    .catch(error => {
        console.error('로그인 오류:', error);
        alert('로그인 중 오류가 발생했습니다.');
    });
}
```

### 4. React 예제

React를 사용하는 경우:

```tsx
import { useEffect } from 'react';

declare global {
    interface Window {
        google?: {
            accounts: {
                id: {
                    initialize: (config: any) => void;
                    renderButton: (element: HTMLElement, config: any) => void;
                };
            };
        };
    }
}

export default function LoginPage() {
    useEffect(() => {
        // Google Sign-In 초기화
        if (window.google) {
            window.google.accounts.id.initialize({
                client_id: '899982530420-48m7i8blbgl9cvebb52bp4c44egd3l7t.apps.googleusercontent.com',
                callback: handleGoogleSignIn,
            });
            
            // 버튼 렌더링
            window.google.accounts.id.renderButton(
                document.getElementById('google-signin-button'),
                {
                    theme: 'outline',
                    size: 'large',
                    text: 'sign_in_with',
                    shape: 'rectangular',
                }
            );
        }
    }, []);
    
    const handleGoogleSignIn = async (response: any) => {
        const idToken = response.credential;
        
        try {
            const res = await fetch('/api/auth/oauth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    provider: 'google',
                    idToken: idToken
                })
            });
            
            const data = await res.json();
            
            if (data.success) {
                const { accessToken, refreshToken, user } = data.data;
                
                // 토큰 저장
                localStorage.setItem('accessToken', accessToken);
                localStorage.setItem('refreshToken', refreshToken);
                localStorage.setItem('user', JSON.stringify(user));
                
                // 메인 페이지로 이동
                window.location.href = '/';
            } else {
                alert('로그인에 실패했습니다: ' + data.message);
            }
        } catch (error) {
            console.error('로그인 오류:', error);
            alert('로그인 중 오류가 발생했습니다.');
        }
    };
    
    return (
        <div>
            <h1>로그인</h1>
            <div id="google-signin-button"></div>
        </div>
    );
}
```

### 5. 에러 처리

다양한 에러 상황을 처리합니다:

```javascript
function handleGoogleSignIn(response) {
    const idToken = response.credential;
    
    fetch('/api/auth/oauth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            provider: 'google',
            idToken: idToken
        })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.message || '로그인에 실패했습니다.');
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            // 로그인 성공 처리
            const { accessToken, refreshToken, user } = data.data;
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            localStorage.setItem('user', JSON.stringify(user));
            window.location.href = '/';
        } else {
            throw new Error(data.message || '로그인에 실패했습니다.');
        }
    })
    .catch(error => {
        console.error('로그인 오류:', error);
        
        // 에러 메시지 표시
        const errorMessage = error.message || '로그인 중 오류가 발생했습니다.';
        alert(errorMessage);
        
        // 필요시 에러 로깅
        // logError(error);
    });
}
```

## API 엔드포인트

### POST /api/auth/oauth/login

Google OAuth 로그인 요청

**Request Body:**
```json
{
    "provider": "google",
    "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij..."
}
```

**Response (Success):**
```json
{
    "success": true,
    "message": "OAuth 로그인에 성공했습니다.",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer",
        "expiresIn": 3600,
        "user": {
            "id": "123e4567-e89b-12d3-a456-426614174000",
            "email": "user@example.com",
            "firstName": "John",
            "lastName": "Doe",
            "name": "John Doe",
            "picture": "https://lh3.googleusercontent.com/...",
            ...
        }
    }
}
```

**Response (Error):**
```json
{
    "success": false,
    "message": "Google ID Token 검증에 실패했습니다.",
    "code": "AUTH_TOKEN_INVALID"
}
```

## 주의사항

1. **Client ID 보안**
   - Client ID는 공개되어도 안전하지만, Client Secret은 절대 노출하지 마세요.
   - Client Secret은 백엔드에서만 사용됩니다.

2. **ID Token 검증**
   - ID Token은 백엔드에서 검증되므로, 프론트엔드에서는 검증하지 않아도 됩니다.
   - ID Token을 직접 사용하지 말고, 백엔드로 전달하여 JWT 토큰을 받아 사용하세요.

3. **토큰 저장**
   - Access Token과 Refresh Token을 안전하게 저장하세요.
   - XSS 공격을 방지하기 위해 HttpOnly Cookie를 사용하는 것을 권장합니다.

4. **에러 처리**
   - 네트워크 오류, 토큰 검증 실패 등 다양한 에러 상황을 처리하세요.
   - 사용자에게 명확한 에러 메시지를 제공하세요.

## 추가 리소스

- [Google Sign-In JavaScript 라이브러리 문서](https://developers.google.com/identity/gsi/web)
- [Google OAuth 2.0 가이드](https://developers.google.com/identity/protocols/oauth2)

