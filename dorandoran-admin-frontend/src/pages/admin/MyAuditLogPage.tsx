import { useState } from 'react'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import { getAuditLogs } from '../../api/admin/myManagementQueue'
import type { AuditLogPageResponse } from '../../types/auditLog'
import type { QueueType } from '../../types/managementQueue'

export default function MyAuditLogPage() {
  // 검색 필터
  const [queueType, setQueueType] = useState<QueueType | ''>('')
  const [adminName, setAdminName] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  // 검색 결과
  const [auditLogs, setAuditLogs] = useState<AuditLogPageResponse | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 날짜 형식 변환 함수 (datetime-local → API 형식)
  const formatDateTimeForAPI = (dateTimeLocal: string): string => {
    // "2025-01-20T14:30" → "2025-01-20T14:30:00"
    return dateTimeLocal + ':00'
  }

  // 검색 실행
  const handleSearch = async (page: number = 0) => {
    // 날짜 유효성 검사
    if (!startDate || !endDate) {
      setError('시작일과 종료일을 모두 입력해주세요')
      return
    }

    if (new Date(startDate) > new Date(endDate)) {
      setError('시작일이 종료일보다 늦을 수 없습니다')
      return
    }

    try {
      setIsLoading(true)
      setError(null)

      const result = await getAuditLogs(
        queueType || undefined,
        adminName || undefined,
        formatDateTimeForAPI(startDate),
        formatDateTimeForAPI(endDate),
        page,
        20
      )

      setAuditLogs(result)
      setCurrentPage(page)
    } catch (err) {
      console.error('감사 로그 조회 실패:', err)
      setError('감사 로그 조회에 실패했습니다')
    } finally {
      setIsLoading(false)
    }
  }

  // 조회하기 버튼
  const handleSearchClick = () => {
    handleSearch(0)
  }

  // 페이지 이동
  const goToPage = (page: number) => {
    handleSearch(page)
  }

  // 변경 내용 요약 (requestData)
  const summarizeRequestData = (requestData: any): string => {
    if (!requestData) return '-'

    const itemCount = requestData.items?.length || 0
    const memo = requestData.memo || ''

    return `${itemCount}개 항목${memo ? ` (메모: ${memo.slice(0, 20)}...)` : ''}`
  }

  // 처리 결과 요약 (resultData)
  const summarizeResultData = (resultData: any): string => {
    if (!resultData) return '-'

    const action = resultData.action || '-'
    const note = resultData.note || ''

    return `${action}${note ? ` (${note.slice(0, 20)}...)` : ''}`
  }

  // 오늘 날짜 기본값 설정 (로컬 시간 사용)
  const getDefaultStartDate = () => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}T00:00`
  }

  const getDefaultEndDate = () => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}T23:59`
  }

  return (
    <AdminLayout>
      <div className="w-full p-8">
        {/* 헤더 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">감사 로그</h1>
          <p className="text-lg text-gray-600">
            관리자 작업 이력을 조회할 수 있습니다.
          </p>
        </div>

        {/* 검색 필터 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <div className="grid grid-cols-2 gap-4 mb-4">
            {/* 큐 타입 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                큐 타입
              </label>
              <select
                value={queueType}
                onChange={(e) => setQueueType(e.target.value as QueueType | '')}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              >
                <option value="">전체</option>
                <option value="CORRECTION">교정 작업</option>
                <option value="DELETION">삭제 작업</option>
              </select>
            </div>

            {/* 관리자명 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                관리자명
              </label>
              <input
                type="text"
                value={adminName}
                onChange={(e) => setAdminName(e.target.value)}
                placeholder="관리자 이메일 입력"
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              />
            </div>

            {/* 시작일 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                시작일시
              </label>
              <input
                type="datetime-local"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              />
              <button
                onClick={() => setStartDate(getDefaultStartDate())}
                className="text-xs text-blue-600 hover:text-blue-700 mt-1"
              >
                오늘 00:00으로 설정
              </button>
            </div>

            {/* 종료일 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                종료일시
              </label>
              <input
                type="datetime-local"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              />
              <button
                onClick={() => setEndDate(getDefaultEndDate())}
                className="text-xs text-blue-600 hover:text-blue-700 mt-1"
              >
                오늘 23:59로 설정
              </button>
            </div>
          </div>

          {/* 에러 메시지 */}
          {error && (
            <div className="mb-4 text-red-600 text-sm">
              {error}
            </div>
          )}

          {/* 조회하기 버튼 */}
          <div className="flex justify-end">
            <Button onClick={handleSearchClick} variant="confirm" size="md">
              조회하기
            </Button>
          </div>
        </div>

        {/* 검색 결과 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">감사 로그 목록</h2>

          {isLoading ? (
            <LoadingSpinner message="조회 중..." />
          ) : auditLogs && auditLogs.content.length > 0 ? (
            <>
              {/* 결과 테이블 */}
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        관리자명
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        작업 일시
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        기능
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        상태
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        요청 내용
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        처리 결과
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        접속 IP
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogs.content.map((log) => (
                      <tr
                        key={log.id}
                        className="border-b border-gray-100 hover:bg-gray-50"
                      >
                        <td className="py-3 px-4 text-sm text-gray-800">
                          {log.adminName}
                        </td>
                        <td className="py-3 px-4 text-sm text-gray-800">
                          {new Date(log.createdAt).toLocaleString('ko-KR')}
                        </td>
                        <td className="py-3 px-4 text-sm">
                          <span
                            className={`inline-block px-2 py-1 text-xs rounded ${
                              log.queueType === 'CORRECTION'
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-red-100 text-red-800'
                            }`}
                          >
                            {log.queueType === 'CORRECTION' ? '교정 작업' : '삭제 작업'}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-sm">
                          <span
                            className={`inline-block px-2 py-1 text-xs rounded ${
                              log.status === 'PENDING'
                                ? 'bg-orange-100 text-orange-800'
                                : 'bg-green-100 text-green-800'
                            }`}
                          >
                            {log.status === 'PENDING' ? '미처리' : '처리 완료'}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-sm text-gray-700">
                          {summarizeRequestData(log.requestData)}
                        </td>
                        <td className="py-3 px-4 text-sm text-gray-700">
                          {summarizeResultData(log.resultData)}
                        </td>
                        <td className="py-3 px-4 text-sm text-gray-600">
                          {log.adminIp}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* 페이지네이션 */}
              <div className="mt-4 flex justify-between items-center">
                <div className="text-sm text-gray-600">
                  {currentPage + 1} / {auditLogs.page.totalPages} 페이지 (전체{' '}
                  {auditLogs.page.totalElements}건)
                </div>
                <div className="flex gap-2">
                  <Button
                    onClick={() => goToPage(currentPage - 1)}
                    disabled={currentPage === 0}
                    variant="cancel"
                  >
                    이전
                  </Button>
                  <Button
                    onClick={() => goToPage(currentPage + 1)}
                    disabled={currentPage >= auditLogs.page.totalPages - 1}
                    variant="cancel"
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          ) : auditLogs ? (
            <div className="text-center py-8 text-gray-500">
              검색 결과가 없습니다.
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              검색 조건을 입력하고 조회하기 버튼을 클릭하세요.
            </div>
          )}
        </div>
      </div>
    </AdminLayout>
  )
}