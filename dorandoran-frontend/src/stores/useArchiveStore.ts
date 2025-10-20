import { create } from 'zustand'
export interface ExpressionItem {
  id: string
  chatRoom: 'Friend' | 'Honey' | 'Coworker' | 'Client'
  text: string
  intimacy: number
  ttsUrl?: string
  savedAt?: string
}

type Room = 'Friend' | 'Honey' | 'Coworker' | 'Client'

interface ArchiveState {
  items: ExpressionItem[]
  selectedIds: Set<string>
  selectionMode: boolean
  deleteMode: boolean
  activeRoom: Room

  seedItems: (items: ExpressionItem[]) => void
  setActiveRoom: (room: Room) => void

  enterSelectionMode: () => void
  exitSelectionMode: () => void

  toggleSelect: (id: string) => void
  selectAll: () => void
  delectAll: () => void

  deleteSelected: () => void
}

const useArchiveStore = create<ArchiveState>(set => ({
  items: [],
  selectedIds: new Set(),
  selectionMode: false,
  activeRoom: 'Honey',
  deleteMode: false,

  seedItems: items => set({ items }),
  setActiveRoom: room => set({ activeRoom: room }),

  enterSelectionMode: () => set({ selectionMode: true }),
  exitSelectionMode: () => set({ selectionMode: false, selectedIds: new Set() }),

  toggleSelect: (id: string) =>
    set(state => {
      const next = new Set(state.selectedIds)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return { selectedIds: next }
    }),

  selectAll: () =>
    set(state => ({
      selectedIds: new Set(state.items.filter(i => i.chatRoom === state.activeRoom).map(i => i.id)),
      deleteMode: true,
    })),

  delectAll: () =>
    set({
      selectedIds: new Set(),
      deleteMode: false,
    }),

  deleteSelected: () =>
    set(state => {
      const remain = state.items.filter(i => !state.selectedIds.has(i.id))
      return {
        items: remain,
        selectedIds: new Set(),
        selectionMode: false,
      }
    }),
}))

export default useArchiveStore
