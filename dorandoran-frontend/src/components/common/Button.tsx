import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'cancel' | 'text' | 'confirm' | 'tab' | 'archive' | 'home'
type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'archive' | 'confirm'

const VARIANTS: Record<Variant, string> = {
  primary: 'text-white rounded-lg disabled:bg-gray-100 disabled:text-gray-300',
  cancel: 'bg-gray-80 text-gray-600 rounded-lg',
  text: 'bg-transparent',
  confirm: 'bg-green-400 text-white rounded-lg',
  tab: 'rounded-[18px] max-w-[91px] text-body text-sm',
  archive: 'text-gray-500 text-sm text-body',
  home: 'bg-green-400 text-white rounded-[18px] border border-green-400',
}

const SIZES: Record<Size, string> = {
  xs: 'w-6',
  sm: 'w-16 h-12',
  md: 'w-32 h-12',
  lg: 'w-[271px] h-12',
  xl: 'w-[335px] h-14',
  archive: 'px-4 py-2',
  confirm: 'w-58 py-2',
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  children: ReactNode
}

export default function Button({
  variant = 'primary',
  size,
  children,
  className = '',
  ...props
}: ButtonProps) {
  return (
    <button
      className={`flex justify-center items-center ${VARIANTS[variant]} ${
        size ? SIZES[size] : ''
      } ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
