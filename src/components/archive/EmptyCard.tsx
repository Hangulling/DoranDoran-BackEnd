import EmptyBookmark from '../../assets/icon/emptyBookmark.svg?react'

export default function EmptyCard() {
  return (
    <div className="flex flex-col items-center justify-center h-full min-h-full">
      <EmptyBookmark />
      <p className="text-subtitle text-lg text-gray-800 mt-[45px] mb-2">Save Expressions!</p>
      <p className="text-body text-sm text-gray-600">No saved expressions.</p>
      <p className="text-body text-sm text-gray-600">Save useful expressions as you learn.</p>
    </div>
  )
}
