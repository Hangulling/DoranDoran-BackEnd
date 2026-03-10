import useArchiveStore from '../../stores/useArchiveStore'
import type { Room } from '../../types/archive'
import Button from '../common/Button'

interface ArchiveTabsProps {
  activeTab: Room
  onChange?: (tab: Room) => void
}

export default function ArchiveTabs({ activeTab, onChange }: ArchiveTabsProps) {
  const { selectionMode } = useArchiveStore()
  const tabs: Room[] = ['Friend', 'Honey', 'Coworker', 'Senior']

  return (
    <div className="flex justify-center md:justify-start md:px-8">
      <div className="flex justify-between gap-2 md:gap-6">
        {tabs.map(tab => {
          const isActive = activeTab === tab
          const disabled = selectionMode && !isActive
          return (
            <Button
              key={tab}
              disabled={disabled}
              className={`my-4 ${isActive ? 'bg-green-400 text-white' : 'bg-transparent text-gray-600 border border-gray-100'}  ${disabled && '!text-gray-200'} `}
              variant="tab"
              size="archive"
              onClick={() => onChange?.(tab)}
            >
              {tab}
            </Button>
          )
        })}
      </div>
    </div>
  )
}
