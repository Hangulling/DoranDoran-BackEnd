import type { LoadingSpinnerProps } from '../../types/common'

export default function LoadingSpinner({ message }: LoadingSpinnerProps) {
  return (
    <div className="flex flex-col justify-center items-center h-64">
      <span className="loading loading-spinner loading-lg"></span>
      {message && <p className="mt-4 text-sm text-gray-600 text-center">{message}</p>}
    </div>
  )
}
