import Slider from '@mui/material/Slider'
import { styled } from '@mui/material/styles'

interface DistanceSliderProps {
  value: number
  onChange: (newValue: number) => void
  roomId: number
}

const marks = [
  { value: 1, label: 'Polite' },
  { value: 3, label: 'Friendly' },
]

const thumbSize = 28

const ClosenessSlider = styled(Slider)({
  height: 10,
  color: '#FF3314',
  '& .MuiSlider-thumb': {
    width: thumbSize,
    height: thumbSize,
    background: 'url(/chat/heart.svg) center/contain no-repeat',
    border: 'none',
    backgroundColor: 'transparent',
    boxShadow: '0.4px 1px 2px rgba(0, 0, 0, 0.3)',
    '&:before, &:after': {
      display: 'none',
    },
    '&:hover, &.Mui-focusVisible, &.Mui-active': {
      boxShadow: '0.4px 1px 2px rgba(0, 0, 0, 0.3)',
    },
  },
  '& .MuiSlider-track': {
    border: 'none',
    background: 'linear-gradient(to right, #FFB98A 0%, #FF583E 60%, #E23A20 100%)',
    boxShadow: 'inset -1px -1px 4px rgba(0,0,0,0.15), inset 3px 3px 12px rgba(255,255,255,0.4)',
  },
  '& .MuiSlider-rail': {
    backgroundColor: '#EAEBEB',
  },
  '& .MuiSlider-mark': {
    backgroundColor: 'transparent',
  },
  '& .MuiSlider-markLabel': {
    marginTop: '4px',
    color: '#A6ABAA',
    '&[data-index="0"]': { transform: 'translateX(0%)' },
    '&[data-index="1"]': { transform: 'translateX(-50%)' },
    '&[data-index="2"]': { transform: 'translateX(-100%)' },
  },
})

const DistanceSlider: React.FC<DistanceSliderProps> = ({ value, onChange }) => {
  const step = 2

  const handleChange = (_: Event, newValue: number | number[]) => {
    const val = (newValue as number) < 2 ? 1 : 3
    onChange(val)
  }

  const handleChangeCommitted = (_: React.SyntheticEvent | Event, newValue: number | number[]) => {
    const val = (newValue as number) < 2 ? 1 : 3
    onChange(val)
  }

  // 각 위치별 thumb 정렬 방식
  const thumbSX =
    Math.round(value) === 1
      ? { left: 0, transform: 'translateY(-50%)', position: 'absolute' }
      : { left: '100%', transform: 'translate(-95%, -50%)', position: 'absolute' }

  return (
    <div
      style={{
        width: '100%',
        padding: 0,
        boxSizing: 'border-box',
      }}
    >
      <ClosenessSlider
        value={value}
        min={1}
        max={3}
        step={step}
        marks={marks}
        onChange={handleChange}
        onChangeCommitted={handleChangeCommitted}
        valueLabelDisplay="off"
        sx={{
          '& .MuiSlider-thumb': thumbSX,
          '& .MuiSlider-markLabel': {
            color: '#A6ABAA',
            '&[data-index="0"]': { transform: 'translateX(0%)' },
            '&[data-index="1"]': { transform: 'translateX(-100%)' },
          },
          [`& .MuiSlider-markLabel[data-index="${Math.round(value) - 1}"]`]: {
            color: '#282A2A',
          },
        }}
      />
    </div>
  )
}

export default DistanceSlider
