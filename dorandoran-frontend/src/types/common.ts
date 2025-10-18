import type { IconType } from '../components/common/CommonToast'

// 네비 바
export interface NavBarProps {
  title?: string
  isMain?: boolean
  showBookmark?: boolean
  showDelete?: boolean
}

// 사이드 바
export interface SidebarProps {
  isOpen: boolean
  onClose: () => void
}

// 공통 모달
export interface CommonModalProps {
  open: boolean
  title: string
  variant?: 'common' | 'signup'
  description?: string | string[] // 상수 지정시 배열
  confirmText?: string
  cancelText?: string
  onConfirm: () => void
  onCancel: () => void
  className?: string
}

// 공통 토스트
export interface ToastProps {
  message: string
  iconType?: IconType
}

// 로딩 스피너
export interface LoadingSpinnerProps {
  message?: string
}
