import { Suspense } from 'react'
import { Routes } from './router/routes'
import LoadingSpinner from './components/common/LoadingSpinner'
import { Toaster } from 'react-hot-toast'

function App() {
  return (
    <div className="relative flex flex-col h-screen w-full bg-white">
      <Toaster position="top-right" />
      <Suspense fallback={<LoadingSpinner />}>
        <Routes />
      </Suspense>
    </div>
  )
}

export default App
