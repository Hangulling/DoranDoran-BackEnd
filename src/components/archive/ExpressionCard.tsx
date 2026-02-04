import check from '../../assets/icon/checkAll.svg'
import checkOff from '../../assets/icon/disabledCheckAll.svg'
import speakerOn from '../../assets/icon/activeVolume.svg'
import speakerOff from '../../assets/icon/defaultVolume.svg'
import expandIcon from '../../assets/icon/expandArrow.svg'
import closeExpandIcon from '../../assets/icon/arrowUp.svg'
import useArchiveStore from '../../stores/useArchiveStore'
import useTTS from '../../hooks/useTTS'
import Badge, { type BadgeVariant } from '../common/Badge'
import DescriptionBubble from '../chat/DescriptionBubble'
import type { BookmarkResponse } from '../../types/archive'
import { forwardRef } from 'react'

interface ExpressionCardProps {
  item: BookmarkResponse
  open?: boolean
  onToggle?: () => void
}

const ExpressionCard = forwardRef<HTMLDivElement, ExpressionCardProps>(
  ({ item, open, onToggle }, ref) => {
    const { selectionMode, selectedIds, toggleSelect } = useArchiveStore()
    const isSelected = selectedIds.has(item.id)
    const ttsText =
      item.correctedContent && item.correctedContent.trim().length > 0
        ? item.correctedContent
        : item.content
    const { onPlay, playing } = useTTS(ttsText)

    const getBadgeVariant = (level: string | undefined): BadgeVariant => {
      switch (level) {
        case 'Polite':
          return 'Polite'
        case 'Casual':
          return 'Casual'
        case 'Friendly':
          return 'Friendly'
        default:
          return 'Polite'
      }
    }

    const handleCheckboxClick = (e: React.MouseEvent<HTMLImageElement>) => {
      e.stopPropagation()
      if (selectionMode) {
        toggleSelect(item.id)
      }
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
      <div ref={ref} className="flex flex-col justify-center items-center mb-3">
        <div>
          <div
            className={`flex flex-col justify-center items-center border ${
              isSelected ? 'border-green-300 bg-green-50' : 'border-gray-100 bg-white'
            } w-[335px] min-h-[71px] rounded-xl`}
          >
            <div className="flex w-full justify-between px-4 pt-[10px]">
              <Badge variant={getBadgeVariant(item.aiResponse.intimacyLevel)} />

              {selectionMode && (
                <img
                  src={isSelected ? check : checkOff}
                  className="w-5 h-5"
                  alt={isSelected ? '선택됨' : '선택 안 됨'}
                  onClick={handleCheckboxClick}
                />
              )}
            </div>

            <div
              className={`flex justify-between w-full px-4 mt-1 items-end ${
                open ? 'pb-0' : 'pb-3'
              }`}
            >
              <div className="flex items-start gap-1 flex-1 min-w-0">
                <img
                  src={playing ? speakerOn : speakerOff}
                  className="w-5 h-5 mr-1"
                  alt={playing ? '읽는 중' : '읽기'}
                  onClick={handleSpeakerClick}
                />
                {(() => {
                  const contentToShow =
                    item.correctedContent && item.correctedContent.trim().length > 0
                      ? item.correctedContent
                      : item.content

                  return (
                    <span
                      className={`text-body text-sm text-gray-800 ${
                        open ? 'whitespace-normal' : 'truncate'
                      }`}
                    >
                      {contentToShow}
                    </span>
                  )
                })()}
              </div>
              <button onClick={handleToggleClick}>
                <img src={open ? closeExpandIcon : expandIcon} />
              </button>
            </div>

            {open && (
              <div>
                <div className="mx-4 my-[6px] h-px bg-gray-80 " />
                {(() => {
                  const ai = item.aiResponse ?? {}
                  const firstVoca =
                    ai.vocabulary && ai.vocabulary.length > 0 ? ai.vocabulary[0] : undefined

                  const word =
                    item.correctedContent && item.correctedContent.trim()
                      ? item.content
                      : (firstVoca?.word && firstVoca.word.trim()) || '표현'

                  const pronunciation =
                    (firstVoca?.pronunciation && firstVoca.pronunciation.trim()) ||
                    (ai.translation?.pronunciation && ai.translation.pronunciation.trim()) ||
                    ''

                  const kor =
                    (firstVoca?.korExplanation && firstVoca.korExplanation.trim()) ||
                    (ai.description && ai.description.trim()) ||
                    ''

                  const eng =
                    (firstVoca?.explanation && firstVoca.explanation.trim()) ||
                    (ai.translation?.english && ai.translation.english.trim()) ||
                    ''

                  const descriptionByTab: Record<string, string> = {}
                  if (kor) descriptionByTab.Kor = kor
                  if (eng) descriptionByTab.Eng = eng

                  if (Object.keys(descriptionByTab).length === 0) return null

                  const hasCorrection =
                    !!item.correctedContent?.trim() &&
                    item.correctedContent.trim() !== item.content.trim()

                  return (
                    <DescriptionBubble
                      isSelected={isSelected}
                      variant="archive"
                      word={word}
                      pronunciation={pronunciation}
                      correctMsg={hasCorrection}
                      descriptionByTab={descriptionByTab}
                      initialTab={eng ? 'Eng' : 'Kor'}
                    />
                  )
                })()}
              </div>
            )}
          </div>
        </div>
      </div>
    )
  }
)

export default ExpressionCard
