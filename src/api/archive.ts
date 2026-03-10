import type {
  BookmarkByRoomResponse,
  BookmarkRequest,
  BookmarkResponse,
  BotType,
} from '../types/archive'
import api from './api'
import { BOOKMARK_ENDPOINTS } from './endpoints'

export async function createBookmark(
  data: BookmarkRequest,
  config?: { headers: Record<string, string> }
) {
  const res = await api.post(BOOKMARK_ENDPOINTS.CREATE, data, config)
  return res.data
}

export async function getAllBookmarks() {
  const res = await api.get(BOOKMARK_ENDPOINTS.LIST_ALL)
  return res.data
}

export async function getBookmarksByCursor(lastId?: string, size = 15) {
  const res = await api.get(`/api/store/bookmarks/cursor`, {
    params: { lastId, size },
  })
  return res.data as {
    content: BookmarkResponse[]
    totalElements: number
    numberOfElements: number
    last: boolean
    first: boolean
    empty: boolean
  }
}

export async function getBookmarksCursorByBotType(botType: BotType, lastId?: string, size = 15) {
  const res = await api.get(BOOKMARK_ENDPOINTS.LIST_CURSOR_BY_BOTTYPE(botType, lastId, size))
  return res.data as {
    content: BookmarkResponse[]
    totalElements: number
    numberOfElements: number
    last: boolean
    first: boolean
    empty: boolean
  }
}

export async function getBookmarksCursorByChatroom(chatroomId: string, lastId?: string, size = 15) {
  const url = BOOKMARK_ENDPOINTS.LIST_CURSOR_BY_CHATROOM(chatroomId, lastId, size)
  const res = await api.get(url)
  return res.data as {
    content: BookmarkResponse[]
    totalElements: number
    numberOfElements: number
    last: boolean
    first: boolean
    empty: boolean
  }
}

export async function getBookmarksByBotType(botType: 'friend' | 'honey' | 'coworker' | 'senior') {
  const res = await api.get(BOOKMARK_ENDPOINTS.LIST_BY_BOT_TYPE(botType))
  return res.data
}

export async function deleteBookmark(bookmarkId: string) {
  await api.delete(BOOKMARK_ENDPOINTS.DELETE_ONE(bookmarkId))
}

export async function deleteManyBookmarks(bookmarkIds: string[]) {
  await api.delete(BOOKMARK_ENDPOINTS.DELETE_MANY, { data: bookmarkIds })
}

export async function countBookmarks(): Promise<number> {
  const res = await api.get<number>(BOOKMARK_ENDPOINTS.COUNT)
  return res.data
}

export async function getBookmarksByRoomId(chatroomId: string): Promise<BookmarkByRoomResponse[]> {
  const res = await api.get<BookmarkByRoomResponse[]>(
    BOOKMARK_ENDPOINTS.LIST_BY_CHATROOMID(chatroomId)
  )
  return res.data
}
