import { useNavigate } from 'react-router-dom'
import { deleteUser, logout } from '../api'
import { useUserStore } from '../stores/useUserStore'
import useClosenessStore from '../stores/useClosenessStore'
import useRoomIdStore from '../stores/useRoomIdStore'
import { useCoachStore, useModalStore } from '../stores/useUiStateStore'
import showToast from '../components/common/CommonToast'
import ReactGA from 'react-ga4'
import { GA_ENABLED, IS_PROD } from '../constants/env'

function resetAllStores() {
  useUserStore.getState().reset()
  useClosenessStore.getState().reset()
  useRoomIdStore.getState().reset()
  useCoachStore.getState().reset()
  useModalStore.getState().reset()
}

// API 호출, 스토어 리셋, 라우팅
export function useSidebar(onClose: () => void) {
  const navigate = useNavigate()
  const userId = useUserStore(state => state.id)

  const handleAppReset = () => {
    resetAllStores()
    onClose()
    navigate('/login')
  }

  // 로그아웃 핸들러
  const handleLogout = async () => {
    try {
      await logout()
      if (IS_PROD && GA_ENABLED) {
        // confirm_logout
        const completeTimestamp = Math.floor(Date.now() / 1000) // UNIX 타임스탬프
        ReactGA.event('confirm_logout', {
          complete_timestamp: completeTimestamp,
        })
        ReactGA.set({ userId: null })
      }
      handleAppReset()
    } catch (error) {
      showToast({ message: 'Failed to log out. Please try again', iconType: 'error' })
      console.error('로그아웃 실패:', error)
    }
  }

  // 회원탈퇴 핸들러
  const handleDeleteAccount = async () => {
    try {
      await deleteUser(userId)
      if (IS_PROD && GA_ENABLED) {
        // confirm_delete_account
        const completeTimestamp = Math.floor(Date.now() / 1000) // UNIX 타임스탬프
        ReactGA.event('confirm_delete_account', {
          complete_timestamp: completeTimestamp,
        })
        ReactGA.set({ userId: null })
      }
      handleAppReset()
    } catch (error) {
      showToast({ message: 'Failed to delete account. Please try again', iconType: 'error' })
      console.error('회원 탈퇴 실패:', error)
    }
  }

  // PDF 열기
  const openAboutPdf = () => {
    onClose()
    const pdfUrl = `${import.meta.env.BASE_URL}docs/${encodeURIComponent('도란도란 서비스 소개서.pdf')}`
    window.open(pdfUrl, '_blank', 'noopener,noreferrer')
  }

  const goPolicy = (term: 'service' | 'privacy') => {
    onClose()
    navigate(`/policy/${term}`, {
      state: { hideConfirm: true, from: 'sidebar' },
    })
  }

  // 구글 폼
  const goForm = () => {
    onClose()
    window.open('https://forms.gle/dRBuvgKjwK7enscy6', '_blank')
  }

  return {
    handleLogout,
    handleDeleteAccount,
    openAboutPdf,
    goPolicy,
    goForm,
  }
}
