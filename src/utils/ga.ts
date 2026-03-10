import ReactGA from 'react-ga4'

const GA_ENABLED = import.meta.env.VITE_GA_ENABLED === 'true'

/**
 * GA 커스텀 이벤트를 전송하는 헬퍼 함수
 * @param eventName - 이벤트 이름 (PM과 협의한 이름, 예: 'login')
 * @param params - (선택) 이벤트와 함께 보낼 추가 정보 (예: { method: 'google' })
 */
export const sendGAEvent = (
  eventName: string,
  params?: Record<string, string | number | boolean | undefined | null>
) => {
  // 운영(production) 환경이고 GA가 활성화되었을 때만 전송
  if (import.meta.env.PROD && GA_ENABLED) {
    ReactGA.event(eventName, params)
  }
}
