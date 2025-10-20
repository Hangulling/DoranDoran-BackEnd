import React, { useEffect, useRef, useState, type FC } from 'react'
import ChatBubble from './ChatBubble'
import DescriptionBubble from './DescriptionBubble'

interface InitChatProps {
  avatar?: string
  onReady?: () => void
}
const LoadingDot = () => <span className="loading loading-dots loading-[5px] text-gray-200" />

const InitChat: React.FC<InitChatProps> = ({ avatar, onReady }) => {
  const [step, setStep] = useState(0)
  const lastMessageRef = useRef<HTMLDivElement>(null)

  const LOADING_DURATION = 600
  const PAUSE_DURATION = 700

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
    const timers: NodeJS.Timeout[] = []

    const schedule = (callback: () => void, duration: number) => {
      timers.push(setTimeout(callback, duration))
    }

    switch (step) {
      case 0:
        schedule(() => setStep(1), 100)
        break
      case 1: // 첫 번째 버블 로딩 중
        schedule(() => setStep(2), LOADING_DURATION)
        break
      case 2: // 첫 번째 버블 완료
        schedule(() => setStep(3), PAUSE_DURATION)
        break
      case 3: // 두 번째 버블 로딩 중
        schedule(() => setStep(4), LOADING_DURATION)
        break
      case 4: // 두 번째 버블 완료
        schedule(() => setStep(5), PAUSE_DURATION)
        break
      case 5: // 세 번째 버블 로딩 중
        schedule(() => setStep(6), LOADING_DURATION)
        break
      case 6: // 렌더링 완료
        break
    }

    return () => timers.forEach(clearTimeout)
  }, [step])

  useEffect(() => {
    if (step === 6 && lastMessageRef.current) {
      setTimeout(() => {
        console.log('onReady called')
        onReady?.()
      }, 0)
    }
  }, [step, onReady])

  return (
    <div>
      {step === 1 && <LoadingBubble showAvatar={true} />}
      {step >= 2 && (
        <ChatBubble
          message={step === 1 ? <LoadingDot /> : '배고파~ 치맥 먹으러 갈래?'}
          isSender={false}
          avatarUrl={avatar}
          variant="basic"
          showIcon={true}
        />
      )}

      {step === 3 && <LoadingBubble showAvatar={false} />}
      {step >= 4 && (
        <DescriptionBubble
          word="치맥"
          pronunciation="chi-maek"
          descriptionByTab={{
            Kor: '맥주와 치킨을 같이 즐기는 어쩌구',
            Eng: 'A Korean slang term for the popular pairing of fried chicken and beer (maekju).',
          }}
        />
      )}

      {step === 5 && <LoadingBubble showAvatar={false} />}
      {step >= 6 && (
        <div ref={lastMessageRef}>
          <ChatBubble
            message={'Let’s continue the conversation about what kind of chicken you want to eat!'}
            isSender={false}
            variant="second"
          />
        </div>
      )}
    </div>
  )
}

export default InitChat
