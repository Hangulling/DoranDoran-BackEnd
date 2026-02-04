import { useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { GoogleLogin, type CredentialResponse } from '@react-oauth/google'
import { oauthLogin } from '../../api/auth'
import { useUserStore } from '../../stores/useUserStore'
import { GA_ENABLED, IS_PROD } from '../../constants/env'
import GoogleLoginButton from '../common/GoogleLoginButton'
import axios from 'axios'
import ReactGA from 'react-ga4'

const GoogleAuth = () => {
  const navigate = useNavigate()
  const setStoreId = useUserStore(s => s.setId)
  const setStoreName = useUserStore(s => s.setName)

  // 숨겨진 구글 버튼을 가리키는 Ref
  const googleLoginContainerRef = useRef<HTMLDivElement | null>(null)

  // 구글 로그인 성공
  const handleOAuthSuccess = async (credentialResponse: CredentialResponse) => {
    try {
      const idToken = credentialResponse.credential
      if (!idToken) {
        alert('Google 로그인에 실패했습니다. (No ID Token)')
        return
      }

      const res = await oauthLogin({
        provider: 'google',
        idToken,
      })

      if (!res?.success) {
        console.error('서버 로그인 실패:', res)
        alert('로그인 처리에 실패했습니다.')

        if (IS_PROD && GA_ENABLED) {
          ReactGA.event('fail_login', {
            error_type: (res as { errorCode?: string }).errorCode ?? 'unknown_oauth_error',
            method: 'oauth_google',
          })
        }
        return
      }

      const user = res?.data?.user
      if (user) {
        setStoreId(user.id)
        setStoreName(user.name)

        if (IS_PROD && GA_ENABLED) {
          ReactGA.set({ user_id: user.id })
          const yyyyMmDd = new Date().toISOString().slice(0, 10)
          ReactGA.event('login', {
            date: yyyyMmDd,
            method: 'oauth_google',
          })
        }
        if (user.isOnboard) {
          navigate('/')
        } else {
          navigate('/onboarding')
        }
      }
    } catch (err: unknown) {
      console.error('OAuth Error:', err)
      alert('구글 로그인 중 오류가 발생했습니다.')

      if (axios.isAxiosError(err) && IS_PROD && GA_ENABLED) {
        ReactGA.event('fail_login', {
          error_type: 'unknown_oauth_catch_error',
          method: 'oauth_google',
        })
      }
    }
  }

  // 구글 로그인 에러
  const handleOAuthError = () => {
    console.error('🚨 Google OAuth 로그인 실패')
    alert('Google 로그인 연결에 실패했습니다.')
  }

  // 숨겨진 구글 버튼 클릭 트리거
  const handleCustomGoogleClick = (e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation()
    }

    const container = googleLoginContainerRef.current
    if (!container) return

    const googleButton = container.querySelector('div[role="button"]') as HTMLElement | null
    if (googleButton) googleButton.click()
  }

  return (
    <>
      <div ref={googleLoginContainerRef} className="hidden">
        <GoogleLogin
          onSuccess={handleOAuthSuccess}
          onError={handleOAuthError}
          useOneTap={false}
          theme="outline"
          size="large"
          text="signup_with"
          shape="rectangular"
          width="100%"
          locale="en"
        />
      </div>

      <div className="w-full">
        <GoogleLoginButton onClick={handleCustomGoogleClick} />
      </div>
    </>
  )
}

export default GoogleAuth
