import { useState, useEffect, useCallback } from 'react'

const useTTS = (text: string) => {
  const [playing, setPlaying] = useState(false)
  const [koreanVoice, setKoreanVoice] = useState<SpeechSynthesisVoice | null>(null)

  const loadVoices = () => {
    const voices = window.speechSynthesis.getVoices()
    if (voices.length > 0) {
      // 'Google 한국의' 음성 우선 로드
      let selectedVoice = voices.find(v => v.name.includes('Google') && v.lang.startsWith('ko'))
      if (!selectedVoice) {
        selectedVoice = voices.find(v => v.lang.startsWith('ko'))
      }
      setKoreanVoice(selectedVoice || null)
    }
  }

  useEffect(() => {
    loadVoices()
    window.speechSynthesis.addEventListener('voiceschanged', loadVoices)

    return () => {
      window.speechSynthesis.removeEventListener('voiceschanged', loadVoices)
      if (window.speechSynthesis.speaking) {
        window.speechSynthesis.cancel()
      }
    }
  }, [])

  const play = useCallback(() => {
    if (!text || !koreanVoice) {
      console.error('사용 가능한 한국어 음성을 찾지 못했습니다.')
      return
    }

    // 중복 재생 방지
    if (playing) {
      return
    }
    // 음성 설정 나중에
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.voice = koreanVoice
    utterance.lang = 'ko-KR'
    utterance.pitch = 1
    utterance.rate = 1
    utterance.volume = 1

    utterance.onstart = () => setPlaying(true)
    utterance.onend = () => setPlaying(false)
    utterance.onerror = e => {
      console.error('TTS 재생 중 오류가 발생했습니다.', e)
      setPlaying(false)
    }

    window.speechSynthesis.speak(utterance)
  }, [text, koreanVoice, playing])

  return { onPlay: play, playing }
}

export default useTTS
