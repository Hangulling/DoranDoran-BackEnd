import { lazy } from 'react'
import { Route, Routes as RouterRoutes } from 'react-router-dom'

const MainPage = lazy(() => import('../pages/MainPage'))
const SignupPage = lazy(() => import('../pages/SignupPage'))
const LoginPage = lazy(() => import('../pages/LoginPage'))
const ClosenessPage = lazy(() => import('../pages/ClosenessPage'))
const ChatPage = lazy(() => import('../pages/ChatPage'))
const ArchivePage = lazy(() => import('../pages/ArchivePage'))
const PolicyPage = lazy(() => import('../pages/PolicyPage'))
const ErrorPage = lazy(() => import('../pages/ErrorPage'))

export function Routes() {
  return (
    <RouterRoutes>
      <Route path="/" element={<MainPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/closeness/:id" element={<ClosenessPage />} />
      <Route path="/chat/:id" element={<ChatPage />} />
      <Route path="/archive/:id" element={<ArchivePage />} />
      <Route path="/policy/:id" element={<PolicyPage />} />
      <Route path="/error" element={<ErrorPage errorCode={500} />} />
      <Route path="*" element={<ErrorPage errorCode={404} />} />
    </RouterRoutes>
  )
}
