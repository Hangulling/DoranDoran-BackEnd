import { type JSX } from 'react'
import { toast } from 'react-hot-toast'
import ErrorIcon from '../../assets/icon/error.svg'
import CheckIcon from '../../assets/icon/checkRound.svg'
import type { ToastProps } from '../../types/common'

export type IconType = 'error' | 'checkRound'

// 에러 / 체크 아이콘 선택
const iconMap: Record<IconType, JSX.Element> = {
  error: <img src={ErrorIcon} alt="error" />,
  checkRound: <img src={CheckIcon} alt="check" />,
}

let toastActive = false

const showToast = ({ message, iconType }: ToastProps) => {
  if (toastActive) return

  toastActive = true

  toast.custom(
    t => (
      <div className="w-full max-w-md">
        <div
          className={`flex items-start mx-5 mb-[20px] bg-[rgba(15,16,16,0.8)] px-[14px] py-[16px] rounded-[12px] gap-[8px]
          ${t.visible ? 'animate-fade-in-up' : 'toast-slide-fade-out'}
        `}
        >
          {iconType && iconMap[iconType]}
          <span className="text-subtitle text-[14px] text-white">{message}</span>
        </div>
      </div>
    ),
    {
      id: `custom-toast-${Date.now()}`, // 중복 방지를 위한 고유 ID
      duration: 4000,
      position: 'bottom-center',
    }
  )
  setTimeout(() => {
    toastActive = false
  }, 4000)
}

export default showToast
