import check from '../../assets/icon/checkAll.svg'
import checkOff from '../../assets/icon/disabledCheckAll.svg'
import speakerOn from '../../assets/icon/activeVolume.svg'
import speakerOff from '../../assets/icon/defaultVolume.svg'
import expandIcon from '../../assets/icon/expandArrow.svg'
import clsoeExpandIcon from '../../assets/icon/arrowUp.svg'
import useArchiveStore from '../../stores/useArchiveStore'
import useTTS from '../../hooks/useTTS'
import Badge, { type BadgeVariant } from '../common/Badge'
import DescriptionBubble from '../chat/DescriptionBubble'

interface ExpressionCardProps {
  item: {
    id: string
    text: string
    intimacy: number
  }

  open?: boolean
  onToggle?: () => void
}

export default function ExpressionCard({ item, open, onToggle }: ExpressionCardProps) {
  const { selectionMode, selectedIds, toggleSelect } = useArchiveStore()
  const isSelected = selectedIds.has(item.id)
  const { onPlay, playing } = useTTS(item.text)
  const getBadgeVariant = (level: number): BadgeVariant => {
    switch (level) {
      case 1:
        return 'Polite'
      case 2:
        return 'Casual'
      case 3:
        return 'Friendly'
      default:
        return 'Polite'
    }
  }

  const handleClick = () => {
    if (selectionMode) toggleSelect(item.id)
  }

  const handleSpeakerClick = (e: React.MouseEvent<HTMLImageElement>) => {
    e.stopPropagation()
    onPlay()
  }

  const handleToggleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation()
    onToggle?.()
  }

  return (
    <div className="flex flex-col justify-center items-center mb-3" onClick={handleClick}>
      <div>
        <div
          className={`flex flex-col justify-center items-center border ${
            isSelected ? 'border-green-300 bg-green-50' : 'border-gray-100 bg-white'
          } w-[335px] min-h-[71px] rounded-xl`}
        >
          <div className="flex w-full justify-between px-4 pt-[10px]">
            <Badge variant={getBadgeVariant(item.intimacy)} />

            {selectionMode && (
              <img
                src={isSelected ? check : checkOff}
                className="w-5 h-5"
                alt={isSelected ? '선택됨' : '선택 안 됨'}
              />
            )}
          </div>

          <div className="flex justify-between w-full px-4 pb-[10px] mt-1">
            <div className="flex">
              <img
                src={playing ? speakerOn : speakerOff}
                className="w-5 h-5 mr-1"
                alt={playing ? '읽는 중' : '읽기'}
                onClick={handleSpeakerClick}
              />
              <span
                className={`text-body text-sm text-gray-800 ${open ? 'whitespace-pre-line' : 'truncate'}`}
              >
                {item.text}
              </span>
            </div>
            <button onClick={handleToggleClick}>
              <img src={open ? clsoeExpandIcon : expandIcon} />
            </button>
          </div>

          {open && (
            <div>
              <div className="mx-4 my-2 h-px bg-gray-80 " />
              <DescriptionBubble
                isSelected={isSelected}
                variant="archive"
                word="치맥"
                pronunciation="chi-maek"
                descriptionByTab={{
                  Kor: '맥주와 치킨을 같이 즐기는 어쩌구',
                  Eng: 'A Korean slang term for the popular pairing of fried chicken and beer (maekju).',
                }}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
