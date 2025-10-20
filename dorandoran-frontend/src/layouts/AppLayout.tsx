import type React from 'react'
import { useLocation, useMatch } from 'react-router-dom'
import NavBar from '../components/common/NavBar'
import useArchiveStore from '../stores/useArchiveStore'
import ClosenessBar from '../components/chat/ClosenessBar'
import { useState } from 'react'
import Sidebar from '../components/common/SideBar'

interface AppLayoutProps {
  children: React.ReactNode
}

const pageTitles: Record<string, string> = {
  '/': '',
  '/signup': 'Sign Up',
  '/archive': 'Archive',
}

const chatRoomNames: Record<string, string> = {
  '1': 'Friend',
  '2': 'Honey',
  '3': 'Coworker',
  '4': 'Senior',
}

const agreementTitles: Record<string, string> = {
  service: 'Terms of Service',
  privacy: 'Privacy Policy',
}

const showBookmarkPaths = ['/']

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const toggleSidebar = () => setSidebarOpen(open => !open)
  const location = useLocation()
  const pathname = location.pathname

  // 알 수 없는 페이지 (*)
  const isUnknownPath = !(
    pathname === '/' ||
    pathname.startsWith('/signup') ||
    pathname.startsWith('/archive') ||
    pathname.startsWith('/chat') ||
    pathname.startsWith('/closeness') ||
    pathname.startsWith('/policy') ||
    pathname.startsWith('/login') ||
    pathname.startsWith('/error')
  )
  const skipNavPaths = ['/login', '/error']

  const isMain = pathname === '/'

  const archiveMatch = useMatch('/archive/:id')
  const onArchive = !!archiveMatch
  const archiveId = archiveMatch?.params.id
  const agreementMatch = useMatch('/policy/:id')
  const agreementId = agreementMatch?.params.id

  const showDelete = onArchive
  const hideNavBar = skipNavPaths.includes(pathname) || isUnknownPath

  // 친밀도 바(채팅에서만)
  const closenessMatch = pathname.match(/^\/chat\/(\d+)$/)
  const closenessId = closenessMatch ? closenessMatch[1] : null

  // 북마크(채팅/친밀)
  const chatRoomMatch = pathname.match(/^\/(chat|closeness)\/(\d+)$/)
  const chatRoomId = chatRoomMatch ? chatRoomMatch[2] : null
  const showBookmark = showBookmarkPaths.includes(pathname) || chatRoomId !== null

  const fromChat = (location.state as { from?: string } | null)?.from === 'chat'

  const { selectionMode } = useArchiveStore()

  // 타이틀
  let title = ''
  if (selectionMode) {
    title = 'Delete'
  } else if (onArchive) {
    title =
      (fromChat && archiveId && (chatRoomNames[archiveId] || `채팅방 ${archiveId}`)) || 'Archive'
  } else if (chatRoomId) {
    title = chatRoomNames[chatRoomId] || `채팅방 ${chatRoomId}`
  } else if (agreementId) {
    title = agreementTitles[agreementId]
  } else {
    title = pageTitles[pathname] || '페이지'
  }

  return (
    <div className="relative mx-auto flex h-full w-full max-w-md flex-col">
      <Sidebar isOpen={sidebarOpen} onClose={toggleSidebar} />

      {!hideNavBar && (
        <header className="sticky top-0 shrink-0 z-40 bg-white">
          <NavBar
            isMain={isMain}
            title={title}
            showBookmark={showBookmark}
            showDelete={showDelete}
            onToggleSidebar={toggleSidebar}
          />
          {closenessId && <ClosenessBar chatRoomId={closenessId} />}
        </header>
      )}
      <main className="flex-grow min-h-0 overflow-y-auto">{children}</main>
    </div>
  )
}

export default AppLayout
