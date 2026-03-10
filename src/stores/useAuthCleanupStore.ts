import { create } from 'zustand'

interface AuthCleanupState {
  // 자동 로그아웃 직전에 실행할 비동기 작업
  preLogoutTask: (() => Promise<void>) | null

  // 로그아웃 직전 작업
  setPreLogoutTask: (task: (() => Promise<void>) | null) => void
}

export const useAuthCleanupStore = create<AuthCleanupState>(set => ({
  preLogoutTask: null,
  setPreLogoutTask: task => set({ preLogoutTask: task }),
}))
