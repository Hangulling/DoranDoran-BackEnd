import React from 'react'
import Slide1 from '../assets/landing/slide1.png'
import Slide2 from '../assets/landing/slide2.png'
import Slide3 from '../assets/landing/slide3.png'

export interface SlideData {
  id: number
  title: React.ReactNode
  desc?: string
  image: string
}

export const SLIDES: SlideData[] = [
  {
    id: 0,
    title: (
      <>
        Are you still learning
        <br />
        Korean from <span className="text-green-500">textbooks?</span>
      </>
    ),
    desc: 'Learn real-life Korean through real chats.',
    image: Slide1,
  },
  {
    id: 1,
    title: (
      <>
        We teach you based on
        <br />
        your chosen <span className="text-green-500">closeness level.</span>
      </>
    ),
    desc: 'Adjust your tone with the intimacy slider.',
    image: Slide2,
  },
  {
    id: 2,
    title: (
      <>
        Sign up just in a minute!
        <br />
        <span className="text-green-500">Get started for free.</span>
      </>
    ),
    image: Slide3,
  },
]

export const slideVariants = {
  enter: (direction: number) => ({ x: direction > 0 ? 100 : -100, opacity: 0 }),
  center: { zIndex: 1, x: 0, opacity: 1 },
  exit: (direction: number) => ({ zIndex: 0, x: direction < 0 ? 100 : -100, opacity: 0 }),
}
