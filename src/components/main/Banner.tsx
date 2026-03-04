import Character from '../../assets/main/mainCharacter.svg'
import { capitalizeName } from '../../utils/capitalizeFirstLetter'

interface BannerProps {
  userName: string
}

const Banner = ({ userName }: BannerProps) => {
  return (
    <div className="w-full bg-[#9ADAD5] h-[99px] relative max-w-md mx-auto overflow-hidden">
      <div className="absolute top-[14px] left-[20px]">
        <div className="text-[14px]">Welcome,</div>
        <div className="text-[16px]">
          <span className="text-title">{capitalizeName(userName)}</span>
          <span> :)</span>
        </div>
        <p className="mt-[8px] text-[12px] text-gray-600">Learn Korean expressions by chat!</p>
      </div>
      <img src={Character} alt="캐릭터 이미지" className="absolute top-[23px] right-[20.16px]" />
    </div>
  )
}

export default Banner
