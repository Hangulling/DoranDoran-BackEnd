d# OAuth 회원가입 필요(needSignup) 프론트엔드 구현 가이드

## 개요

OAuth 로그인 시 신규 사용자(404)인 경우, 백엔드가 `needSignup: true` + `oauthUserInfo`를 반환합니다.  
프론트는 이 정보로 회원가입 폼을 pre-fill하고, 사용자 동의 후 `confirmSignup: true`로 재호출해 가입을 완료합니다.

---

## 1. 타입 정의 업데이트

`src/types/auth.ts`:

```typescript
export interface OAuthUserInfo {
  email: string
  firstName: string
  lastName: string
  name: string
  picture: string | null
  provider: string  // GOOGLE | APPLE | FIREBASE
}

export interface OAuthLoginRequest {
  provider: string
  idToken: string
  /** 신규 사용자일 때 회원가입 확정 여부. true면 즉시 가입 후 토큰 반환 */
  confirmSignup?: boolean
}

export interface LoginResponseData {
  accessToken?: string
  refreshToken?: string
  tokenType?: 'Bearer'
  expiresIn?: number
  user?: User
  /** 회원가입 필요 시 true. 이때 oauthUserInfo로 폼 pre-fill */
  needSignup?: boolean
  oauthUserInfo?: OAuthUserInfo
}

export interface LoginResponse {
  success: boolean
  message: string
  data: LoginResponseData
}

export type OAuthLoginResponse = LoginResponse
```

---

## 2. API 레이어 업데이트

`src/api/auth.ts`의 `oauthLogin`:

```typescript
export async function oauthLogin(data: OAuthLoginRequest) {
  const res = await publicApi.post<OAuthLoginResponse>(AUTH_ENDPOINTS.OAUTH_LOGIN, data)
  const { data: resData } = res.data

  // needSignup일 때는 토큰 저장 안 함
  if (!resData.needSignup && resData.accessToken) {
    sessionStorage.setItem('accessToken', resData.accessToken)
  }
  if (!resData.needSignup && resData.refreshToken) {
    sessionStorage.setItem('refreshToken', resData.refreshToken)
  }

  return res.data
}
```

---

## 3. 구현 방식 선택

### 방식 A: 별도 OAuth 회원가입 페이지 (권장)

- **경로**: `/signup/oauth` 또는 `/oauth-signup`
- **진입**: `needSignup` 응답 시 `navigate('/signup/oauth', { state: { oauthUserInfo, idToken, provider } })`
- **장점**: 폼·약관 UI를 독립적으로 구성 가능

### 방식 B: SignupPage 확장

- **경로**: 기존 `/signup`
- **진입**: `navigate('/signup', { state: { fromOAuth: true, oauthUserInfo, idToken, provider } })`
- **장점**: 기존 SignupPage의 약관·레이아웃 재사용

### 방식 C: LoginPage 내 모달

- **장점**: 화면 전환 없이 처리
- **단점**: idToken 유효 시간(보통 10분) 내 모달 완료 필요

---

## 4. 로그인 흐름 (LoginPage)

### 4.1 Google 로그인 핸들러 수정

```typescript
// LoginPage.tsx - handleOAuthSuccess 내부
const handleOAuthSuccess = async (credentialResponse: CredentialResponse) => {
  try {
    const idToken = credentialResponse.credential
    if (!idToken) {
      setErrorMsg('Google 로그인에 실패했습니다.')
      return
    }

    const res = await oauthLogin({
      provider: 'google',
      idToken,
      // confirmSignup 생략 = false
    })

    if (!res?.success) {
      // 기존 에러 처리
      return
    }

    const { needSignup, oauthUserInfo, user, accessToken } = res.data ?? {}

    // 신규 사용자 → 회원가입 폼으로 이동
    if (needSignup && oauthUserInfo) {
      navigate('/signup/oauth', {
        replace: true,
        state: {
          oauthUserInfo,
          idToken,
          provider: 'google',
        },
      })
      return
    }

    // 기존 사용자 → 로그인 완료
    if (user && accessToken) {
      setStoreId(user.id)
      setStoreName(user.name)
      // GA 등 ...
      navigate(user.isOnboard ? '/' : '/onboarding')
    }
  } catch (err) {
    // 기존 catch 로직
  }
}
```

### 4.2 Apple 로그인 (React Native/Expo 등)

동일하게 `needSignup`/`oauthUserInfo`를 확인한 뒤, 앱의 Sign up 화면으로 이동하면 됩니다.

---

## 5. OAuth 회원가입 페이지 예시

`src/pages/OAuthSignupPage.tsx` (신규 생성):

```tsx
import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { oauthLogin } from '../api/auth'
import { useUserStore } from '../stores/useUserStore'
import Agreement from '../components/common/Agreement'
import Button from '../components/common/Button'
import type { OAuthUserInfo } from '../types/auth'

interface LocationState {
  oauthUserInfo: OAuthUserInfo
  idToken: string
  provider: string
}

export default function OAuthSignupPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const setStoreId = useUserStore(s => s.setId)
  const setStoreName = useUserStore(s => s.setName)

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const agreements = useAgreementStore(s => s.value)

  const oauthState = state as LocationState | null
  const { oauthUserInfo, idToken, provider } = oauthState ?? {}

  // state 없이 직접 진입 시 로그인으로 복귀
  useEffect(() => {
    if (!oauthUserInfo || !idToken || !provider) {
      navigate('/login', { replace: true })
    }
  }, [oauthUserInfo, idToken, provider, navigate])

  const allAgreed = agreements?.terms && agreements?.privacy // 실제 약관 필드에 맞게 수정

  const handleSubmit = async () => {
    if (!allAgreed || !oauthUserInfo || !idToken || !provider) return

    setLoading(true)
    setError('')

    try {
      const res = await oauthLogin({
        provider,
        idToken,
        confirmSignup: true,
        // birthDate: birthDate,  // 선택. 폼에 birthDate 입력 필드 추가 시 전달 (yyyy-MM-dd)
      })

      if (!res?.success) {
        setError(res?.message ?? '회원가입에 실패했습니다.')
        return
      }

      const user = res.data?.user
      if (user) {
        setStoreId(user.id)
        setStoreName(user.name)
        navigate(user.isOnboard ? '/' : '/onboarding', { replace: true })
      }
    } catch (err: unknown) {
      setError('회원가입 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  if (!oauthState) return null

  return (
    <div className="...">
      <h1>회원가입</h1>
      <p>아래 정보로 가입합니다.</p>
      <div>
        <span>이메일: {oauthUserInfo.email}</span>
        <span>이름: {oauthUserInfo.firstName} {oauthUserInfo.lastName}</span>
      </div>
      {/* birthDate 입력 필드 추가 시, state로 관리하여 confirmSignup 호출 시 전달 */}

      <Agreement />

      {error && <p className="text-red-500">{error}</p>}
      <Button
        disabled={!allAgreed || loading}
        onClick={handleSubmit}
      >
        {loading ? '가입 중...' : '가입하기'}
      </Button>
    </div>
  )
}
```

라우트에 추가:

```tsx
// routes.tsx
<Route path="/signup/oauth" element={<OAuthSignupPage />} />
```

---

## 6. 주의사항

1. **idToken 유효 시간**: Apple/Google idToken은 보통 10분 내 만료. 이 시간 안에 회원가입을 완료해야 합니다.
2. **직접 진입 방지**: `/signup/oauth`를 state 없이 열면 `/login`으로 리다이렉트.
3. **약관 동의**: `useAgreementStore` 등 실제 약관 상태와 연동해 `allAgreed` 검사.
4. **생년월일(birthDate)**: OAuth 토큰에 birthdate가 없으므로, 회원가입 폼에 birthDate 입력 필드를 추가하고 `confirmSignup` 시 `birthDate`(yyyy-MM-dd)를 함께 전달하면 DB에 저장됩니다.
5. **모바일 앱**: React Native 등에서는 `needSignup` 수신 시 해당 화면으로 navigate하고, `confirmSignup: true`로 동일 API 재호출하면 됩니다.

---

## 7. 요약 흐름

```
[사용자] Apple/Google 로그인 클릭
    ↓
[프론트] oauthLogin({ provider, idToken })
    ↓
[백엔드] 사용자 없음 → needSignup: true, oauthUserInfo 반환
    ↓
[프론트] needSignup 확인 → /signup/oauth로 이동 (state에 oauthUserInfo, idToken, provider)
    ↓
[사용자] 약관 동의, "가입하기" 클릭
    ↓
[프론트] oauthLogin({ provider, idToken, confirmSignup: true, birthDate? })
    ↓
[백엔드] 회원 생성 후 토큰 반환
    ↓
[프론트] 토큰 저장, 메인/온보딩으로 이동
```
