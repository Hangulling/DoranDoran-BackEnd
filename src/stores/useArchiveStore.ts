import { create } from 'zustand'
import type { BookmarkResponse, Room } from '../types/archive'
import { BOT_TO_ROOM } from '../types/archive'

interface ArchiveState {
  items: BookmarkResponse[]
  selectedIds: Set<string>
  selectionMode: boolean
  deleteMode: boolean
  activeRoom: Room

  seedItems: (items: BookmarkResponse[]) => void
  appendItems: (items: BookmarkResponse[]) => void
  resetItems: () => void

  setActiveRoom: (room: Room) => void
  enterSelectionMode: () => void
  exitSelectionMode: () => void
  toggleSelect: (id: string) => void
  selectAll: () => void
  deselectAll: () => void

  deleteSelected: () => void
}

const useArchiveStore = create<ArchiveState>(set => ({
  items: [],
  selectedIds: new Set(),
  selectionMode: false,
  activeRoom: 'Honey',
  deleteMode: false,

  seedItems: items => set({ items }),

  appendItems: newItems =>
    set(state => {
      if (!newItems?.length) return {}
      const existing = new Set(state.items.map(i => i.id))
      const deduped = newItems.filter(i => !existing.has(i.id))
      if (deduped.length === 0) return {}
      return { items: [...state.items, ...deduped] }
    }),

  resetItems: () =>
    set({
      items: [],
      selectedIds: new Set(),
      selectionMode: false,
      deleteMode: false,
    }),

  setActiveRoom: room =>
    set({
      activeRoom: room,
      selectedIds: new Set(),
      deleteMode: false,
    }),

  enterSelectionMode: () => set({ selectionMode: true }),
  exitSelectionMode: () => set({ selectionMode: false, selectedIds: new Set(), deleteMode: false }),

  toggleSelect: (id: string) =>
    set(state => {
      const next = new Set(state.selectedIds)
      if (next.has(id)) next.delete(id)
      else next.add(id)

      const idsInRoom = state.items
        .filter(i => BOT_TO_ROOM[i.botType] === state.activeRoom)
        .map(i => i.id)

      const allSelectedInRoom = idsInRoom.length > 0 && idsInRoom.every(x => next.has(x))

      return { selectedIds: next, deleteMode: allSelectedInRoom }
    }),

  selectAll: () =>
    set(state => {
      const idsInRoom = state.items
        .filter(i => BOT_TO_ROOM[i.botType] === state.activeRoom)
        .map(i => i.id)
      return {
        selectedIds: new Set(idsInRoom),
        deleteMode: idsInRoom.length > 0,
        selectionMode: true,
      }
    }),

  deselectAll: () =>
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
        deleteMode: false,
      }
    }),
}))

export default useArchiveStore
