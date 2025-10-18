import { useEffect, useRef } from 'react'
import { getSseUrl } from '../api'

const eventNames = [
  'intimacy_analysis', // 친밀도 분석 결과
  'vocabulary_extracted', // 어휘 추출 결과
  'vocabulary_translated', // 번역 결과
  'conversation_chunk', // 대화 응답 스트림
  'conversation_complete', // 대화 완료
  'aggregated_complete', // 전체 결과 집계
]

export function useChatStream<T = unknown>(
  chatroomId: string,
  userId?: string,
  onEventReceived?: (eventType: string, data: T) => void,
  onError?: (event: Event) => void
) {
  const eventSourceRef = useRef<EventSource | null>(null)

  useEffect(() => {
    if (!chatroomId) return

    const sseUrl = getSseUrl(chatroomId, userId)
    // 서버와 연결 시작
    const eventSource = new EventSource(sseUrl)

    // 기본 메시지 이벤트 처리
    eventSource.onmessage = event => {
      if (onEventReceived) {
        onEventReceived('message', JSON.parse(event.data))
      }
    }

    // 배열에 있는 이벤트들 이름 구독, 이벤트 발생 시 콜백
    eventNames.forEach(name => {
      eventSource.addEventListener(name, (event: MessageEvent) => {
        if (onEventReceived) {
          onEventReceived(name, JSON.parse(event.data))
        }
      })
    })

    if (onError) {
      eventSource.onerror = onError
    }

    eventSourceRef.current = eventSource

    // 채팅방 나갈때 자동으로 끊기
    return () => {
      eventSource.close()
    }
  }, [chatroomId, userId, onEventReceived, onError])

  return eventSourceRef.current
}
