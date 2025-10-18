import React from 'react'
import Arrow1 from '../../assets/chat/arrow1.svg'
import Arrow2 from '../../assets/chat/arrow2.svg'
import Example from '../../assets/chat/exampleChat.svg'
import CloseIcon from '../../assets/chat/close.svg?react'

const stepData = {
  holeStyle: {
    top: '5px',
    right: '10px',
    width: '50px',
    height: '50px',
    borderRadius: '100px',
  },
  content: (
    <>
      <div className="absolute top-[24px] right-[85px] w-[157px] flex flex-col items-center text-center text-subtitle text-[16px]">
        <div className=" bg-green-400 text-[12px] px-[10px] py-[1px] rounded-[10px] mb-[6px]">
          2
        </div>
        <p>
          <span className="text-green-300">Check</span> your saved expressions.
        </p>
      </div>
      <img src={Arrow1} alt="첫번째 화살표" className="absolute top-[60px] right-[45.81px]" />
      <div className="absolute top-[257px] left-[75px] flex flex-col items-center text-center text-subtitle text-[16px]">
        <div className=" bg-green-400 text-[12px] px-[10px] py-[1px] rounded-[10px] mb-[6px]">
          1
        </div>
        <p>
          <span className="text-green-300">Save</span> this expression.
        </p>
      </div>
      <img src={Arrow2} alt="두번째 화살표" className="absolute top-[202.33px] left-[156px]" />
      <img src={Example} alt="예시 채팅" className="absolute top-[133px] left-15" />
    </>
  ),
}

interface CoachMarkProps {
  show: boolean
  onClose: () => void
}

const CoachMark: React.FC<CoachMarkProps> = ({ show, onClose }) => {
  if (!show) return null

  return (
    <div className="fixed inset-0 z-50 cursor-pointer" onClick={onClose}>
      <div className="relative mx-auto w-full max-w-md min-h-dvh overflow-hidden pointer-events-none">
        {/* 투명 영역 */}
        <div
          style={stepData.holeStyle}
          className="absolute transition-all duration-300 ease-in-out shadow-[0_0_0_9999px_rgba(0,0,0,0.8)] pointer-events-none"
        />

        {/* 안내 UI */}
        <div className="absolute w-full h-full text-white pointer-events-none">
          {stepData.content}
        </div>

        {/* 닫기 버튼 */}
        <button
          onClick={e => {
            e.stopPropagation()
            onClose()
          }}
          className="absolute top-[17px] left-[20px] pointer-events-auto"
        >
          <CloseIcon />
        </button>
      </div>
    </div>
  )
}

export default CoachMark
