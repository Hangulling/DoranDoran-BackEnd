import type { ReactNode } from 'react'
import AdminSidebar from './AdminSidebar'

type AdminLayoutProps = {
  children: ReactNode
}

export default function AdminLayout({ children }: AdminLayoutProps) {
  return (
    <div className="flex h-screen bg-gray-50 w-full">
      {/* 좌측 사이드바 */}
      <AdminSidebar />
      
      {/* 메인 콘텐츠 영역 */}
      <main className="flex-1 overflow-y-auto bg-gray-50 w-full">
        {children}
      </main>
    </div>
  )
}
