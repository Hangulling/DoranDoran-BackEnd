import React, { useState } from 'react'
import useClosenessStore from '../../stores/useClosenessStore'

interface CorrectionBubbleProps {
  chatRoomId: string
  initialTab?: string
  descriptionByTab: Record<string, string>
}

const bubbleClass = 'bg-green-50 rounded-lg px-[10px] py-[10px] max-w-[265px] mb-2'

const CorrectionBubble: React.FC<CorrectionBubbleProps> = ({
  chatRoomId,
  initialTab = 'Eng',
  descriptionByTab,
}) => {
  const [selectedTab, setSelectedTab] = useState(initialTab)
  const closeness = useClosenessStore(state => state.closenessMap[chatRoomId] ?? 1)
  const closenessText = closeness === 1 ? 'Polite' : closeness === 2 ? 'Casual' : 'Friendly'

  const tabs = ['Kor', 'Eng']

  const wrapperClass = 'chat chat-end'

  return (
    <div className={wrapperClass}>
      <div className={bubbleClass}>
        <div className="flex items-center justify-between text-[12px] mb-1">
          <div className="flex flex-col space-y-3 items-center text-title">
            <p className="text-green-500">
              closeness level -<span> {closenessText}</span>
            </p>
            <p>교정문구</p>
          </div>
          <div className="flex p-0.5 bg-green-80 rounded-[6px]">
            {tabs.map(tab => (
              <button
                key={tab}
                className={
                  selectedTab === tab
                    ? 'px-1 py-0.5 rounded-[4px] bg-white text-green-400 text-subtitle shadow-none'
                    : 'px-1 py-0.5 rounded-[4px] bg-green-80 text-gray-300 text-subtitle shadow-none'
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
        <div className="h-[1px] bg-green-80 w-full my-2" />
        <div className="text-[14px] text-gray-700">{descriptionByTab[selectedTab]}</div>
      </div>
    </div>
  )
}

export default CorrectionBubble
