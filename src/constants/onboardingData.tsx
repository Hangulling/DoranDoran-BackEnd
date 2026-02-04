import React from 'react'
import Slide1 from '../assets/onboarding/slide1.png'
import Slide2 from '../assets/onboarding/slide2.png'
import Slide3 from '../assets/onboarding/slide3.png'
import Slide4 from '../assets/onboarding/slide4.png'

export interface OnboardingSlide {
  id: number
  title: React.ReactNode
  desc: string
  image: string
}

export const ONBOARDING_SLIDES: OnboardingSlide[] = [
  {
    id: 0,
    title: 'Choose a Partner',
    desc: 'Pick who you want to chat with.',
    image: Slide1,
  },
  {
    id: 1,
    title: 'Set Closeness',
    desc: 'Adjust your tone before chatting.',
    image: Slide2,
  },
  {
    id: 2,
    title: 'Start Chatting',
    desc: 'Chat naturally based on closeness.',
    image: Slide3,
  },
  {
    id: 3,
    title: 'Review Phrases',
    desc: 'Save and learn useful expressions.',
    image: Slide4,
  },
]
