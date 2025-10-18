import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type TermId = 'service' | 'privacy' | 'marketing'
export type AgreementValue = Record<TermId, boolean>

interface State {
  value: AgreementValue
  setOne: (id: TermId, checked: boolean) => void
  setAll: (checked: boolean) => void
  setMany: (next: AgreementValue) => void
  reset: () => void
}

export const useAgreementStore = create<State>()(
  persist(
    set => ({
      value: { service: false, privacy: false, marketing: false },
      setOne: (id, checked) => set(s => ({ value: { ...s.value, [id]: checked } })),
      setAll: checked =>
        set(() => ({ value: { service: checked, privacy: checked, marketing: checked } })),
      setMany: next => set(() => ({ value: next })),
      reset: () => set(() => ({ value: { service: false, privacy: false, marketing: false } })),
    }),
    { name: 'agreement' }
  )
)
