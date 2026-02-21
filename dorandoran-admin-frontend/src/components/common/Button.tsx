import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant =
  | 'primary'
  | 'cancel'
  | 'text'
  | 'confirm'
  | 'tab'
  | 'archive'
  | 'home'
  | 'dropdown'
type Size =
  | 'xs'
  | 'sm'
  | 'md'
  | 'lg'
  | 'xl'
  | 'full'
  | 'archive'
  | 'confirm'
  | 'dropdown'

const VARIANTS: Record<Variant, string> = {
  primary:
    'text-gray-0 text-subtitle text-[16px] rounded-xl disabled:bg-gray-100 disabled:text-gray-0',
  cancel: 'bg-gray-80 text-gray-600 rounded-lg',
  text: 'bg-transparent',
  confirm: 'bg-primary-300 text-white rounded-xl',
  tab: 'rounded-[18px] px-[14px] py-2 text-body text-sm',
  archive: 'text-gray-500 text-sm text-body',
  home: 'bg-green-400 text-white rounded-[18px] border border-green-400',
  dropdown: 'px-3',
}

const SIZES: Record<Size, string> = {
  xs: 'w-6',
  sm: 'w-[71px] h-12',
  md: 'w-32 h-12',
  lg: 'w-[260px] h-[45px]',
  xl: 'w-[335px] h-14',
  full: 'w-full h-14',
  archive: 'px-4 py-2',
  confirm: 'w-full h-13',
  dropdown: 'w-[335px] h-12',
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
  const isDropdown = variant === 'dropdown'

  return (
    <button
      className={`${!isDropdown && `flex justify-center items-center`}
       ${VARIANTS[variant]} ${size ? SIZES[size] : ''} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
