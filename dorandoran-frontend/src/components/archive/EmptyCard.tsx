import Button from '../common/Button'
import arrowActive from '../../assets/icon/arrowRight.svg'
import emptyBookmark from '../../assets/icon/emptyBookmark.svg'
import { useLocation, useNavigate } from 'react-router-dom'
import useArchiveStore from '../../stores/useArchiveStore'
import type { Room } from './ArchiveTabs'

export default function EmptyCard() {
  const navigate = useNavigate()
  const location = useLocation()
  const fromChat = (location.state as { from?: string } | null)?.from === 'chat'
  const { activeRoom } = useArchiveStore()
  const roomToChatId: Record<Room, string> = {
      Friend: '1',
    Honey: '2',
    Coworker: '3',
    Client: '4',
  }

  const handleClick = () => {
    const chatId = roomToChatId[activeRoom]
    if (chatId) {
      navigate(`/chat/${chatId}`)
    } else {
      navigate('/chat')
    }
  }
  return (
    <div className="flex flex-col items-center justify-center py-10 mt-50">
      {fromChat && <img src={emptyBookmark} />}
      <p className="text-subtitle text-lg text-gray-800 my-2">Saved Expressions!</p>
      <p className="text-body text-sm text-gray-600">No saved expressions.</p>
      <p className="text-body text-sm text-gray-600 mb-6">Save useful expressions as you learn.</p>
      {!fromChat && (
        <div>
          <Button
            variant="text"
            className="border border-green-400 rounded-full px-4 py-2 text-green-400 hover:bg-green-50 active:bg-green-50"
            onClick={handleClick}
          >
            <span className="flex items-center gap-1 text-sm text-body">
              Start chatting
              <img src={arrowActive} alt="" className="w-4 h-4" />
            </span>
          </Button>
        </div>
      )}
    </div>
  )
}
