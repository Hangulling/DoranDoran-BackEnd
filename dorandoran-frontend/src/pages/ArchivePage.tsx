import ArchiveTabs, { type Room } from '../components/archive/ArchiveTabs'
import ExpressionCard from '../components/archive/ExpressionCard'
import CommonModal from '../components/common/CommonModal'
import EmptyCard from '../components/archive/EmptyCard'
import Button from '../components/common/Button'
import checkCircle from '../assets/icon/checkRound.svg'
import useArchiveStore from '../stores/useArchiveStore'
import { useEffect, useMemo, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { fakeArchiveItems } from '../mocks/db/archive'

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
  const [openModal, setOpenModal] = useState(false)
  const [showToast, setShowToast] = useState(false)
  const [openId, setOpenId] = useState<string | null>(null)
  const list = items.filter(i => i.chatRoom === activeRoom)
  const location = useLocation()

  const fromChat = (location.state as { from?: string } | null)?.from === 'chat'
  const { id } = useParams<{ id?: string }>()
  const idToRoom: Record<string, Room> = useMemo(
    () => ({ '1': 'Friend', '2': 'Honey', '3': 'Coworker', '4': 'Client' }),
    []
  )

  useEffect(() => {
    setOpenId(null)
  }, [activeRoom])

  useEffect(() => {
    if (items.length === 0) seedItems(fakeArchiveItems)
  }, [items.length, seedItems])

  useEffect(() => {
    if (id && idToRoom[id]) setActiveRoom(idToRoom[id])
  }, [id, idToRoom, setActiveRoom])

  return (
    <div className="min-h-full bg-gray-50 flex flex-col">
      {!fromChat && (
        <ArchiveTabs key={activeRoom} activeTab={activeRoom} onChange={setActiveRoom} />
      )}

      {list.length > 0 ? (
        <div className="mt-5">
          {list.map(item => (
            <ExpressionCard
              key={item.id}
              item={item}
              open={openId === item.id}
              onToggle={() => setOpenId(prev => (prev === item.id ? null : item.id))}
            />
          ))}
        </div>
      ) : (
        <EmptyCard />
      )}

      {selectionMode && (
        <div className="flex justify-center items-center">
          <Button
            variant="text"
            className={`min-w-md h-11 bg-white fixed bottom-0 ${selectedIds.size > 0 ? 'text-orange-300' : 'text-orange-100'} active:text-orange-600 hover:text-orange-600`}
            size="xl"
            onClick={() => setOpenModal(true)}
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
          title="Deleted saved phrase"
          description={'Do you want to delete phrase'}
          cancelText="Keep"
          confirmText="Delete"
          onCancel={() => setOpenModal(false)}
          onConfirm={() => {
            setOpenModal(false)
            setShowToast(true)
            setTimeout(() => setShowToast(false), 2000)
            exitSelectionMode()
          }}
        />
      )}
    </div>
  )
}
