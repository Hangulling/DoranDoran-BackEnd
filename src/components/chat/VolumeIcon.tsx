import React from 'react'
import VolumeIcon from '../../assets/icon/defaultVolume.svg?react'
import ActiveVolumeIcon from '../../assets/icon/activeVolume.svg?react'

interface TTSIconProps {
  onPlay: () => void
  playing: boolean
}

const TTSIcon: React.FC<TTSIconProps> = ({ onPlay, playing }) => {
  return (
    <button onClick={onPlay} className="focus:outline-none" aria-label="음성 재생">
      {playing ? <ActiveVolumeIcon /> : <VolumeIcon />}
    </button>
  )
}

export default TTSIcon
