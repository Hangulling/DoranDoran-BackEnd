import { useState } from 'react'
import Button from './Button'
interface InputProps {
  id?: string
  name?: string
  label?: string
  type?: string
  placeholder?: string
  variant?: 'primary' | 'error'
  error?: string
  size?: 'sm' | 'md' | 'lg'
  value?: string
  readOnly?: boolean
  inputMode?: React.HTMLAttributes<HTMLInputElement>['inputMode']
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
  onKeyDown?: (e: React.KeyboardEvent<HTMLInputElement>) => void
}

export default function Input({
  id,
  name,
  label,
  variant = 'primary',
  type = 'text',
  size = 'lg',
  placeholder,
  value,
  inputMode,
  readOnly,
  onChange,
  onBlur,
  onKeyDown,
}: InputProps) {
  const [show, setShow] = useState<boolean>(false)
  const isPassword = type === 'password'
  const VARIANTS = {
    primary: 'flex border rounded-lg border-gray-200 px-4 py-3 my-2 bg-white',
    error: 'flex border rounded-lg border-orange-300 px-4 py-3 my-2 bg-white',
  } as const

  const SIZES = {
    sm: 'w-[247px] h-12',
    md: 'w-[254px] h-12',
    lg: 'w-full h-14',
  }

  return (
    <div className="flex flex-col">
      {label && (
        <label
          htmlFor={id}
          className="whitespace-nowrap text-gray-800 text-base text-subtitle my-2"
        >
          {label}
        </label>
      )}
      <div className={`${VARIANTS[variant]} ${SIZES[size]} `}>
        <input
          type={isPassword ? (show ? 'text' : 'password') : type}
          id={id}
          name={name}
          placeholder={placeholder}
          className="outline-none w-full text-base text-body "
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          onKeyDown={onKeyDown}
          inputMode={inputMode}
          readOnly={readOnly}
        />
        {isPassword && value && value.length > 0 && (
          <Button type="button" variant="text" size="xs" onClick={() => setShow(s => !s)}>
            <span className="text-gray-500">{show ? '👁️' : '👁️‍🗨️'}</span>
          </Button>
        )}
      </div>
    </div>
  )
}
