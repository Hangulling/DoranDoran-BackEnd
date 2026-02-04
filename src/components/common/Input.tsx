import { useState } from 'react'
import eye from '../../assets/icon/eye.svg'
import eyeOff from '../../assets/icon/eyeOff.svg'
import Button from './Button'
interface InputProps {
  id?: string
  name?: string
  label?: string
  type?: string
  placeholder?: string
  variant?: 'primary' | 'error'
  error?: string
  size?: 'md' | 'lg'
  value?: string
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
  onChange,
  onBlur,
  onKeyDown,
}: InputProps) {
  const [show, setShow] = useState<boolean>(false)
  const isPassword = type === 'password'
  const VARIANTS = {
    primary: 'flex border rounded-lg border-gray-100 px-3 my-2',
    error: 'flex border rounded-lg border-orange-300 px-3 my-2',
  } as const

  const SIZES = {
    md: 'w-[254px] h-12',
    lg: 'w-[335px] h-12',
  }

  return (
    <div className="flex flex-col">
      {label && (
        <label htmlFor={id} className="text-gray-800 text-base text-subtitle my-2">
          {label}
        </label>
      )}
      <div className={`${VARIANTS[variant]} ${SIZES[size]}`}>
        <input
          type={isPassword ? (show ? 'text' : 'password') : type}
          id={id}
          name={name}
          placeholder={placeholder}
          className="outline-none w-full text-sm text-body "
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          onKeyDown={onKeyDown}
        />
        {isPassword && value && value.length > 0 && (
          <Button type="button" variant="text" size="xs" onClick={() => setShow(s => !s)}>
            <img src={show ? eye : eyeOff} alt="eye" className="w-5 h-5" />
          </Button>
        )}
      </div>
    </div>
  )
}
