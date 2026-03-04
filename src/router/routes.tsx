import { lazy } from 'react'
import { Route, Routes as RouterRoutes } from 'react-router-dom'
import PrivateRoute from './PrivateRoute'

const MainPage = lazy(() => import('../pages/MainPage'))
const SignupPage = lazy(() => import('../pages/SignupPage'))
const LoginPage = lazy(() => import('../pages/LoginPage'))
const ClosenessPage = lazy(() => import('../pages/ClosenessPage'))
const ChatPage = lazy(() => import('../pages/ChatPage'))
const ArchivePage = lazy(() => import('../pages/ArchivePage'))
const PolicyPage = lazy(() => import('../pages/PolicyPage'))
const ErrorPage = lazy(() => import('../pages/ErrorPage'))
const TestMainPage = lazy(() => import('../test/TestMainPage'))
const TestClosenessPage = lazy(() => import('../test/TestClosenessPage'))
const TestChatPage = lazy(() => import('../test/TestChatPage'))
const LandingPage = lazy(() => import('../pages/LandingPage'))
const OnboardingPage = lazy(() => import('../pages/OnboardingPage'))

export function Routes() {
  return (
    <RouterRoutes>
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/insta" element={<LandingPage />} />
      {/* Test routes */}
      <Route
        path="/test/chat/:model"
        element={
          <PrivateRoute>
            <TestMainPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/test/closeness/:model/:id"
        element={
          <PrivateRoute>
            <TestClosenessPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/test/chat/:model/:id"
        element={
          <PrivateRoute>
            <TestChatPage />
          </PrivateRoute>
        }
      />

      <Route
        path="/"
        element={
          <PrivateRoute>
            <MainPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/closeness/:id"
        element={
          <PrivateRoute>
            <ClosenessPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/chat/:id"
        element={
          <PrivateRoute>
            <ChatPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/archive/:id"
        element={
          <PrivateRoute>
            <ArchivePage />
          </PrivateRoute>
        }
      />
      <Route
        path="/onboarding"
        element={
          <PrivateRoute>
            <OnboardingPage />
          </PrivateRoute>
        }
      />
      <Route path="/policy/:id" element={<PolicyPage />} />
      <Route path="/error" element={<ErrorPage />} />
      <Route path="*" element={<ErrorPage errorCode={404} />} />
    </RouterRoutes>
  )
}
