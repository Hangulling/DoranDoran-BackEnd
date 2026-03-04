import React, { useState } from 'react'
import CloseIcon from '../../assets/icon/blackClose.svg?react'
import RightArrowIcon from '../../assets/icon/blackArrowRight.svg'
import LogoutIcon from '../../assets/icon/logout.svg?react'
import SignupIcon from '../../assets/icon/signup.svg?react'
import type { SidebarProps } from '../../types/common'
import CommonModal from './CommonModal'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../../constants/env'
import { useSidebarAnimation } from '../../hooks/useSidebarAnimation'
import { useSidebar } from '../../hooks/useSidebarAction'

const LOGOUT_DESC = ['You can log in again anytime.']
const SIGNOUT_DESC_JSX = [
  '1. This action cannot be undone.',
  '2. It is not possible to sign up again with a withdrawn email.',
  <span key="confirm" className="text-orange-400">
    Are you sure you want to continue?
  </span>,
]

const menuBtn =
  'flex items-center justify-between px-5 py-4 w-full focus:bg-green-80 hover:bg-green-80'
const iconBtn =
  'flex items-center gap-x-[6px] text-gray-400 text-[14px] hover:text-gray-800 focus:text-gray-800'

const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { visible, isActive } = useSidebarAnimation(isOpen, 300)
  const { handleLogout, handleDeleteAccount, openAboutPdf, goPolicy, goForm } = useSidebar(onClose)

  const [modalOpen, setModalOpen] = useState(false)
  const [modalType, setModalType] = useState<'logout' | 'signup' | null>(null)

  if (!visible) return null

  // 로그아웃 버튼
  const openLogoutModal = () => {
    setModalType('logout')
    setModalOpen(true)
  }

  // 회원탈퇴 버튼
  const openSignupModal = () => {
    setModalType('signup')
    setModalOpen(true)
  }

  const handleConfirm = async () => {
    if (modalType === 'logout') {
      // click_logout
      if (IS_PROD && GA_ENABLED) {
        ReactGA.event('click_logout', {})
      }
      await handleLogout()
    } else if (modalType === 'signup') {
      // click_delete_account
      if (IS_PROD && GA_ENABLED) {
        ReactGA.event('click_delete_account', {})
      }
      await handleDeleteAccount()
    }
    setModalOpen(false)
  }

  const handleCancel = () => {
    setModalOpen(false)
  }

  return (
    <>
      {/* 배경 오버레이 */}
      <div
        className={`absolute inset-0 z-50 h-full bg-black transition-opacity duration-300 ${
          isActive ? 'opacity-80' : 'opacity-0 pointer-events-none'
        }`}
        onClick={onClose}
        aria-hidden="true"
      />
      {/* 사이드바 */}
      <div
        className={`absolute top-0 left-0 z-60 bg-[#fafafa] h-full w-[303px] transform transition-transform duration-300 ease-in-out ${
          isActive ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* 닫기 버튼 */}
        <button className="absolute top-6 right-6" onClick={onClose} aria-label="닫기">
          <CloseIcon />
        </button>
        {/* 컨텐츠 */}
        <div className="flex flex-col h-full pb-[env(safe-area-inset-bottom)]">
          {/* 메뉴 리스트 */}
          <div className="flex flex-col gap-y-1 mt-20 py-2 text-[14px] border-t border-b border-gray-80">
            <button className={menuBtn} onClick={openAboutPdf}>
              About the Service
              <img src={RightArrowIcon} />
            </button>
            <button className={menuBtn} onClick={() => goPolicy('service')}>
              Terms of Service
              <img src={RightArrowIcon} />
            </button>
            <button className={menuBtn} onClick={() => goPolicy('privacy')}>
              Privacy Policy
              <img src={RightArrowIcon} />
            </button>
            <button className={menuBtn} onClick={goForm}>
              Contact Us
              <img src={RightArrowIcon} />
            </button>
          </div>

          {/* 하단 버튼 그룹 */}
          <div className="mt-auto mb-[34px] ml-5 flex flex-col gap-5">
            <button className={iconBtn} onClick={openLogoutModal}>
              <LogoutIcon />
              Log out
            </button>
            <button className={iconBtn} onClick={openSignupModal}>
              <SignupIcon />
              Delete account
            </button>
          </div>
        </div>
      </div>

      {/* 모달 */}
      <CommonModal
        open={modalOpen}
        title={modalType === 'logout' ? 'Logout' : 'Delete Account'}
        description={modalType === 'logout' ? LOGOUT_DESC : SIGNOUT_DESC_JSX}
        confirmText={modalType === 'logout' ? 'Log out' : 'Delete'}
        cancelText="keep"
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    </>
  )
}

export default Sidebar
