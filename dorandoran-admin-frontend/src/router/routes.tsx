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

// 추가
const MyChatLogListPage = lazy(() => import('../pages/admin/MyChatLogListPage'))
const MyChatLogDetailPage = lazy(() => import('../pages/admin/MyChatLogDetailPage'))
const MyManagementQueuePage = lazy(() => import('../pages/admin/MyManagementQueuePage'))
const MyAuditLogPage = lazy(() => import('../pages/admin/MyAuditLogPage'))

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

      <Route
        path="/my/chat-logs"
        element={
          <AdminPrivateRoute>
            <MyChatLogListPage />
          </AdminPrivateRoute>
        }
      />
      <Route
        path="/my/chat-logs/:chatroomId"
        element={
          <AdminPrivateRoute>
            <MyChatLogDetailPage />
          </AdminPrivateRoute>
        }
      />
      <Route
        path="/my/management-queue"
        element={
          <AdminPrivateRoute>
            <MyManagementQueuePage />
          </AdminPrivateRoute>
        }
      />
      <Route
        path="/my/audit-logs"
        element={
          <AdminPrivateRoute>
            <MyAuditLogPage />
          </AdminPrivateRoute>
        }
      />


      <Route path="*" element={<Navigate to="/" replace />} />
    </RouterRoutes>
  )
}
