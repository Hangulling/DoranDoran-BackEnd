import { useEffect, useState } from 'react'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import { getChatroomOptions, type ChatroomOption } from '../../api/admin'
import {
  getAdminConversations,
  getAdminConversationDetail,
  type AdminConversationDetailResponse,
  type AdminConversationListResponse,
} from '../../api/admin/conversations'

export default function ChatLogsUserHistoryPage() {
  const [dataSource, setDataSource] = useState<'chat' | 'archive'>('chat')
  const [userEmail, setUserEmail] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [roomKey, setRoomKey] = useState('')
  const [intimacyLevel, setIntimacyLevel] = useState<number | ''>('')
  const [chatroomOptions, setChatroomOptions] = useState<ChatroomOption[]>([])
  const [conversations, setConversations] = useState<AdminConversationListResponse | null>(null)
  const [detail, setDetail] = useState<AdminConversationDetailResponse | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    loadChatroomOptions()
  }, [])

  useEffect(() => {
    loadConversations(currentPage)
  }, [currentPage])

  const loadChatroomOptions = async () => {
    try {
      const options = await getChatroomOptions()
      setChatroomOptions(options)
    } catch (error) {
      console.error('채팅룸 옵션 로드 실패:', error)
    }
  }

  const loadConversations = async (page: number) => {
    try {
      const normalizeDateTime = (value: string) =>
        value ? (value.length === 16 ? `${value}:00` : value) : undefined

      setIsLoading(true)
      const response = await getAdminConversations({
        userEmail: userEmail || undefined,
        from: normalizeDateTime(from),
        to: normalizeDateTime(to),
        roomKey: roomKey || undefined,
        intimacyLevel: intimacyLevel === '' ? undefined : intimacyLevel,
        dataSource,
        page,
        size: 20,
      })
      setConversations(response)
    } catch (error) {
      console.error('대화 목록 조회 실패:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleSearch = () => {
    setCurrentPage(0)
    loadConversations(0)
  }

  const handleRowClick = async (conversationId: string) => {
    try {
      setSelectedId(conversationId)
      const response = await getAdminConversationDetail(conversationId, dataSource)
      setDetail(response)
    } catch (error) {
      console.error('대화 상세 조회 실패:', error)
    }
  }

  return (
    <AdminLayout>
      <div className="w-full p-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">사용자 채팅 내역</h1>
          <p className="text-lg text-gray-600">사용자들의 채팅 내역을 조회할 수 있습니다.</p>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <div className="grid grid-cols-6 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">데이터 소스</label>
              <select
                value={dataSource}
                onChange={(e) => setDataSource(e.target.value as 'chat' | 'archive')}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              >
                <option value="chat">운영(chat_schema)</option>
                <option value="archive">보관(archive_schema)</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">사용자 이메일</label>
              <input
                value={userEmail}
                onChange={(e) => setUserEmail(e.target.value)}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
                placeholder="user@example.com"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">시작일</label>
              <input
                type="datetime-local"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">종료일</label>
              <input
                type="datetime-local"
                value={to}
                onChange={(e) => setTo(e.target.value)}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">컨셉</label>
              {dataSource === 'archive' ? (
                <select
                  value={roomKey}
                  onChange={(e) => setRoomKey(e.target.value)}
                  className="w-full h-11 px-3 border border-gray-300 rounded-lg"
                >
                  <option value="">전체</option>
                  {chatroomOptions.map((option) => (
                    <option key={option.id} value={option.concept}>
                      {option.name} ({option.concept})
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  value={roomKey}
                  onChange={(e) => setRoomKey(e.target.value)}
                  className="w-full h-11 px-3 border border-gray-300 rounded-lg"
                  placeholder="friend"
                />
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">친밀도 레벨</label>
              <select
                value={intimacyLevel}
                onChange={(e) => {
                  const value = e.target.value
                  setIntimacyLevel(value === '' ? '' : Number(value))
                }}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              >
                <option value="">전체</option>
                <option value="1">레벨 1</option>
                <option value="3">레벨 3</option>
              </select>
            </div>
          </div>
          <div className="mt-4 flex justify-end">
            <Button onClick={handleSearch} variant="confirm">
              조회하기
            </Button>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">대화 목록</h2>
          {isLoading ? (
            <div className="text-center py-8 text-gray-500">로딩 중...</div>
          ) : conversations && conversations.content.length > 0 ? (
            <>
              <table className="w-full border-collapse">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">대화 ID</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">컨셉</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">친밀도</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">최근 메시지</th>
                  </tr>
                </thead>
                <tbody>
                  {conversations.content.map((conv) => (
                    <tr
                      key={conv.conversationId}
                      className={`border-b border-gray-100 cursor-pointer ${
                        selectedId === conv.conversationId ? 'bg-blue-50' : ''
                      }`}
                      onClick={() => handleRowClick(conv.conversationId)}
                    >
                      <td className="py-3 px-4 text-sm">{conv.conversationId}</td>
                      <td className="py-3 px-4 text-sm">{conv.roomKey || '-'}</td>
                      <td className="py-3 px-4 text-sm">{conv.intimacyLevel ?? '-'}</td>
                      <td className="py-3 px-4 text-sm">
                        {conv.lastMessageAt ? new Date(conv.lastMessageAt).toLocaleString() : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="mt-4 flex justify-between items-center">
                <div className="text-sm text-gray-600">
                  총 {conversations.page.totalElements}개 중{' '}
                  {conversations.page.number * conversations.page.size + 1}-
                  {Math.min(
                    (conversations.page.number + 1) * conversations.page.size,
                    conversations.page.totalElements
                  )}
                  개
                </div>
                <div className="flex gap-2">
                  <Button
                    onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                    disabled={currentPage === 0}
                    variant="cancel"
                  >
                    이전
                  </Button>
                  <Button
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage >= conversations.page.totalPages - 1}
                    variant="cancel"
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <div className="text-center py-8 text-gray-500">대화 목록이 없습니다.</div>
          )}
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">대화 상세</h2>
          {detail ? (
            <div className="space-y-3">
              {detail.timeline.map((msg) => (
                <div key={`${msg.messageId}-${msg.sequenceNumber}`} className="border-b border-gray-100 pb-3">
                  <div className="text-sm text-gray-500 mb-1">
                    {msg.senderType} · {msg.createdAt ? new Date(msg.createdAt).toLocaleString() : '-'} · #{msg.sequenceNumber}
                  </div>
                  <div className="text-sm text-gray-800 whitespace-pre-wrap">{msg.content}</div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">대화를 선택해주세요.</div>
          )}
        </div>
      </div>
    </AdminLayout>
  )
}
