import { create } from 'zustand'

interface UserState {
  id: string
  name: string
  setId: (id: string) => void
  setName: (name: string) => void
}

// ID, 이름 저장
export const useUserStore = create<UserState>(set => ({
  id: '',
  name: '',
  setId: id => set({ id }),
  setName: name => set({ name }),
}))
