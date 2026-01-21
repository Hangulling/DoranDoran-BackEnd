import { useEffect, useState } from 'react'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import {
  getManagementQueueList,
  getManagementQueueCount,
  updateManagementQueue,
  batchDeleteManagementQueue,
  batchCompleteManagementQueue,
} from '../../api/admin/myManagementQueue'
import type {
  ManagementQueueResponse,
  ManagementQueueCountResponse,
  QueueStatus,
  QueueType,
} from '../../types/managementQueue'

export default function MyManagementQueuePage() {
  // 상태 관리
  const [status, setStatus] = useState<QueueStatus>('PENDING')
  const [queueType, setQueueType] = useState<QueueType | ''>('')
  const [counts, setCounts] = useState<ManagementQueueCountResponse | null>(null)
  const [queues, setQueues] = useState<ManagementQueueResponse[]>([])
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 편집 중인 메모 관리 (id -> memo)
  const [editingMemos, setEditingMemos] = useState<Map<string, string>>(new Map())

  // 카운트 로드
  const loadCounts = async () => {
    try {
      const result = await getManagementQueueCount(status)
      setCounts(result)
    } catch (err) {
      console.error('카운트 조회 실패:', err)
    }
  }

  // 목록 로드
  const loadQueues = async () => {
    try {
      setIsLoading(true)
      setError(null)

      const response = await getManagementQueueList(
        queueType || undefined,
        status,
        currentPage,
        20
      )

      setQueues(response.content)
      setTotalPages(response.page.totalPages)
      setTotalElements(response.page.totalElements)
    } catch (err) {
      console.error('목록 조회 실패:', err)
      setError('목록을 불러오는데 실패했습니다')
    } finally {
      setIsLoading(false)
    }
  }

  // 초기 로드 & 상태/타입 변경 시 재로드
  useEffect(() => {
    loadCounts()
    loadQueues()
    setSelectedIds(new Set()) // 필터 변경 시 선택 초기화
  }, [status, queueType, currentPage])

  // 체크박스 토글
  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  // 전체 선택/해제
  const toggleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedIds(new Set(queues.map((q) => q.id)))
    } else {
      setSelectedIds(new Set())
    }
  }

  // 메모 편집 시작
  const handleMemoEdit = (id: string, currentMemo: string) => {
    setEditingMemos((prev) => new Map(prev).set(id, currentMemo))
  }

  // 메모 변경
  const handleMemoChange = (id: string, newMemo: string) => {
    setEditingMemos((prev) => new Map(prev).set(id, newMemo))
  }

  // 메모 저장
  const handleMemoSave = async (id: string) => {
    const memo = editingMemos.get(id)
    if (memo === undefined) return

    try {
      await updateManagementQueue(id, memo)

      // 목록 다시 로드
      await loadQueues()

      // 편집 모드 종료
      setEditingMemos((prev) => {
        const next = new Map(prev)
        next.delete(id)
        return next
      })
    } catch (err) {
      console.error('메모 수정 실패:', err)
      alert('메모 수정에 실패했습니다')
    }
  }

  // 메모 취소
  const handleMemoCancel = (id: string) => {
    setEditingMemos((prev) => {
      const next = new Map(prev)
      next.delete(id)
      return next
    })
  }

  // 일괄 삭제
  const handleBatchDelete = async () => {
    if (selectedIds.size === 0) {
      alert('삭제할 항목을 선택해주세요')
      return
    }

    if (!confirm(`선택한 ${selectedIds.size}건을 삭제하시겠습니까?`)) {
      return
    }

    try {
      await batchDeleteManagementQueue({
        ids: Array.from(selectedIds),
      })

      alert('삭제되었습니다')
      setSelectedIds(new Set())
      await loadQueues()
      await loadCounts()
    } catch (err) {
      console.error('일괄 삭제 실패:', err)
      alert('삭제에 실패했습니다')
    }
  }

  // 일괄 처리 완료
  const handleBatchComplete = async () => {
    if (selectedIds.size === 0) {
      alert('처리 완료할 항목을 선택해주세요')
      return
    }

    const note = prompt('처리 노트를 입력해주세요 (선택사항):')
    if (note === null) return // 취소

    try {
      await batchCompleteManagementQueue({
        ids: Array.from(selectedIds),
        note: note || undefined,
      })

      alert('처리 완료되었습니다')
      setSelectedIds(new Set())
      await loadQueues()
      await loadCounts()
    } catch (err) {
      console.error('일괄 처리 완료 실패:', err)
      alert('처리 완료에 실패했습니다')
    }
  }

  // 페이지 변경
  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage)
    }
  }

  // 아이템 타입별 UI 렌더링
  const renderItem = (item: any, index: number) => {
    const { type } = item

    switch (type) {
      case 'intimacy':
        return (
          <div key={index} className="p-3 bg-yellow-50 rounded border border-yellow-200">
            <div className="flex items-center gap-2 mb-2">
              <span className="inline-block px-2 py-0.5 text-xs rounded bg-yellow-100 text-yellow-800">
                친밀도 교정
              </span>
            </div>
            <div className="space-y-1 text-sm">
              <div>
                <span className="text-gray-600">원본:</span>{' '}
                <span className="text-gray-800">{item.content}</span>
              </div>
              <div>
                <span className="text-gray-600">교정:</span>{' '}
                <span className="text-blue-600 font-medium">{item.correctedSentence}</span>
              </div>
              <div>
                <span className="text-gray-600">감지 레벨:</span>{' '}
                <span className="text-gray-800">{item.detectedLevel}</span>
              </div>
              {item.feedback?.ko && (
                <div>
                  <span className="text-gray-600">피드백:</span>{' '}
                  <span className="text-gray-700">{item.feedback.ko}</span>
                </div>
              )}
            </div>
          </div>
        )

      case 'conversation':
        return (
          <div key={index} className="p-3 bg-purple-50 rounded border border-purple-200">
            <div className="flex items-center gap-2 mb-2">
              <span className="inline-block px-2 py-0.5 text-xs rounded bg-purple-100 text-purple-800">
                대화 내용
              </span>
            </div>
            <div className="text-sm text-gray-800">
              {item.content}
            </div>
          </div>
        )

      case 'voca':
      case 'vocabulary':
        return (
          <div key={index} className="p-3 bg-pink-50 rounded border border-pink-200">
            <div className="flex items-center gap-2 mb-2">
              <span className="inline-block px-2 py-0.5 text-xs rounded bg-pink-100 text-pink-800">
                어휘
              </span>
            </div>
            <div className="space-y-1 text-sm">
              <div>
                <span className="text-gray-600">단어:</span>{' '}
                <span className="text-gray-800 font-medium">{item.word}</span>
              </div>
              <div>
                <span className="text-gray-600">난이도:</span>{' '}
                <span className="text-gray-800">{item.difficulty}</span>
              </div>
              {item.context && (
                <div>
                  <span className="text-gray-600">맥락:</span>{' '}
                  <span className="text-gray-700">{item.context}</span>
                </div>
              )}
            </div>
          </div>
        )

      default:
        return (
          <div key={index} className="p-3 bg-gray-50 rounded border border-gray-200">
            <div className="text-sm text-gray-600">
              알 수 없는 타입: {type}
            </div>
          </div>
        )
    }
  }

  const allSelected = queues.length > 0 && selectedIds.size === queues.length

  return (
    <AdminLayout>
      <div className="p-6">
        {/* 헤더 */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-800">관리 필요 내역</h1>
          <p className="text-sm text-gray-600 mt-1">
            AI 에이전트 결과 중 관리가 필요한 항목을 확인하고 처리하세요
          </p>
        </div>

        {/* 상태 토글 */}
        <div className="mb-4 flex items-center gap-2">
          <button
            onClick={() => {
              setStatus('PENDING')
              setCurrentPage(0)
            }}
            className={`px-4 py-2 rounded font-medium transition ${
              status === 'PENDING'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            미처리
          </button>
          <button
            onClick={() => {
              setStatus('COMPLETED')
              setCurrentPage(0)
            }}
            className={`px-4 py-2 rounded font-medium transition ${
              status === 'COMPLETED'
                ? 'bg-green-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            처리 완료
          </button>
        </div>

        {/* 큐 타입 필터 */}
        <div className="mb-4 flex items-center gap-2">
          <span className="text-sm text-gray-600 mr-2">큐 타입:</span>
          <button
            onClick={() => {
              setQueueType('')
              setCurrentPage(0)
            }}
            className={`px-3 py-1.5 rounded text-sm font-medium transition ${
              queueType === ''
                ? 'bg-gray-700 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            전체
          </button>
          <button
            onClick={() => {
              setQueueType('CORRECTION')
              setCurrentPage(0)
            }}
            className={`px-3 py-1.5 rounded text-sm font-medium transition ${
              queueType === 'CORRECTION'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            교정 작업
          </button>
          <button
            onClick={() => {
              setQueueType('DELETION')
              setCurrentPage(0)
            }}
            className={`px-3 py-1.5 rounded text-sm font-medium transition ${
              queueType === 'DELETION'
                ? 'bg-red-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            삭제 작업
          </button>
        </div>

        {/* 타입별 카운트 (표시용) */}
        {counts && (
          <div className="mb-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
            <div className="text-sm text-gray-600 mb-2">현재 상태의 타입별 통계:</div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <span className="inline-block w-3 h-3 rounded-full bg-yellow-400"></span>
                <span className="text-sm text-gray-700">
                  친밀도: <span className="font-medium">{counts.intimacyCount}</span>건
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className="inline-block w-3 h-3 rounded-full bg-purple-400"></span>
                <span className="text-sm text-gray-700">
                  대화: <span className="font-medium">{counts.conversationCount}</span>건
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className="inline-block w-3 h-3 rounded-full bg-pink-400"></span>
                <span className="text-sm text-gray-700">
                  어휘: <span className="font-medium">{counts.vocaCount}</span>건
                </span>
              </div>
              <div className="flex items-center gap-2 ml-auto">
                <span className="text-sm text-gray-700">
                  전체 대기: <span className="font-semibold">{counts.totalPendingCount}</span>건
                </span>
              </div>
            </div>
          </div>
        )}

        {/* 로딩 */}
        {isLoading && (
          <div className="flex justify-center py-20">
            <LoadingSpinner />
          </div>
        )}

        {/* 에러 */}
        {error && !isLoading && (
          <div className="p-4 bg-red-50 border border-red-200 rounded text-red-700">
            {error}
          </div>
        )}

        {/* 결과 없음 */}
        {!isLoading && !error && queues.length === 0 && (
          <div className="text-center py-20 text-gray-500">
            조회된 내역이 없습니다
          </div>
        )}

        {/* 목록 */}
        {!isLoading && !error && queues.length > 0 && (
          <>
            {/* 전체 선택 */}
            <div className="mb-4 flex items-center gap-2">
              <input
                type="checkbox"
                checked={allSelected}
                onChange={(e) => toggleSelectAll(e.target.checked)}
                className="w-4 h-4"
              />
              <span className="text-sm text-gray-700">
                전체 선택 ({selectedIds.size}/{queues.length})
              </span>
            </div>

            {/* 큐 카드 목록 */}
            <div className="space-y-4 mb-6">
              {queues.map((queue) => {
                const isEditing = editingMemos.has(queue.id)
                const currentMemo = queue.requestData?.memo || ''
                const editingMemo = editingMemos.get(queue.id) || currentMemo

                return (
                  <div
                    key={queue.id}
                    className="border border-gray-300 rounded-lg p-4 bg-white shadow-sm"
                  >
                    {/* 헤더 */}
                    <div className="flex items-start gap-3 mb-3">
                      <input
                        type="checkbox"
                        checked={selectedIds.has(queue.id)}
                        onChange={() => toggleSelect(queue.id)}
                        className="w-5 h-5 mt-1"
                      />
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="text-xs text-gray-500">
                            ID: {queue.id.slice(0, 8)}...
                          </span>
                          <span
                            className={`inline-block px-2 py-0.5 text-xs rounded ${
                              queue.status === 'PENDING'
                                ? 'bg-orange-100 text-orange-800'
                                : 'bg-green-100 text-green-800'
                            }`}
                          >
                            {queue.status === 'PENDING' ? '미처리' : '처리 완료'}
                          </span>
                          <span className="inline-block px-2 py-0.5 text-xs rounded bg-gray-100 text-gray-700">
                            {queue.queueType}
                          </span>
                        </div>
                        <div className="text-xs text-gray-500">
                          등록일: {new Date(queue.createdAt).toLocaleString('ko-KR')}
                        </div>
                      </div>
                    </div>

                    {/* Items */}
                    {queue.requestData?.items && queue.requestData.items.length > 0 && (
                      <div className="space-y-2 mb-3">
                        {queue.requestData.items.map((item, idx) => renderItem(item, idx))}
                      </div>
                    )}

                    {/* 메모 */}
                    <div className="border-t pt-3">
                      <div className="flex items-start gap-2">
                        <span className="text-sm text-gray-600 font-medium">메모:</span>
                        {!isEditing ? (
                          <>
                            <span className="text-sm text-gray-800 flex-1">
                              {currentMemo || '없음'}
                            </span>
                            <button
                              onClick={() => handleMemoEdit(queue.id, currentMemo)}
                              className="text-sm text-blue-600 hover:text-blue-700"
                            >
                              수정
                            </button>
                          </>
                        ) : (
                          <div className="flex-1">
                            <textarea
                              value={editingMemo}
                              onChange={(e) => handleMemoChange(queue.id, e.target.value)}
                              className="w-full px-2 py-1 border border-gray-300 rounded text-sm"
                              rows={2}
                              maxLength={500}
                            />
                            <div className="flex items-center gap-2 mt-2">
                              <button
                                onClick={() => handleMemoSave(queue.id)}
                                className="px-3 py-1 bg-blue-600 text-white text-sm rounded hover:bg-blue-700"
                              >
                                저장
                              </button>
                              <button
                                onClick={() => handleMemoCancel(queue.id)}
                                className="px-3 py-1 bg-gray-200 text-gray-700 text-sm rounded hover:bg-gray-300"
                              >
                                취소
                              </button>
                              <span className="text-xs text-gray-500 ml-auto">
                                {editingMemo.length}/500
                              </span>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* 완료 정보 (COMPLETED 상태) */}
                    {queue.status === 'COMPLETED' && queue.resultData && (
                      <div className="border-t mt-3 pt-3">
                        <div className="text-sm">
                          <span className="text-gray-600">처리자:</span>{' '}
                          <span className="text-gray-800">
                            {queue.resultData.processedBy || '-'}
                          </span>
                        </div>
                        {queue.resultData.note && (
                          <div className="text-sm mt-1">
                            <span className="text-gray-600">처리 노트:</span>{' '}
                            <span className="text-gray-800">{queue.resultData.note}</span>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>

            {/* 하단 액션 버튼 */}
            <div className="flex items-center justify-between border-t pt-4">
              <div className="flex items-center gap-2">
                <Button
                  onClick={handleBatchDelete}
                  variant="cancel"
                  disabled={selectedIds.size === 0}
                >
                  삭제 ({selectedIds.size})
                </Button>
                {status === 'PENDING' && (
                  <Button
                    onClick={handleBatchComplete}
                    variant="confirm"
                    disabled={selectedIds.size === 0}
                  >
                    처리 완료 ({selectedIds.size})
                  </Button>
                )}
              </div>

              {/* 페이지네이션 */}
              <div className="flex items-center gap-2">
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0}
                  className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
                >
                  이전
                </button>
                <span className="text-sm text-gray-700">
                  {currentPage + 1} / {totalPages || 1}
                </span>
                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1}
                  className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
                >
                  다음
                </button>
              </div>
            </div>

            {/* 총 개수 */}
            <div className="text-sm text-gray-600 text-center mt-4">
              전체 {totalElements}건
            </div>
          </>
        )}
      </div>
    </AdminLayout>
  )
}
