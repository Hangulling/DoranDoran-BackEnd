import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import bubble1 from '../assets/auth/bubble1.svg'
import bubble2 from '../assets/auth/bubble2.svg'
import bubble3 from '../assets/auth/bubble3.svg'
import character from '../assets/auth/character.svg'
import doran from '../assets/auth/doranText.svg'
import Button from '../components/common/Button'
import Input from '../components/common/Input'
import { Link, useNavigate } from 'react-router-dom'
import { login, oauthLogin } from '../api/auth'
import { useUserStore } from '../stores/useUserStore'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../constants/env'
import { GoogleLogin, type CredentialResponse } from '@react-oauth/google'
import GoogleLoginButton from '../components/common/GoogleLoginButton'

type ErrorKind = 'wrong_email' | 'wrong_password' | 'both' | 'general' | null

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<ErrorKind>(null)
  const [errorMsg, setErrorMsg] = useState('')
  const navigate = useNavigate()

  const setStoreId = useUserStore(s => s.setId)
  const setStoreName = useUserStore(s => s.setName)

  const googleLoginContainerRef = useRef<HTMLDivElement | null>(null)

  // 페이지 뷰
  useEffect(() => {
    if (IS_PROD && GA_ENABLED) {
      const yyyyMmDd = new Date().toISOString().slice(0, 10)
      ReactGA.event('view_login', {
        date: yyyyMmDd,
      })
    }
  }, []) // 1회 실행

  const mapAuthError = ({
    status,
    errorCode,
    code,
    message,
  }: {
    status?: number
    errorCode?: string
    code?: string
    message?: string
  }): { type: ErrorKind; msg?: string } => {
    const ec = (errorCode || code || '').toUpperCase()
    const msg = (message || '').toLowerCase()

    const isEmailError =
      status === 404 || ec === 'USER_NOT_FOUND' || /(이메일|존재하지 않|email|not found)/i.test(msg)

    const isPasswordError =
      status === 401 ||
      ec === 'INVALID_PASSWORD' ||
      ec === 'INVALID_CREDENTIALS' ||
      /(비밀번호|password)/i.test(msg)

    if (isEmailError && isPasswordError) return { type: 'both' }
    if (isEmailError) return { type: 'wrong_email' }
    if (isPasswordError) return { type: 'wrong_password' }

    return { type: 'both', msg: 'Email error + Password error' }
  }

  const handleLogin = async () => {
    setError(null)
    setErrorMsg('')

    if (!email && !password) {
      setError('both')
      return
    }
    if (!email) {
      setError('wrong_email')
      return
    }
    if (!password) {
      setError('wrong_password')
      return
    }

    try {
      const res = await login({ email, password })

      if (!res?.success) {
        const mapped = mapAuthError({
          errorCode: (res as { errorCode?: string }).errorCode,
          message: (res as { message?: string }).message,
        })
        setError(mapped.type)
        setErrorMsg(mapped.msg ?? '')

        if (IS_PROD && GA_ENABLED) {
          ReactGA.event('fail_login', {
            user_id: email,
            error_type: mapped.type ?? 'unknown_api_error',
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
            method: email,
          })
        }
        // 온보딩 확인 여부에 따라 라우팅
        if (user.isOnboard) {
          navigate('/')
        } else {
          navigate('/onboarding')
        }
      }
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const status = err.response?.status
        const data = (err.response?.data ?? {}) as {
          errorCode?: string
          code?: string
          message?: string
        }

        if (!status || status >= 500) {
          navigate('/error', {
            replace: true,
            state: { code: status ?? 503, from: 'login' },
          })
          return
        }

        const mapped = mapAuthError({
          status,
          errorCode: data.errorCode,
          code: data.code,
          message: data.message,
        })
        setError(mapped.type)
        setErrorMsg(mapped.msg ?? '')
        console.error('🚨 로그인 에러:', err.response?.data || err)

        if (IS_PROD && GA_ENABLED) {
          ReactGA.event('fail_login', {
            user_id: email,
            error_type: mapped.type ?? 'unknown_catch_error',
          })
        }
      } else {
        navigate('/error', { replace: true, state: { code: 500, from: 'login' } })
      }
    }
  }

  const handleOAuthSuccess = async (credentialResponse: CredentialResponse) => {
    try {
      const idToken = credentialResponse.credential
      if (!idToken) {
        setError('general')
        setErrorMsg('Google 로그인에 실패했습니다.')
        return
      }

      console.log('[Google] idToken typeof:', typeof idToken)

      const res = await oauthLogin({
        provider: 'google',
        idToken,
      })

      console.log('[OAuthLogin] res:', res)

      if (!res?.success) {
        const mapped = mapAuthError({
          errorCode: (res as { errorCode?: string }).errorCode,
          message: (res as { message?: string }).message,
        })
        setError(mapped.type)
        setErrorMsg(mapped.msg ?? '')

        if (IS_PROD && GA_ENABLED) {
          ReactGA.event('fail_login', {
            error_type: mapped.type ?? 'unknown_oauth_error',
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
      if (axios.isAxiosError(err)) {
        const status = err.response?.status
        const data = (err.response?.data ?? {}) as {
          errorCode?: string
          code?: string
          message?: string
        }

        if (!status || status >= 500) {
          navigate('/error', {
            replace: true,
            state: { code: status ?? 503, from: 'oauth_login' },
          })
          return
        }

        const mapped = mapAuthError({
          status,
          errorCode: data.errorCode,
          code: data.code,
          message: data.message,
        })
        setError(mapped.type)
        setErrorMsg(mapped.msg ?? '')
        console.error('🚨 OAuth 로그인 에러:', err.response?.data || err)

        if (IS_PROD && GA_ENABLED) {
          ReactGA.event('fail_login', {
            error_type: mapped.type ?? 'unknown_oauth_catch_error',
            method: 'oauth_google',
          })
        }
      } else {
        navigate('/error', { replace: true, state: { code: 500, from: 'oauth_login' } })
      }
    }
  }

  const handleOAuthError = () => {
    console.error('🚨 Google OAuth 로그인 실패')
    setError('general')
    setErrorMsg('Google 로그인에 실패했습니다.')

    if (IS_PROD && GA_ENABLED) {
      ReactGA.event('fail_login', {
        error_type: 'oauth_google_error',
        method: 'oauth_google',
      })
    }
  }

  const handleCustomGoogleClick = () => {
    const container = googleLoginContainerRef.current
    if (!container) {
      console.error('Google 로그인 컨테이너를 찾을 수 없습니다.')
      return
    }

    const googleButton = container.querySelector('div[role="button"]') as HTMLElement | null
    if (!googleButton) {
      console.error('Google 로그인 버튼을 찾을 수 없습니다.')
      return
    }

    googleButton.click()
  }

  const onKeyDown: React.KeyboardEventHandler<HTMLInputElement> = e => {
    if (e.key === 'Enter') handleLogin()
  }

  return (
    <div className="flex justify-center items-center min-h-screen overflow-y-hidden pt-[38px] pb-[65px]">
      <div className="w-full bg-white rounded-lg">
        <div className="flex flex-col justify-center items-center mb-12">
          <img src={doran} alt="DoranDoran" className="mb-4" />
          <span className="text-gray-700 text-sm text-subtitle">
            Chat your way to real-life Korean
          </span>
        </div>

        <div className="flex justify-center items-center">
          <div className="relative w-[335px] h-[200px] flex justify-center items-end">
            <img
              src={bubble1}
              alt="bubble1"
              className="absolute top-[80px] left-[2px] object-contain w-[112px] h-[33px]
                 z-10 drop-shadow-[0_2px_4px_rgba(0,0,0,0.14)]"
            />

            <img
              src={bubble2}
              alt="bubble2"
              className="absolute top-[22px] left-1/2 -translate-x-1/3 object-contain w-[120px] h-[33px]
                 z-10 drop-shadow-[0_2px_4px_rgba(0,0,0,0.14)]"
            />

            <img
              src={bubble3}
              alt="bubble3"
              className="absolute top-[80px] right-[10px] object-contain w-[102px] h-[33px]
                 z-10 drop-shadow-[0_2px_4px_rgba(0,0,0,0.14)]"
            />

            <img
              src={character}
              alt="characters"
              className="object-contain z-20 translate-y-[10px]"
            />
          </div>
        </div>

        <div className="flex justify-center items-center">
          <div className="mt-4">
            <Input
              type="email"
              variant={error === 'wrong_email' || error === 'both' ? 'error' : 'primary'}
              placeholder="E-mail"
              value={email}
              onChange={e => setEmail(e.target.value)}
              onKeyDown={onKeyDown}
            />
            <Input
              type="password"
              variant={error === 'wrong_password' || error === 'both' ? 'error' : 'primary'}
              placeholder="Password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              onKeyDown={onKeyDown}
            />

            {(error || errorMsg) && (
              <span className="mt-1 block text-xs text-orange-500 text-body">
                {error === 'wrong_email' && 'Email error'}
                {error === 'wrong_password' && 'Password error'}
                {error === 'both' && 'Email error + Password error'}
                {!error && errorMsg && errorMsg}
              </span>
            )}

            <Button
              variant="primary"
              size="xl"
              className="bg-gray-800 my-4 w-full text-subtitle"
              onClick={handleLogin}
            >
              Login
            </Button>

            <div className="relative my-5">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-100"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-white text-gray-300 text-body">You can connect with</span>
              </div>
            </div>

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

            <div className="my-4 w-full">
              <GoogleLoginButton onClick={handleCustomGoogleClick} />
            </div>
          </div>
        </div>

        <div className="flex justify-center items-center gap-2 mb-4 text-sm">
          <span className="text-gray-700 text-body">Don't have an account yet?</span>
          <Link to="/signup" className="underline underline-offset-4 text-title text-gray-800">
            Sign up
          </Link>
        </div>
      </div>
    </div>
  )
}
