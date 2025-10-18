import useClosenessStore from '../../stores/useClosenessStore'

interface ClosenessBarProps {
  chatRoomId: string
}

const BG_COLORS: Record<string, string> = {
  formal: 'bg-label-blue',
  casual: 'bg-label-yellow',
  close: 'bg-label-pink',
}

const ClosenessBar: React.FC<ClosenessBarProps> = ({ chatRoomId }) => {
  const closeness = useClosenessStore(state => state.closenessMap[chatRoomId] ?? 1)

  const closenessText = closeness == 1 ? 'Polite' : closeness == 2 ? 'Casual' : 'Friendly'

  const bgClass = BG_COLORS[closenessText] || 'bg-green-350'

  return (
    <div
      className={`mx-auto w-full max-w-md h-[33px] ${bgClass} overflow-hidden relative flex items-center justify-center text-white text-[14px] select-none shadow-[0_1px_2px_rgba(0,0,0,0.12)]`}
    >
      Chatting in {closenessText} Mode
    </div>
  )
}

export default ClosenessBar
