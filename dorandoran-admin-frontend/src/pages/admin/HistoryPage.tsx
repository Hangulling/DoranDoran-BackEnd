import { useEffect, useState } from 'react'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import { getAuditLogs, type AdminAuditLogListResponse } from '../../api/admin/auditLogs'

export default function HistoryPage() {
  const [adminUserId, setAdminUserId] = useState('')
  const [actionType, setActionType] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [currentPage, setCurrentPage] = useState(0)
  const [logs, setLogs] = useState<AdminAuditLogListResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    loadLogs(currentPage)
  }, [currentPage])

  const loadLogs = async (page: number) => {
    try {
      const normalizeDateTime = (value: string) =>
        value ? (value.length === 16 ? `${value}:00` : value) : undefined

      setIsLoading(true)
      const response = await getAuditLogs({
        adminUserId: adminUserId || undefined,
        actionType: actionType || undefined,
        from: normalizeDateTime(from),
        to: normalizeDateTime(to),
        page,
        size: 20,
      })
      setLogs(response)
    } catch (error) {
      console.error('감사 로그 조회 실패:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleSearch = () => {
    setCurrentPage(0)
    loadLogs(0)
  }

  return (
    <AdminLayout>
      <div className="w-full p-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">관리 이력 조회</h1>
          <p className="text-lg text-gray-600">관리자의 작업 이력을 조회할 수 있습니다.</p>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <div className="grid grid-cols-5 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">관리자 ID</label>
              <input
                value={adminUserId}
                onChange={(e) => setAdminUserId(e.target.value)}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
                placeholder="uuid"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">액션 타입</label>
              <input
                value={actionType}
                onChange={(e) => setActionType(e.target.value)}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
                placeholder="PROMPT_CREATE"
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
            <div className="flex items-end">
              <Button onClick={handleSearch} variant="confirm" className="w-full">
                조회하기
              </Button>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">감사 로그</h2>
          {isLoading ? (
            <div className="text-center py-8 text-gray-500">로딩 중...</div>
          ) : logs && logs.content.length > 0 ? (
            <>
              <table className="w-full border-collapse">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">시간</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">관리자</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">액션</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">대상</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">요약</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.content.map((log) => (
                    <tr key={log.id} className="border-b border-gray-100">
                      <td className="py-3 px-4 text-sm">{new Date(log.createdAt).toLocaleString()}</td>
                      <td className="py-3 px-4 text-sm">{log.adminUserId}</td>
                      <td className="py-3 px-4 text-sm">{log.actionType}</td>
                      <td className="py-3 px-4 text-sm">
                        {log.targetType || '-'} {log.targetId ? `#${log.targetId}` : ''}
                      </td>
                      <td className="py-3 px-4 text-sm">{log.summary || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="mt-4 flex justify-between items-center">
                <div className="text-sm text-gray-600">
                  총 {logs.page.totalElements}개 중 {logs.page.number * logs.page.size + 1}-
                  {Math.min((logs.page.number + 1) * logs.page.size, logs.page.totalElements)}개
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
                    disabled={currentPage >= logs.page.totalPages - 1}
                    variant="cancel"
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <div className="text-center py-8 text-gray-500">감사 로그가 없습니다.</div>
          )}
        </div>
      </div>
    </AdminLayout>
  )
}
