import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSignupFormStore } from '../stores/useSignupStore'
import { parseEmailVerifiedUrl } from '../utils/emailVerifiedDeepLink'

/**
 * Capacitor 앱에서 dorandoran://email-verified 딥링크 수신 시
 * 회원가입 스토어에 인증정보 반영 후 /signup으로 이동.
 * 웹 빌드 또는 Capacitor 미설치 시에는 아무 동작 안 함.
 */
export function useEmailVerifiedDeepLink() {
  const navigate = useNavigate()

  useEffect(() => {
    let listener: { remove: () => Promise<void> } | null = null

    const setup = async () => {
      try {
        const [{ Capacitor }, { App }] = await Promise.all([
          import('@capacitor/core'),
          import('@capacitor/app'),
        ])
        if (!Capacitor.isNativePlatform()) return

        listener = await App.addListener('appUrlOpen', (ev) => {
          const payload = parseEmailVerifiedUrl(ev.url)
          if (!payload) return

          const { setMany } = useSignupFormStore.getState()
          setMany({
            email: payload.email,
            firstName: payload.firstName ?? '',
            lastName: payload.lastName ?? '',
            verifiedEmail: payload.verified ? payload.email : null,
            emailVerified: payload.verified,
          })
          navigate('/signup', { replace: true })
        })
      } catch {
        // @capacitor/* 미설치(웹 전용 빌드) 시 무시
      }
    }

    setup()
    return () => {
      listener?.remove().catch(() => {})
    }
  }, [navigate])
}
