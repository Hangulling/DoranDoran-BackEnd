import { useEffect, useState } from 'react'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import {
  getReviewTickets,
  getReviewTicketCounts,
  getReviewTicketDetail,
  updateReviewTicket,
  deleteReviewTicket,
  completeReviewTickets,
  type ReviewTicketListResponse,
  type ReviewTicketDetailResponse,
} from '../../api/admin/reviewTickets'

export default function ChatLogsManagementNeededPage() {
  const [status, setStatus] = useState('OPEN')
  const [agentType, setAgentType] = useState<string>('')
  const [counts, setCounts] = useState<Record<string, number>>({})
  const [tickets, setTickets] = useState<ReviewTicketListResponse | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [selectedTicket, setSelectedTicket] = useState<ReviewTicketDetailResponse | null>(null)
  const [note, setNote] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    loadCounts()
  }, [status])

  useEffect(() => {
    loadTickets()
  }, [status, agentType, currentPage])

  const loadCounts = async () => {
    try {
      const response = await getReviewTicketCounts(status)
      setCounts(response.byAgentType || {})
    } catch (error) {
      console.error('카운트 조회 실패:', error)
    }
  }

  const loadTickets = async () => {
    try {
      setIsLoading(true)
      const response = await getReviewTickets(status, agentType || undefined, currentPage, 20)
      setTickets(response)
    } catch (error) {
      console.error('티켓 목록 조회 실패:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleSelectTicket = async (ticketId: number) => {
    try {
      const detail = await getReviewTicketDetail(ticketId)
      setSelectedTicket(detail)
      setNote(detail.note || '')
    } catch (error) {
      console.error('티켓 상세 조회 실패:', error)
    }
  }

  const handleUpdateNote = async () => {
    if (!selectedTicket) return
    try {
      const updated = await updateReviewTicket(selectedTicket.id, note)
      setSelectedTicket({ ...selectedTicket, note: updated.note })
      await loadTickets()
      alert('메모가 수정되었습니다.')
    } catch (error) {
      console.error('메모 수정 실패:', error)
      alert('메모 수정에 실패했습니다.')
    }
  }

  const handleDelete = async () => {
    if (!selectedTicket) return
    if (!confirm('해당 티켓을 삭제하시겠습니까?')) return
    try {
      await deleteReviewTicket(selectedTicket.id)
      setSelectedTicket(null)
      await loadTickets()
      alert('삭제되었습니다.')
    } catch (error) {
      console.error('티켓 삭제 실패:', error)
      alert('삭제에 실패했습니다.')
    }
  }

  const handleCompleteSelected = async () => {
    if (selectedIds.length === 0) {
      alert('처리할 항목을 선택해주세요.')
      return
    }
    try {
      await completeReviewTickets(selectedIds)
      setSelectedIds([])
      await loadTickets()
      alert('처리 완료되었습니다.')
    } catch (error) {
      console.error('처리 완료 실패:', error)
      alert('처리 완료에 실패했습니다.')
    }
  }

  return (
    <AdminLayout>
      <div className="w-full p-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">관리 필요 내역</h1>
          <p className="text-lg text-gray-600">관리자의 개입이 필요한 채팅 내역을 확인할 수 있습니다.</p>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <div className="flex flex-wrap gap-3 items-center">
            <div className="flex gap-2">
              {['OPEN', 'DONE'].map((s) => (
                <Button
                  key={s}
                  variant={status === s ? 'confirm' : 'cancel'}
                  onClick={() => {
                    setStatus(s)
                    setCurrentPage(0)
                  }}
                >
                  {s === 'OPEN' ? '미처리' : '처리완료'}
                </Button>
              ))}
            </div>
            <select
              value={agentType}
              onChange={(e) => {
                setAgentType(e.target.value)
                setCurrentPage(0)
              }}
              className="h-11 px-3 border border-gray-300 rounded-lg"
            >
              <option value="">전체</option>
              <option value="intimacy">intimacy</option>
              <option value="conver">conver</option>
              <option value="voca">voca</option>
            </select>
            <div className="text-sm text-gray-600">
              {Object.entries(counts).map(([key, value]) => (
                <span key={key} className="mr-3">
                  {key}: {value}
                </span>
              ))}
            </div>
            <div className="ml-auto">
              <Button onClick={handleCompleteSelected} variant="primary" disabled={selectedIds.length === 0}>
                처리 완료 ({selectedIds.length})
              </Button>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-6">
          <div className="col-span-2 bg-white rounded-lg shadow-sm p-6 border border-gray-200">
            <h2 className="text-xl font-semibold text-gray-800 mb-4">티켓 목록</h2>
            {isLoading ? (
              <div className="text-center py-8 text-gray-500">로딩 중...</div>
            ) : tickets && tickets.content.length > 0 ? (
              <>
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200">
                      <th className="text-left py-3 px-4">
                        <input
                          type="checkbox"
                          checked={selectedIds.length === tickets.content.length}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedIds(tickets.content.map((t) => t.id))
                            } else {
                              setSelectedIds([])
                            }
                          }}
                        />
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">ID</th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">에이전트</th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">메모</th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">생성일</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tickets.content.map((ticket) => (
                      <tr
                        key={ticket.id}
                        className="border-b border-gray-100 cursor-pointer hover:bg-gray-50"
                        onClick={() => handleSelectTicket(ticket.id)}
                      >
                        <td className="py-3 px-4">
                          <input
                            type="checkbox"
                            checked={selectedIds.includes(ticket.id)}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedIds([...selectedIds, ticket.id])
                              } else {
                                setSelectedIds(selectedIds.filter((id) => id !== ticket.id))
                              }
                            }}
                          />
                        </td>
                        <td className="py-3 px-4 text-sm">{ticket.id}</td>
                        <td className="py-3 px-4 text-sm">{ticket.agentType || '-'}</td>
                        <td className="py-3 px-4 text-sm">{ticket.note || '-'}</td>
                        <td className="py-3 px-4 text-sm">{new Date(ticket.createdAt).toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="mt-4 flex justify-between items-center">
                  <div className="text-sm text-gray-600">
                    총 {tickets.page.totalElements}개 중 {tickets.page.number * tickets.page.size + 1}-
                    {Math.min((tickets.page.number + 1) * tickets.page.size, tickets.page.totalElements)}개
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
                      disabled={currentPage >= tickets.page.totalPages - 1}
                      variant="cancel"
                    >
                      다음
                    </Button>
                  </div>
                </div>
              </>
            ) : (
              <div className="text-center py-8 text-gray-500">티켓이 없습니다.</div>
            )}
          </div>

          <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
            <h2 className="text-xl font-semibold text-gray-800 mb-4">티켓 상세</h2>
            {selectedTicket ? (
              <div className="space-y-4">
                <div className="text-sm text-gray-600">ID: {selectedTicket.id}</div>
                <div className="text-sm text-gray-600">상태: {selectedTicket.status}</div>
                <div className="text-sm text-gray-600">에이전트: {selectedTicket.agentType || '-'}</div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">메모</label>
                  <textarea
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    className="w-full h-24 px-3 py-2 border border-gray-300 rounded-lg"
                  />
                </div>
                <div className="flex gap-2">
                  <Button onClick={handleUpdateNote} variant="confirm">
                    메모 수정
                  </Button>
                  <Button onClick={handleDelete} variant="cancel">
                    삭제
                  </Button>
                </div>
                <div>
                  <div className="text-sm font-medium text-gray-700 mb-2">항목</div>
                  {selectedTicket.items.length > 0 ? (
                    <ul className="text-sm text-gray-600 list-disc pl-4">
                      {selectedTicket.items.map((item, idx) => (
                        <li key={`${item.messageId ?? 'item'}-${idx}`}>
                          {item.agentType || '-'} · {item.messageId || '-'}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <div className="text-sm text-gray-500">항목이 없습니다.</div>
                  )}
                </div>
              </div>
            ) : (
              <div className="text-center py-8 text-gray-500">티켓을 선택해주세요.</div>
            )}
          </div>
        </div>
      </div>
    </AdminLayout>
  )
}
