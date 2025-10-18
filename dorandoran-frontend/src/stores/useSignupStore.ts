import { create } from 'zustand'

interface SignupFormState {
  firstName: string
  lastName: string
  email: string
  password: string
  passwordCheck: string
  setMany: (p: Partial<SignupFormState>) => void
  reset: () => void
}

export const useSignupFormStore = create<SignupFormState>(set => ({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  passwordCheck: '',
  setMany: p => set(p),
  reset: () =>
    set({
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      passwordCheck: '',
    }),
}))
