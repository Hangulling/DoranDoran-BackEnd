import React from 'react'
import checkIcon from '../../assets/icon/signupCheck.svg'
import Button from './Button'
import type { CommonModalProps } from '../../types/common'

const CommonModal: React.FC<CommonModalProps> = ({
  open,
  title,
  variant = 'common',
  description,
  confirmText = '확인',
  cancelText = '닫기',
  onConfirm,
  onCancel,
}) => {
  if (!open) return null

  const isSignup = variant === 'signup'

  return (
    <div
      className="fixed inset-0 w-full mx-auto max-w-md z-100 flex items-center justify-center"
      role="dialog"
    >
      <div className="absolute inset-0 bg-black/80" onClick={onCancel}></div>
      <div className="relative z-10 flex flex-col items-center w-[303px] rounded-lg bg-white py-5 px-4">
        {isSignup && (
          <div className="m-2">
            <img src={checkIcon} className="w-10 h-10" />
          </div>
        )}
        <h3 className="text-title text-[18px] mb-1">{title}</h3>
        {Array.isArray(description) ? (
          <div className="text-[14px] text-center">
            {description.map((line, idx) => (
              <p key={idx}>{line}</p>
            ))}
          </div>
        ) : (
          <p className="text-[14px] text-center">{description}</p>
        )}

        {isSignup ? (
          <div className="w-full flex mt-4">
            <Button
              variant="confirm"
              onClick={onConfirm}
              size="lg"
              className="text-[14px] text-white py-[14px] text-subtitle"
            >
              {confirmText}
            </Button>
          </div>
        ) : (
          <div className="w-full flex gap-x-[14px] mt-4">
            <button
              className="bg-gray-80 text-subtitle text-[14px] text-gray-600 rounded-lg flex-1 py-[14px]"
              onClick={onCancel}
            >
              {cancelText}
            </button>
            <button
              className="bg-green-400 text-subtitle text-[14px] text-white rounded-lg flex-1 py-[14px]"
              onClick={onConfirm}
            >
              {confirmText}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default CommonModal
