import { Suspense } from 'react'
import { Routes } from './router/routes'
import LoadingSpinner from './components/common/LoadingSpinner'
import AppLayout from './layouts/AppLayout'
import { Toaster } from 'react-hot-toast'
// import { useIsOpenKeyboard } from './hooks/useIsOpenKeyboard'

function App() {
  // const { isOpen, viewportHeight } = useIsOpenKeyboard()

  // useEffect(() => {
  //   if (isOpen) {
  //     document.body.style.height = `${viewportHeight}px`
  //   } else {
  //     document.body.style.height = '100dvh'
  //   }
  // }, [isOpen, viewportHeight])

  return (
    <div className="relative flex flex-col h-dvh mx-auto w-full max-w-md bg-white">
      <Toaster position="bottom-center" />
      <AppLayout>
        <Suspense fallback={<LoadingSpinner />}>
          <Routes />
        </Suspense>
      </AppLayout>
    </div>
  )
}

export default App
