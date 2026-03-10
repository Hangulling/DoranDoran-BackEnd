import React, { useEffect, useRef, useState, type FC } from 'react'
import ChatBubble from './ChatBubble'
interface InitChatProps {
  avatar?: string
  onReady?: () => void
  message1: string | null
  message2: string
  skipAnimation?: boolean
}
const LoadingDot = () => <span className="loading loading-dots loading-[5px] text-gray-200" />

const InitChat: React.FC<InitChatProps> = ({
  avatar,
  onReady,
  message1,
  message2,
  skipAnimation = false,
}) => {
  const lastMessageRef = useRef<HTMLDivElement>(null)

  const LOADING_DURATION = 600

  // skipAnimation이 true이면 4, 아니면 1에서 즉시 시작
  const [step, setStep] = useState(skipAnimation || (message1 && message2) ? 4 : 1)

  const LoadingBubble: FC<{ showAvatar?: boolean }> = ({ showAvatar }) => (
    <ChatBubble
      message={<LoadingDot />}
      isSender={false}
      avatarUrl={showAvatar ? avatar : undefined}
      variant={showAvatar ? 'basic' : 'second'}
      showIcon={false}
    />
  )

  useEffect(() => {
    if (skipAnimation) {
      setStep(4)
      return
    }

    if (step >= 4) return

    switch (step) {
      case 1: // 첫 번째 버블 로딩 중
        if (message1) {
          setStep(2) // message1이 도착하면 step 2로 이동
        }
        break

      case 2: // 첫 번째 버블 완료 (message1 렌더링됨)
        setStep(3)
        break

      case 3: // 두 번째 버블 로딩
        break
    }
  }, [step, message1, skipAnimation])

  useEffect(() => {
    if (step === 4) {
      const timer = setTimeout(() => {
        console.log('onReady called')
        onReady?.()
      }, 0)
      return () => clearTimeout(timer)
    }
  }, [step, onReady])

  useEffect(() => {
    let timer: NodeJS.Timeout | null = null
    // step 3일 때만 타이머 작동
    if (step === 3) {
      // 600ms 후에 step 4로 이동
      timer = setTimeout(() => {
        setStep(4)
      }, LOADING_DURATION)
    }
    return () => {
      if (timer) clearTimeout(timer)
    }
  }, [step])

  return (
    <div className="flex flex-col gap-y-2">
      {step >= 1 && (
        <ChatBubble
          // 로딩 단계일 때는 로딩닷 표시
          message={step === 1 || !message1 ? <LoadingDot /> : message1}
          isSender={false}
          avatarUrl={avatar}
          variant="basic"
        />
      )}

      {step === 3 && message2 && <LoadingBubble showAvatar={false} />}

      {step >= 4 && (
        <div ref={lastMessageRef}>
          {message2 && <ChatBubble message={message2} isSender={false} variant="second" />}
        </div>
      )}
    </div>
  )
}

export default InitChat
