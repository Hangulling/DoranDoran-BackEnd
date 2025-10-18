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

  const toastId = toast.custom(
    <div className="flex items-start w-full mx-[20px] mb-[20px] bg-[rgba(15,16,16,0.8)] px-[14px] py-[16px] rounded-[12px] gap-[8px]">
      {iconType && iconMap[iconType]}
      <span className="text-subtitle text-[14px] text-white">{message}</span>
    </div>,
    {
      duration: 4000,
      position: 'bottom-center',
    }
  )
  setTimeout(() => {
    toastActive = false
    toast.dismiss(toastId)
  }, 4000)
}

export default showToast
