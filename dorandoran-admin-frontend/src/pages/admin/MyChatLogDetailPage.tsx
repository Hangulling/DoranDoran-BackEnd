import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ExportToQueueModal from '../../components/admin/ExportToQueueModal'
import { getChatLogTimeline } from '../../api/admin/myChatLogs'
import type { MessageTimelinePageResponse, MessageTimelineResponse } from '../../types/chatLog'

export default function MyChatLogDetailPage() {
  const { chatroomId } = useParams<{ chatroomId: string }>()
  const navigate = useNavigate()

  // 타임라인 데이터
  const [timeline, setTimeline] = useState<MessageTimelinePageResponse | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [isLoading, setIsLoading] = useState(false)

  // 선택된 메시지 (user 메시지 단위)
  const [selectedMessageIds, setSelectedMessageIds] = useState<Set<string>>(new Set())

  // 모달 상태
  const [isModalOpen, setIsModalOpen] = useState(false)

  // 초기 로드
  useEffect(() => {
    if (chatroomId) {
      loadTimeline(0)
    }
  }, [chatroomId])

  // 타임라인 로드
  const loadTimeline = async (page: number) => {
    if (!chatroomId) return

    try {
      setIsLoading(true)
      const result = await getChatLogTimeline(chatroomId, page, 50)
      setTimeline(result)
      setCurrentPage(page)
    } catch (err) {
      console.error('타임라인 조회 실패:', err)
      alert('타임라인 조회에 실패했습니다')
    } finally {
      setIsLoading(false)
    }
  }

  // 체크박스 토글
  const toggleMessageSelection = (messageId: string) => {
    setSelectedMessageIds((prev) => {
      const next = new Set(prev)
      if (next.has(messageId)) {
        next.delete(messageId)
      } else {
        next.add(messageId)
      }
      return next
    })
  }

  // 내보내기 버튼 클릭
  const handleExportClick = () => {
    if (selectedMessageIds.size === 0) {
      alert('내보낼 항목을 선택해주세요')
      return
    }
    setIsModalOpen(true)
  }

  // 모달 닫기
  const handleModalClose = () => {
    setIsModalOpen(false)
  }

  // 내보내기 성공
  const handleExportSuccess = () => {
    setSelectedMessageIds(new Set())
    setIsModalOpen(false)
    alert('관리 필요 내역으로 등록되었습니다')
  }

  // 뒤로 가기
  const handleBack = () => {
    navigate('/my/chat-logs')
  }

  // 타입별 렌더링
  const renderMessage = (message: MessageTimelineResponse) => {
    const isUser = message.senderType === 'USER'
    const isSelected = selectedMessageIds.has(message.messageId)

    return (
      <div
        key={message.messageId}
        className={`border-b border-gray-100 py-4 ${isUser ? 'bg-blue-50' : ''}`}
      >
        <div className="flex items-start gap-4">
          {/* 체크박스 (USER만) */}
          {isUser && (
            <div className="pt-1">
              <input
                type="checkbox"
                checked={isSelected}
                onChange={() => toggleMessageSelection(message.messageId)}
                className="w-5 h-5 cursor-pointer"
              />
            </div>
          )}

          {/* 시각 */}
          <div className="w-20 text-sm text-gray-500 flex-shrink-0">
            {new Date(message.sourceCreatedAt).toLocaleTimeString('ko-KR', {
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
            })}
          </div>

          {/* 타입 */}
          <div className="w-24 flex-shrink-0">
            <span
              className={`inline-block px-2 py-1 text-xs rounded ${
                message.senderType === 'USER'
                  ? 'bg-blue-100 text-blue-800'
                  : message.senderType === 'CHATBOT'
                  ? 'bg-green-100 text-green-800'
                  : 'bg-gray-100 text-gray-800'
              }`}
            >
              {message.senderType}
            </span>
          </div>

          {/* 내용 */}
          <div className="flex-1">
            <div className="text-sm text-gray-800 whitespace-pre-wrap break-words">
              {message.content}
            </div>

            {/* Agent 결과 (USER인 경우) */}
            {message.agentResults && (
              <div className="mt-3 space-y-2">
                {/* Intimacy */}
                {message.agentResults.intimacy && (
                  <div className="bg-yellow-50 p-3 rounded border border-yellow-200">
                    <div className="text-xs font-medium text-yellow-800 mb-1">
                      intimacy
                    </div>
                    <div className="text-sm space-y-1">
                      <div>
                        <span className="text-gray-600">감지 레벨:</span> Level{' '}
                        {message.agentResults.intimacy.detectedLevel}
                      </div>
                      <div>
                        <span className="text-gray-600">교정:</span>{' '}
                        {message.agentResults.intimacy.correctedSentence}
                      </div>
                      {message.agentResults.intimacy.feedback && (
                        <div>
                          <span className="text-gray-600">피드백:</span>{' '}
                          {message.agentResults.intimacy.feedback.ko}
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Conversation */}
                {message.agentResults.conversation && (
                  <div className="bg-purple-50 p-3 rounded border border-purple-200">
                    <div className="text-xs font-medium text-purple-800 mb-1">
                      conversation
                    </div>
                    <div className="text-sm">
                      {message.agentResults.conversation.content}
                    </div>
                  </div>
                )}

                {/* Vocabulary */}
                {message.agentResults.vocabulary &&
                  message.agentResults.vocabulary.words.length > 0 && (
                    <div className="bg-pink-50 p-3 rounded border border-pink-200">
                      <div className="text-xs font-medium text-pink-800 mb-1">
                        vocabulary
                      </div>
                      <div className="space-y-2">
                        {message.agentResults.vocabulary.words.map((word, idx) => (
                          <div key={idx} className="text-sm">
                            <span className="font-medium">{word.word}</span>
                            <span className="text-gray-600 ml-2">
                              (난이도: {word.difficulty})
                            </span>
                            <div className="text-gray-600 text-xs mt-1">
                              {word.context}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
              </div>
            )}

            {/* 메타 정보 */}
            {(message.tokenCount || message.processingTimeMs) && (
              <div className="mt-2 text-xs text-gray-500">
                {message.tokenCount && <span>토큰: {message.tokenCount}</span>}
                {message.tokenCount && message.processingTimeMs && (
                  <span className="mx-2">·</span>
                )}
                {message.processingTimeMs && (
                  <span>처리시간: {message.processingTimeMs}ms</span>
                )}
              </div>
            )}
          </div>

          {/* 시퀀스 */}
          <div className="w-16 text-sm text-gray-500 text-right flex-shrink-0">
            #{message.sequenceNumber}
          </div>
        </div>
      </div>
    )
  }

  if (!chatroomId) {
    return (
      <AdminLayout>
        <div className="w-full p-8">
          <div className="text-center py-8 text-gray-500">
            잘못된 접근입니다.
          </div>
        </div>
      </AdminLayout>
    )
  }

  return (
    <AdminLayout>
      <div className="w-full p-8">
        {/* 헤더 */}
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-800 mb-2">
              채팅 상세 타임라인
            </h1>
            <p className="text-sm text-gray-600">채팅방 ID: {chatroomId}</p>
          </div>
          <Button onClick={handleBack} variant="cancel">
            목록으로
          </Button>
        </div>

        {/* 타임라인 */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 mb-6">
          <div className="p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-gray-800">타임라인</h2>
              <div className="text-sm text-gray-600">
                선택됨: {selectedMessageIds.size}개
              </div>
            </div>

            {isLoading ? (
              <LoadingSpinner message="로딩 중..." />
            ) : timeline && timeline.content.length > 0 ? (
              <>
                {/* 타임라인 컨텐츠 */}
                <div className="max-h-[600px] overflow-y-auto border border-gray-200 rounded">
                  {timeline.content.map((message) => renderMessage(message))}
                </div>

                {/* 페이지네이션 */}
                <div className="mt-4 flex justify-between items-center">
                  <div className="text-sm text-gray-600">
                    {currentPage + 1} / {timeline.page.totalPages} 페이지 (총{' '}
                    {timeline.page.totalElements}건)
                  </div>
                  <div className="flex gap-2">
                    <Button
                      onClick={() => loadTimeline(currentPage - 1)}
                      disabled={currentPage === 0}
                      variant="cancel"
                    >
                      이전
                    </Button>
                    <Button
                      onClick={() => loadTimeline(currentPage + 1)}
                      disabled={currentPage >= timeline.page.totalPages - 1}
                      variant="cancel"
                    >
                      다음
                    </Button>
                  </div>
                </div>
              </>
            ) : (
              <div className="text-center py-8 text-gray-500">
                메시지가 없습니다.
              </div>
            )}
          </div>
        </div>

        {/* 내보내기 버튼 */}
        <div className="flex justify-end">
          <Button
            onClick={handleExportClick}
            variant="confirm"
            disabled={selectedMessageIds.size === 0}
          >
            관리 필요 내역으로 내보내기 ({selectedMessageIds.size})
          </Button>
        </div>

        {/* 내보내기 모달 */}
        {isModalOpen && timeline && (
          <ExportToQueueModal
            chatroomId={chatroomId}
            selectedMessageIds={Array.from(selectedMessageIds)}
            messages={timeline.content.filter((m) =>
              selectedMessageIds.has(m.messageId)
            )}
            onClose={handleModalClose}
            onSuccess={handleExportSuccess}
          />
        )}
      </div>
    </AdminLayout>
  )
}