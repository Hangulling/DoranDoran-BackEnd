const IDLE_TIMEOUT_MS = 59 * 60 * 1000 // 59분
// 사용자의 활동을 감지하는 이벤트 목록
const events = ['keydown', 'click', 'scroll', 'touchstart']

let idleTimer: NodeJS.Timeout | null = null

function resetIdleTimer() {
  // 기존 타이머가 있으면 취소
  if (idleTimer) {
    clearTimeout(idleTimer)
  }

  // (디버깅용) 타이머가 (재)설정될 때 로그
  // console.log(`비활성 타이머가 ${IDLE_TIMEOUT_MS / 1000}초로 재설정`)

  idleTimer = setTimeout(() => {
    console.warn(`auth:inactive 이벤트를 발생.`)

    // 세션 만료 모달을 띄우는 이벤트를 발생시킴
    window.dispatchEvent(new CustomEvent('auth:inactive', { detail: { reason: 'user_idle' } }))
  }, IDLE_TIMEOUT_MS)
}

export function stopIdleTimer() {
  if (idleTimer) {
    clearTimeout(idleTimer)
    idleTimer = null
  }
  events.forEach(event => {
    window.removeEventListener(event, resetIdleTimer, { capture: true })
  })
  //  console.log('비활성 타이머 중지')
}

export function startIdleTimer() {
  stopIdleTimer()

  // 사용자가 어떤 활동이든 하면, 타이머를 리셋
  events.forEach(event => {
    window.addEventListener(event, resetIdleTimer, { capture: true, passive: true })
  })

  // 타이머 최초 실행
  resetIdleTimer()
  console.log('비활성 타이머 시작')
}
