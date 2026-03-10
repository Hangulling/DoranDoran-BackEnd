import type { OnboardingSlide } from '../../constants/onboardingData'

interface Props {
  slide: OnboardingSlide
}

export default function OnboardingContent({ slide }: Props) {
  return (
    <>
      {/* 텍스트 */}
      <div className="text-center mb-4 px-5 shrink-0">
        <h2 className="text-[36px] font-extrabold text-green-500 tracking-[-2px] mb-1">
          {slide.title}
        </h2>
        <p className="text-gray-600 text-[14px]">{slide.desc}</p>
      </div>

      {/* 이미지 */}
      <div className="flex-1 w-full flex items-end justify-center overflow-hidden pb-4">
        <img src={slide.image} alt="Onboarding Screen" className="w-full h-full object-contain" />
      </div>
    </>
  )
}
