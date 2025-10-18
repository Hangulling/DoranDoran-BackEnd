import React from 'react'
import { useModalStore } from '../../stores/useUiStateStore'
import CommonModal from '../common/CommonModal'
import CheckIcon from '../../assets/icon/checkAll.svg'
import DisabledCheckIcon from '../../assets/icon/disabledCheckAll.svg'

interface Props {
  open: boolean
  onConfirm: () => void
  onCancel: () => void
}

const ExitModal: React.FC<Props> = ({ open, onConfirm, onCancel }) => {
  const { noShowAgain, setNoShowAgain } = useModalStore()
  const [checked, setChecked] = React.useState(noShowAgain)

  React.useEffect(() => {
    setChecked(noShowAgain)
  }, [noShowAgain])

  if (!open) return null

  const verticalOffset = 125
  const translateXpx = window.innerWidth / 2 - 151.5

  return (
    <>
      {/* 모달 */}
      <CommonModal
        open={open}
        title="Leave chat room"
        description={[
          'If you leave, the chat',
          'history will be deleted.',
          'Are you sure you want to leave?',
        ]}
        confirmText="Leave"
        cancelText="Keep"
        onConfirm={() => {
          setNoShowAgain(checked)
          onConfirm()
        }}
        onCancel={onCancel}
      />

      {/* 다시 보지 않기 */}
      <div className="fixed w-full inset-x-0 top-1/2 z-[1001] -translate-y-1/2 pointer-events-none">
        <div
          className="w-[149px]"
          style={{
            transform: `translateX(${translateXpx}px) translateY(${verticalOffset}px)`,
          }}
        >
          <label className="flex items-center gap-2 text-[14px] text-white pointer-events-auto w-fit">
            <input
              type="checkbox"
              id="noShowAgainCheck"
              className="hidden"
              checked={checked}
              onChange={e => setChecked(e.target.checked)}
            />
            <img
              src={checked ? CheckIcon : DisabledCheckIcon}
              alt={checked ? 'checked' : 'unchecked'}
            />
            <span>don't watch again</span>
          </label>
        </div>
      </div>
    </>
  )
}

export default ExitModal
