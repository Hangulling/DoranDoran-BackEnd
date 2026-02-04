import { useEffect, useRef, useState, type JSX, type RefObject } from 'react'
import Send from '../../assets/icon/send.svg'
import ActiveSend from '../../assets/icon/activeSend.svg'
import ErrorIcon from '../../assets/icon/error.svg'
import CheckIcon from '../../assets/icon/checkRound.svg'

interface ChatFooterProps {
  inputRef: RefObject<HTMLTextAreaElement | null>
  onSendMessage: (message: string) => void
  disabled?: boolean
}

type IconType = 'error' | 'checkRound'

type ToastMessageProps = {
  message: string
  iconType: IconType
}

const MAX_ROWS = 3
const LINE_HEIGHT = 21
const SINGLE_LINE_HEIGHT = 37

// 위치 문제로 따로 구현
const ToastMessage = ({ message, iconType }: ToastMessageProps) => {
  const iconMap: Record<IconType, JSX.Element> = {
    error: <img src={ErrorIcon} alt="error" />,
    checkRound: <img src={CheckIcon} alt="check" />,
  }

  return (
    <div className="flex items-start mb-[20px] bg-[rgba(15,16,16,0.8)] px-[14px] py-[16px] rounded-[12px] gap-[8px]">
      {iconType && iconMap[iconType]}
      <span className="text-subtitle text-[14px] text-white">{message}</span>
    </div>
  )
}

const ChatFooter: React.FC<ChatFooterProps> = ({ inputRef, onSendMessage, disabled }) => {
  const [inputValue, setInputValue] = useState('')
  const [textareaHeight, setTextareaHeight] = useState(SINGLE_LINE_HEIGHT)
  const [isComposing, setIsComposing] = useState(false)
  const [toast, setToast] = useState<ToastMessageProps | null>(null)
  const [isToastVisible, setIsToastVisible] = useState(false)
  const toastTimerRef = useRef<NodeJS.Timeout | null>(null)

  // 컴포넌트 언마운트 시 타이머 클리어
  useEffect(() => {
    return () => {
      if (toastTimerRef.current) {
        clearTimeout(toastTimerRef.current)
      }
    }
  }, [])

  const showToast = (message: string, iconType: IconType) => {
    if (toastTimerRef.current) {
      clearTimeout(toastTimerRef.current)
    }

    setToast({ message, iconType })
    setIsToastVisible(true)
    toastTimerRef.current = setTimeout(() => {
      setIsToastVisible(false)

      // 애니메이션이 끝난 후 토스트 데이터 제거
      setTimeout(() => {
        setToast(null)
      }, 400)
    }, 3600)
  }

  const handleCompositionStart = () => setIsComposing(true)

  const handleCompositionEnd = () => {
    setIsComposing(false)
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const originalValue = e.target.value
    let finalValue = originalValue

    if (finalValue.length > 50) {
      showToast('Maximum of 50 characters allowed', 'error')
      finalValue = finalValue.substring(0, 50)
    }

    setInputValue(finalValue)

    const textarea = inputRef.current
    if (textarea) {
      textarea.style.height = 'auto'

      const scrollHeight = textarea.scrollHeight
      const maxHeight = LINE_HEIGHT * MAX_ROWS + (SINGLE_LINE_HEIGHT - LINE_HEIGHT)
      const newHeight = Math.min(scrollHeight, maxHeight)

      textarea.style.height = `${newHeight}px`
      textarea.style.overflowY = scrollHeight > maxHeight ? 'auto' : 'hidden'

      setTextareaHeight(newHeight)
    }
  }

  const handleSendClick = () => {
    // AI가 응답 중이거나 입력값이 없으면 전송 방지
    if (disabled || !inputValue.trim()) {
      return
    }

    onSendMessage(inputValue.trim())
    setInputValue('')
    if (inputRef.current) {
      inputRef.current.style.height = `${SINGLE_LINE_HEIGHT}px`
      inputRef.current.blur()
    }
  }

  // 엔터 키 이벤트
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !isComposing && !disabled) {
      e.preventDefault() // 줄바꿈 방지
      handleSendClick() // 메시지 전송
    }
  }

  const isButtonDisabled = disabled || !inputValue.trim()

  return (
    <div className="relative bg-white shadow-[0_-1px_2px_rgba(0,0,0,0.08)]">
      <div className="absolute bottom-full w-full left-0 flex justify-center">
        {toast && (
          <div
            className={`mx-[20px] ${isToastVisible ? 'toast-slide-fade-in' : 'toast-slide-fade-out'}`}
          >
            <ToastMessage message={toast.message} iconType={toast.iconType} />
          </div>
        )}
      </div>

      <div className="flex items-center w-full px-5 py-2.5">
        <textarea
          ref={inputRef}
          placeholder="Type a message"
          value={inputValue}
          onCompositionStart={handleCompositionStart}
          onCompositionEnd={handleCompositionEnd}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          rows={1}
          style={{
            height: textareaHeight,
            lineHeight: `${LINE_HEIGHT}px`,
            resize: 'none',
          }}
          className="flex-grow px-3 py-2 mr-2 border border-gray-100 bg-gray-50 rounded-[20px] focus:border-gray-100 focus:outline-none [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]"
        />
        <button onClick={handleSendClick} disabled={isButtonDisabled}>
          {!disabled && inputValue.trim() ? (
            <img src={ActiveSend} alt="활성화된 보내기" />
          ) : (
            <img src={Send} alt="보내기" />
          )}
        </button>
      </div>
    </div>
  )
}

export default ChatFooter
