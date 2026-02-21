import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAdminUser } from '../../hooks/useAdminUser'
import { adminLogout } from '../../api/admin/auth'

type MenuItem = {
  label: string
  path: string
  children?: MenuItem[]
}

type MenuSection = {
  title: string
  items: MenuItem[]
}

const menuSections: MenuSection[] = [
  // TODO : 중복 확인 필요
  {
    title: '채팅 로그 조회',
    items: [
      { label: '사용자 채팅 내역', path: '/chat-logs/user-history' },
      { label: '관리 필요 내역', path: '/chat-logs/management-needed' },
    ],
  },
  {
    title: '프롬프트 관리',
    items: [
      { label: '프롬프트 테스트 및 적용', path: '/prompts/test-and-apply' },
      { label: '프롬프트 버전 관리', path: '/prompts/versions' },
    ],
  },
  {
    title: '히스토리 관리',
    items: [
      { label: '관리 이력 조회', path: '/history' },
    ],
  },
  // ===== 신규 구현 (아카이브 DB 기반) =====
  {
    title: '채팅 로그 조회',  // ✅ 사용자 관점 이름
    items: [
      { label: '채팅 로그 조회', path: '/my/chat-logs' },
      { label: '관리 필요 내역', path: '/my/management-queue' },
      { label: '감사 로그', path: '/my/audit-logs' },
    ],
  },
]

export default function AdminSidebar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAdminUser()
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    new Set(['채팅 로그 조회']) // 기본으로 첫 번째 섹션 펼침
  )

  // 사용자 정보 (인증된 사용자 또는 로딩 중)
  const userName = user?.name || '로딩 중...'
  const adminId = user?.id || '로딩 중...'

  const toggleSection = (title: string) => {
    setExpandedSections(prev => {
      const next = new Set(prev)
      if (next.has(title)) {
        next.delete(title)
      } else {
        next.add(title)
      }
      return next
    })
  }

  const isActive = (path: string) => {
    return location.pathname === path
  }

  const handleLogout = async () => {
    try {
      await adminLogout()
    } catch (error) {
      console.error('로그아웃 중 오류 발생:', error)
    } finally {
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="w-72 h-screen bg-white border-r border-gray-200 flex flex-col shadow-sm">
      {/* 헤더 */}
      <div className="p-6 border-b border-gray-200 bg-gray-50">
        <h1 className="text-2xl font-bold text-gray-800">Koach Admin</h1>
      </div>

      {/* 사용자 정보 */}
      <div className="p-6 border-b border-gray-200">
        <div className="mb-4">
          <p className="text-base font-semibold text-gray-800 mb-1">{userName}님</p>
          <p className="text-sm text-gray-500">관리자 ID: {adminId}</p>
        </div>
        <button
          onClick={handleLogout}
          className="w-full py-3 px-4 bg-blue-600 text-white text-base font-medium rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
        >
          로그아웃
        </button>
      </div>

      {/* 메뉴 */}
      <div className="flex-1 overflow-y-auto">
        {menuSections.map(section => {
          const isExpanded = expandedSections.has(section.title)
          return (
            <div key={section.title} className="border-b border-gray-100">
              <button
                onClick={() => toggleSection(section.title)}
                className="w-full px-6 py-4 text-left flex items-center justify-between hover:bg-gray-50 transition-colors"
              >
                <span className="text-base font-semibold text-gray-700">{section.title}</span>
                <span className={`text-gray-400 transition-transform text-sm ${isExpanded ? 'rotate-90' : ''}`}>
                  ▼
                </span>
              </button>
              {isExpanded && (
                <div className="bg-gray-50">
                  {section.items.map(item => {
                    const active = isActive(item.path)
                    return (
                      <button
                        key={item.path}
                        onClick={() => navigate(item.path)}
                        className={`w-full px-8 py-3 text-left text-base transition-colors ${
                          active
                            ? 'bg-blue-600 text-white font-medium'
                            : 'text-gray-700 hover:bg-gray-100'
                        }`}
                      >
                        {item.label}
                      </button>
                    )
                  })}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
