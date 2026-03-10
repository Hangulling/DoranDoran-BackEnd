import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant =
  | 'primary'
  | 'cancel'
  | 'text'
  | 'confirm'
  | 'tab'
  | 'archive'
  | 'home'
  | 'notice'
  | 'disabledNotice'
type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'full' | 'archive' | 'confirm' | 'notice'

const VARIANTS: Record<Variant, string> = {
  primary: 'text-white rounded-lg disabled:bg-gray-100 disabled:text-gray-300',
  cancel: 'bg-gray-80 text-gray-600 rounded-lg',
  text: 'bg-transparent',
  confirm: 'bg-green-400 text-white rounded-lg',
  tab: 'rounded-[18px] px-[14px] py-2 text-body text-sm',
  archive: 'text-gray-500 text-sm text-body',
  home: 'bg-green-400 text-white rounded-[18px] border border-green-400',
  notice:
    'bg-[#6c51f0] text-white rounded-[12px] border border-[#6c51f0] text-subtitle text-[16px]',
  disabledNotice:
    'rounded-[12px] border-[1.6px] border-dashed border-[#dedddf] text-[16px] text-[#aba9ad]',
}

const SIZES: Record<Size, string> = {
  xs: 'w-6',
  sm: 'w-16 h-12',
  md: 'w-32 h-12',
  lg: 'w-[271px] h-12',
  xl: 'w-[335px] h-14',
  full: 'w-full h-14',
  archive: 'px-4 py-2',
  confirm: 'w-58 py-2',
  notice: 'px-5 py-[14px] w-[335px] h-13',
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
