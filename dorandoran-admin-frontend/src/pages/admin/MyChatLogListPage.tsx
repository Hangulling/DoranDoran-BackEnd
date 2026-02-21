import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import {
  getChatroomOptions,
  getIntimacyLevelOptions,
  searchChatLogs,
} from '../../api/admin/myChatLogs'
import type {
  ChatroomOption,
  IntimacyLevelOption,
  ChatLogListPageResponse,
} from '../../types/chatLog'

export default function MyChatLogListPage() {
  const navigate = useNavigate()

  // 필터 상태
  const [year, setYear] = useState('')
  const [month, setMonth] = useState('')
  const [day, setDay] = useState('')
  const [chatroomId, setChatroomId] = useState('')
  const [intimacyLevel, setIntimacyLevel] = useState<number | ''>('')

  // 옵션 데이터
  const [chatroomOptions, setChatroomOptions] = useState<ChatroomOption[]>([])
  const [intimacyOptions, setIntimacyOptions] = useState<IntimacyLevelOption[]>([])

  // 검색 결과
  const [searchResult, setSearchResult] = useState<ChatLogListPageResponse | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 초기 로드: 옵션 데이터
  useEffect(() => {
    loadOptions()
  }, [])

  // 옵션 로드
  const loadOptions = async () => {
    try {
      const [chatrooms, intimacyLevels] = await Promise.all([
        getChatroomOptions(),
        getIntimacyLevelOptions(),
      ])
      setChatroomOptions(chatrooms)
      setIntimacyOptions(intimacyLevels)
    } catch (err) {
      console.error('옵션 로드 실패:', err)
    }
  }

  // 검색 실행
  const handleSearch = async (page: number = 0) => {
    // 날짜 유효성 검사
    if (!year || !month || !day) {
      setError('년/월/일을 모두 입력해주세요')
      return
    }

    const monthNum = parseInt(month)
    const dayNum = parseInt(day)

    if (monthNum < 1 || monthNum > 12) {
      setError('월은 1~12 사이여야 합니다')
      return
    }

    if (dayNum < 1 || dayNum > 31) {
      setError('일은 1~31 사이여야 합니다')
      return
    }

    // 날짜 포맷팅 (yyyy-MM-dd)
    const startDate = `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`

    try {
      setIsLoading(true)
      setError(null)

      const result = await searchChatLogs({
        startDate,
        chatroomId: chatroomId || undefined,
        intimacyLevel: intimacyLevel === '' ? undefined : intimacyLevel,
        page,
        size: 20,
      })

      setSearchResult(result)
      setCurrentPage(page)
    } catch (err) {
      console.error('검색 실패:', err)
      setError('검색에 실패했습니다')
    } finally {
      setIsLoading(false)
    }
  }

  // 조회하기 버튼
  const handleSearchClick = () => {
    handleSearch(0)
  }

  // 행 클릭 (상세 페이지로 이동)
  const handleRowClick = (chatroomId: string) => {
    navigate(`/my/chat-logs/${chatroomId}`)
  }

  // 페이지 이동
  const goToPage = (page: number) => {
    handleSearch(page)
  }

  return (
    <AdminLayout>
      <div className="w-full p-8">
        {/* 헤더 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">채팅 로그 조회</h1>
          <p className="text-lg text-gray-600">사용자의 채팅 내역을 조회할 수 있습니다.</p>
        </div>

        {/* 검색 필터 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <div className="grid grid-cols-5 gap-4">
            {/* 날짜 필터 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">년</label>
              <input
                type="number"
                value={year}
                onChange={(e) => setYear(e.target.value)}
                placeholder="2025"
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
                min="2020"
                max="2099"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">월</label>
              <input
                type="number"
                value={month}
                onChange={(e) => setMonth(e.target.value)}
                placeholder="01"
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
                min="1"
                max="12"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">일</label>
              <input
                type="number"
                value={day}
                onChange={(e) => setDay(e.target.value)}
                placeholder="20"
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
                min="1"
                max="31"
              />
            </div>

            {/* 채팅룸 선택 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">채팅룸</label>
              <select
                value={chatroomId}
                onChange={(e) => setChatroomId(e.target.value)}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              >
                <option value="">채팅룸을 선택해주세요</option>
                {chatroomOptions.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.name} ({option.concept})
                  </option>
                ))}
              </select>
            </div>

            {/* 친밀도 선택 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">친밀도</label>
              <select
                value={intimacyLevel}
                onChange={(e) => {
                  const value = e.target.value
                  setIntimacyLevel(value === '' ? '' : Number(value))
                }}
                className="w-full h-11 px-3 border border-gray-300 rounded-lg"
              >
                <option value="">친밀도를 선택해주세요</option>
                {intimacyOptions.map((option) => (
                  <option key={option.level} value={option.level}>
                    Level {option.level} - {option.description}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* 에러 메시지 */}
          {error && (
            <div className="mt-4 text-red-600 text-sm">
              {error}
            </div>
          )}

          {/* 조회하기 버튼 */}
          <div className="mt-4 flex justify-end">
            <Button onClick={handleSearchClick} variant="confirm" size="md">
              조회하기
            </Button>
          </div>
        </div>

        {/* 검색 결과 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">검색 결과</h2>

          {isLoading ? (
            <LoadingSpinner message="검색 중..." />
          ) : searchResult && searchResult.content.length > 0 ? (
            <>
              {/* 결과 테이블 */}
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200">
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        사용자 ID
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        날짜
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        채팅룸
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        친밀도
                      </th>
                      <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">
                        메시지 수
                      </th>
                      <th className="text-center py-3 px-4 text-sm font-medium text-gray-700">
                        상세
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {searchResult.content.map((log) => (
                      <tr
                        key={log.chatroomId}
                        className="border-b border-gray-100 hover:bg-gray-50 cursor-pointer"
                        onClick={() => handleRowClick(log.chatroomId)}
                      >
                        <td className="py-3 px-4 text-sm">{log.userEmail}</td>
                        <td className="py-3 px-4 text-sm">
                          {new Date(log.lastMessageAt).toLocaleString('ko-KR')}
                        </td>
                        <td className="py-3 px-4 text-sm">
                          {log.chatroomName} ({log.concept})
                        </td>
                        <td className="py-3 px-4 text-sm">Level {log.intimacyLevel}</td>
                        <td className="py-3 px-4 text-sm">{log.messageCount}</td>
                        <td className="py-3 px-4 text-center text-sm">
                          <span className="text-blue-600">→</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* 페이지네이션 */}
              <div className="mt-4 flex justify-between items-center">
                <div className="text-sm text-gray-600">
                  {currentPage + 1} / {searchResult.page.totalPages} 페이지 (총{' '}
                  {searchResult.page.totalElements}건)
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
                    disabled={currentPage >= searchResult.page.totalPages - 1}
                    variant="cancel"
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          ) : searchResult ? (
            <div className="text-center py-8 text-gray-500">검색 결과가 없습니다.</div>
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