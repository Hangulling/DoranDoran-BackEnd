import type { SlideData } from '../../constants/landingData'

interface Props {
  slide: SlideData
}

export default function LandingContent({ slide }: Props) {
  return (
    <div className="flex flex-col h-full">
      {/* 타이틀 */}
      <h2 className="text-[24px] leading-[1.3] tracking-[-0.5px] text-title mb-3">{slide.title}</h2>

      {/* 설명 */}
      <p className="text-gray-300 text-[14px] whitespace-pre-line min-h-[21px]">{slide.desc}</p>

      {/* 이미지 */}
      <div className="flex-1 flex items-center justify-center pb-1">
        {
          <img
            src={slide.image}
            alt="Onboarding Illustration"
            className="w-full h-full object-contain"
          />
        }
      </div>
    </div>
  )
}
