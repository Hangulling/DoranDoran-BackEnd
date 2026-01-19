import { lazy } from 'react'
import { Navigate, Route, Routes as RouterRoutes } from 'react-router-dom'
import AdminPrivateRoute from './AdminPrivateRoute'

const AdminLoginPage = lazy(() => import('../pages/admin/AdminLoginPage'))
const AdminPage = lazy(() => import('../pages/admin/AdminPage'))
const ChatLogsUserHistoryPage = lazy(() => import('../pages/admin/ChatLogsUserHistoryPage'))
const ChatLogsManagementNeededPage = lazy(() => import('../pages/admin/ChatLogsManagementNeededPage'))
const PromptsTestAndApplyPage = lazy(() => import('../pages/admin/PromptsTestAndApplyPage'))
const PromptVersionsPage = lazy(() => import('../pages/admin/PromptVersionsPage'))
const HistoryPage = lazy(() => import('../pages/admin/HistoryPage'))

export function Routes() {
  return (
    <RouterRoutes>
      <Route path="/login" element={<AdminLoginPage />} />
      <Route
        path="/"
        element={
          <AdminPrivateRoute>
            <AdminPage />
          </AdminPrivateRoute>
        }
      />
      <Route
        path="/chat-logs/user-history"
        element={
          <AdminPrivateRoute>
            <ChatLogsUserHistoryPage />
          </AdminPrivateRoute>
        }
      />
      <Route
        path="/chat-logs/management-needed"
        element={
          <AdminPrivateRoute>
            <ChatLogsManagementNeededPage />
          </AdminPrivateRoute>
        }
      />
      <Route
        path="/prompts/test-and-apply"
        element={
          <AdminPrivateRoute>
            <PromptsTestAndApplyPage />
          </AdminPrivateRoute>
        }
      />
      <Route
        path="/prompts/versions"
        element={
          <AdminPrivateRoute>
            <PromptVersionsPage />
          </AdminPrivateRoute>
        }
      />
      <Route
        path="/history"
        element={
          <AdminPrivateRoute>
            <HistoryPage />
          </AdminPrivateRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </RouterRoutes>
  )
}
