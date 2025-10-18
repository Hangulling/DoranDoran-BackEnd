import React, { useState } from 'react'
import useTTS from '../../hooks/useTTS'
import TTSIcon from './VolumeIcon'
import useArchiveStore from '../../stores/useArchiveStore'

type VARIANT = 'chat' | 'archive'

const VARIANTS: Record<VARIANT, string> = {
  chat: 'ml-10 bg-white border border-gray-100 rounded-lg px-[10px] py-[10px] max-w-[265px] mb-2',
  archive: 'ml-0 bg-white rounded-lg px-4 pb-[10px] w-[330px] mb-2',
}

interface DescriptionBubbleProps {
  word: string
  pronunciation: string
  initialTab?: string
  descriptionByTab: Record<string, string>
  variant?: VARIANT
  isSelected?: boolean
}

const DescriptionBubble: React.FC<DescriptionBubbleProps> = ({
  word,
  pronunciation,
  initialTab = 'Eng',
  descriptionByTab,
  variant = 'chat',
  isSelected,
}) => {
  const [selectedTab, setSelectedTab] = useState(initialTab)
  const tabs = ['Kor', 'Eng']
  const { selectionMode } = useArchiveStore()
  const { onPlay: playTTS, playing } = useTTS(word)

  const containerClass = [
    VARIANTS[variant],
    variant === 'archive' && (selectionMode ? isSelected : isSelected) ? '!bg-green-50 ' : '',
  ].join(' ')

  return (
    <div className={containerClass}>
      <div className="flex items-center justify-between text-[12px] mb-1">
        <div className="flex items-center">
          <div className="mr-1 h-5 w-5">
            <TTSIcon playing={playing} onPlay={playTTS} />
          </div>
          <span className="text-title">{`‘${word}’`}</span>
          {pronunciation && <span className="ml-2 text-gray-400">[{pronunciation}]</span>}
        </div>
        <div className="flex p-0.5 bg-gray-80 rounded-[6px]">
          {tabs.map(tab => (
            <button
              key={tab}
              className={
                selectedTab === tab
                  ? 'px-1 py-0.5 rounded-[4px] bg-white text-gray-800 text-subtitle shadow-none'
                  : 'px-1 py-0.5 rounded-[4px] bg-gray-80 text-gray-300 text-subtitle shadow-none'
              }
              style={{ border: 'none' }}
              onClick={() => setSelectedTab(tab)}
              type="button"
            >
              {tab}
            </button>
          ))}
        </div>
      </div>
      {variant === 'chat' && <div className="h-[1px] bg-gray-80 w-full my-2" />}
      <div className="text-[14px] text-gray-700">{descriptionByTab[selectedTab]}</div>
    </div>
  )
}

export default DescriptionBubble
