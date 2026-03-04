import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { SLIDES, slideVariants } from '../constants/landingData'
import { useSlider } from '../hooks/useSlider'
import GoogleAuth from '../components/landing/GoogleAuth'
import LandingContent from '../components/landing/LandingContent'

export default function LandingPage() {
  const navigate = useNavigate()
  const { page, direction, handleScreenClick, handleDragEnd } = useSlider(SLIDES.length)

  return (
    <div
      className="flex flex-col h-[100dvh] bg-white relative overflow-hidden"
      onClick={handleScreenClick}
    >
      <div className="px-5 my-[30px] flex justify-between items-center z-20">
        {/* 인디케이터 */}
        <div className="flex gap-2">
          {SLIDES.map((_, index) => (
            <div
              key={index}
              className={`h-[10px] w-[10px] rounded-full transition-all duration-300 ${
                index === page ? 'bg-green-500' : 'bg-gray-80'
              }`}
            />
          ))}
        </div>

        <button
          onClick={e => {
            e.stopPropagation()
            navigate('/login')
          }}
          className="py-1 px-3 bg-gray-30 text-gray-400 text-[16px] rounded-full transition-transform"
        >
          login
        </button>
      </div>

      <div className="flex-1 relative w-full">
        <AnimatePresence initial={false} custom={direction} mode="wait">
          <motion.div
            key={page}
            custom={direction}
            variants={slideVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.3, ease: 'easeInOut' }}
            className="absolute inset-0 px-5"
            drag="x"
            dragConstraints={{ left: 0, right: 0 }}
            dragElastic={0.2}
            onDragEnd={handleDragEnd}
          >
            <LandingContent slide={SLIDES[page]} />
          </motion.div>
        </AnimatePresence>
      </div>

      {/* 버튼 */}
      <div className="px-5 pb-4 w-full z-30 bg-white">
        <div className="flex flex-col gap-4">
          <button
            onClick={e => {
              e.stopPropagation()
              navigate('/signup')
            }}
            className="w-full bg-green-400 text-white text-subtitle text-[16px] py-4 rounded-[8px] transition-transform"
          >
            Sign up
          </button>

          <div onClick={e => e.stopPropagation()}>
            <GoogleAuth />
          </div>
        </div>
      </div>
    </div>
  )
}
