import { useEffect, useMemo, useState, useRef } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { useInView } from 'react-intersection-observer'
import ReactGA from 'react-ga4'

import ArchiveTabs from '../components/archive/ArchiveTabs'
import ExpressionCard from '../components/archive/ExpressionCard'
import CommonModal from '../components/common/CommonModal'
import EmptyCard from '../components/archive/EmptyCard'
import Button from '../components/common/Button'
import checkCircle from '../assets/icon/checkRound.svg'
import useArchiveStore from '../stores/useArchiveStore'
import { useUserStore } from '../stores/useUserStore'
import {
  BOT_TO_ROOM,
  ROOM_TO_BOT,
  type BookmarkResponse,
  type BotType,
  type Room,
} from '../types/archive'
import { deleteManyBookmarks, getBookmarksByBotType } from '../api/archive'
import { GA_ENABLED, IS_PROD } from '../constants/env'

const PAGE_SIZE = 15

// 문자열 → 숫자 매핑(대소문자/공백 허용)
const mapIntimacyToNum = (s?: string | null) => {
  if (!s) return undefined
  const v = s.trim().toLowerCase()
  if (v === 'polite') return 1
  if (v === 'casual') return 2
  if (v === 'friendly') return 3
  return undefined
}

// 친밀도 안전 추출 헬퍼 (any 없이)
function readIntimacyLevel(item: unknown): string | undefined {
  if (typeof item !== 'object' || item === null) return undefined
  const obj = item as Record<string, unknown>

  const nested = obj['aiResponse']
  if (nested && typeof nested === 'object') {
    const lvl = (nested as Record<string, unknown>)['intimacyLevel']
    if (typeof lvl === 'string') return lvl
  }
  const flat = obj['intimacyLevel']
  if (typeof flat === 'string') return flat
  return undefined
}

// ====== 삭제 이벤트용 안전 접근 헬퍼 ======
type ContentType = 'bot_message' | 'word_explanation' | 'ment_explanation'

function readChatroomId(item: unknown): string | undefined {
  if (typeof item !== 'object' || item === null) return undefined
  const v = (item as Record<string, unknown>)['chatroomId']
  return typeof v === 'string' ? v : undefined
}

function readCreatedAt(item: unknown): string | undefined {
  if (typeof item !== 'object' || item === null) return undefined
  const v = (item as Record<string, unknown>)['createdAt']
  return typeof v === 'string' ? v : undefined
}

function readContentType(item: unknown): ContentType {
  if (typeof item !== 'object' || item === null) return 'bot_message'
  const obj = item as Record<string, unknown>
  const raw = obj['type'] ?? obj['contentType']
  if (raw === 'word_explanation') return 'word_explanation'
  if (raw === 'ment_explanation') return 'ment_explanation'
  return 'bot_message'
}

// "hh.mm" 형태 문자열(요구사항 그대로)로 경과시간 생성
function timeSavedHHDotMM(createdAt?: string): string | undefined {
  if (!createdAt) return undefined
  const ms = Date.now() - new Date(createdAt).getTime()
  if (!Number.isFinite(ms)) return undefined
  const totalMin = Math.max(0, Math.round(ms / 60000))
  const hh = Math.floor(totalMin / 60)
  const mm = totalMin % 60
  return `${hh}.${mm.toString().padStart(2, '0')}`
}

export default function ArchivePage() {
  const {
    items,
    activeRoom,
    setActiveRoom,
    selectionMode,
    selectedIds,
    seedItems,
    exitSelectionMode,
  } = useArchiveStore()

  const userId = useUserStore(state => state.id)

  const [openModal, setOpenModal] = useState(false)
  const [showToast, setShowToast] = useState(false)
  const [openId, setOpenId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [roomResolved, setRoomResolved] = useState(false)
  const [deleteCount, setDeleteCount] = useState(0)

  const list = useMemo(
    () => items.filter(i => BOT_TO_ROOM[i.botType as BotType] === activeRoom),
    [items, activeRoom]
  )

  const [windowStart, setWindowStart] = useState(0)

  useEffect(() => {
    setWindowStart(0)
    setOpenId(null)
  }, [activeRoom, list.length])

  const visibleList = useMemo(() => {
    if (list.length <= PAGE_SIZE) return list
    const maxStart = Math.max(0, list.length - PAGE_SIZE)
    const safeStart = Math.max(0, Math.min(windowStart, maxStart))
    return list.slice(safeStart, safeStart + PAGE_SIZE)
  }, [list, windowStart])

  const location = useLocation()
  const fromChat = (location.state as { from?: string } | null)?.from === 'chat'
  const { id } = useParams<{ id?: string }>()
  const idToRoom: Record<string, Room> = useMemo(
    () => ({ '1': 'Friend', '2': 'Honey', '3': 'Coworker', '4': 'Senior' }),
    []
  )

  useEffect(() => {
    if (id && idToRoom[id]) setActiveRoom(idToRoom[id])
    setRoomResolved(true)
  }, [id, idToRoom, setActiveRoom])
  // GA: view_store (페이지 진입 1회)
  useEffect(() => {
    if (!roomResolved) return
    if (!IS_PROD || !GA_ENABLED) return
    if (!userId) return

    const entryPoint = fromChat ? 'chatroom_id' : 'chatroom_list'
    const entryTimestamp = Math.floor(Date.now() / 1000)
    ReactGA.event('view_store', {
      entry_point: entryPoint,
      entry_timestamp: entryTimestamp,
    })
  }, [roomResolved, fromChat, userId])

  // 탭 클릭인지 구분용
  const pendingClickRoomRef = useRef<Room | null>(null)

  const handleTabChange = (room: Room) => {
    pendingClickRoomRef.current = room
    setActiveRoom(room)
  }

  useEffect(() => {
    const fetchByRoom = async () => {
      try {
        const botType: BotType = ROOM_TO_BOT[activeRoom]
        const res: BookmarkResponse[] = await getBookmarksByBotType(botType)
        seedItems(res)
        setError(null)

        if (
          IS_PROD &&
          GA_ENABLED &&
          userId &&
          pendingClickRoomRef.current === activeRoom &&
          res.length > 0
        ) {
          const latest = res[0]
          const strLevel = readIntimacyLevel(latest) ?? null

          ReactGA.event('click_home_store', {
            concept: activeRoom.toLowerCase(),
            intimacy_level: mapIntimacyToNum(strLevel),
          })
        }
        pendingClickRoomRef.current = null
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e)
        console.error('★ 북마크 조회 실패:', msg)
        setError('조회 중 오류가 발생했습니다.')
      }
    }
    fetchByRoom()
  }, [activeRoom, seedItems, userId])

  const { ref: topRef, inView: topInView } = useInView({
    threshold: 0.1,
  })
  const { ref: bottomRef, inView: bottomInView } = useInView({
    threshold: 0.1,
  })

  useEffect(() => {
    if (!bottomInView) return
    if (list.length <= PAGE_SIZE) return

    setWindowStart(prev => {
      const maxStart = Math.max(0, list.length - PAGE_SIZE)
      if (prev >= maxStart) return prev
      return Math.min(prev + 1, maxStart)
    })
  }, [bottomInView, list.length])

  useEffect(() => {
    if (!topInView) return
    if (list.length <= PAGE_SIZE) return

    setWindowStart(prev => {
      if (prev <= 0) return 0
      return prev - 1
    })
  }, [topInView, list.length])

  const handleConfirmDelete = async () => {
    try {
      const ids = [...selectedIds]
      const itemsToDelete = list.filter(it => ids.includes(it.id))

      if (ids.length > 0) {
        await deleteManyBookmarks(ids)
      }

      if (IS_PROD && GA_ENABLED && userId) {
        const concept = activeRoom.toLowerCase()
        itemsToDelete.forEach(item => {
          ReactGA.event('click_store_delete', {
            chatroom_id: readChatroomId(item),
            content_type: readContentType(item),
            time_saved: timeSavedHHDotMM(readCreatedAt(item)),
            concept: concept,
          })
        })
      }

      setShowToast(true)
      setTimeout(() => setShowToast(false), 4000)
      exitSelectionMode()

      const botType: BotType = ROOM_TO_BOT[activeRoom]
      const data: BookmarkResponse[] = await getBookmarksByBotType(botType)
      seedItems(data)
    } catch (error) {
      console.log(error)
      setError('삭제 중 오류 발생')
    } finally {
      setOpenModal(false)
    }
  }

  return (
    <div className="min-h-full bg-gray-50 flex flex-col">
      {!fromChat && (
        <ArchiveTabs key={activeRoom} activeTab={activeRoom} onChange={handleTabChange} />
      )}

      {error && (
        <div className="alert alert-error my-2">
          <span>{error}</span>
        </div>
      )}

      {visibleList.length > 0 ? (
        <div className="mt-5">
          {visibleList.map((item, idx) => {
            const isFirst = idx === 0
            const isLast = idx === visibleList.length - 1

            const ref = isFirst ? topRef : isLast ? bottomRef : undefined

            return (
              <ExpressionCard
                key={item.id}
                item={item}
                open={openId === item.id}
                onToggle={() => setOpenId(prev => (prev === item.id ? null : item.id))}
                ref={ref}
              />
            )
          })}
        </div>
      ) : (
        <div className="flex flex-grow justify-center items-center">
          <EmptyCard />
        </div>
      )}

      {selectionMode && (
        <div className="flex justify-center items-center">
          <Button
            variant="text"
            className={`min-w-md h-11 bg-white fixed bottom-0 ${
              selectedIds.size > 0 ? 'text-orange-300' : 'text-orange-100'
            } active:text-orange-600 hover:text-orange-600`}
            size="xl"
            onClick={() => {
              setDeleteCount(selectedIds.size)
              setOpenModal(true)
            }}
            disabled={selectedIds.size < 1}
          >
            Delete {selectedIds.size > 0 ? `${selectedIds.size}` : ''}
          </Button>
        </div>
      )}

      {showToast && (
        <div className="toast toast-center w-[335px] mb-6">
          <div className="alert bg-[#0F1010] opacity-80">
            <img src={checkCircle} className="w-5 h-5" />
            <span className="text-subtitle text-sm text-white">Saved phrase is deleted</span>
          </div>
        </div>
      )}

      {openModal && (
        <CommonModal
          open
          title="Delete saved phrase"
          description={`Do you want to delete ${deleteCount} ${
            deleteCount > 1 ? 'phrases' : 'phrase'
          }?`}
          cancelText="Keep"
          confirmText="Delete"
          onCancel={() => setOpenModal(false)}
          onConfirm={handleConfirmDelete}
        />
      )}
    </div>
  )
}
