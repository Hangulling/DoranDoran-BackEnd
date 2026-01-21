import { useState, useEffect } from 'react'
import Button from '../common/Button'
import { createManagementQueue } from '../../api/admin/myManagementQueue'
import type { MessageTimelineResponse } from '../../types/chatLog'
import type { QueueItem, ManagementQueueRequest } from '../../types/managementQueue'

interface ExportToQueueModalProps {
  chatroomId: string
  selectedMessageIds: string[]
  messages: MessageTimelineResponse[]
  onClose: () => void
  onSuccess: () => void
}

interface SelectableItem {
  id: string  // 고유 ID
  type: 'user' | 'intimacy' | 'conversation' | 'voca'
  messageId: string
  time: string
  summary: string
  data: MessageTimelineResponse
  queueItem?: QueueItem  // 실제 등록할 데이터
}

export default function ExportToQueueModal({
  chatroomId,
  selectedMessageIds,
  messages,
  onClose,
  onSuccess,
}: ExportToQueueModalProps) {
  const [selectableItems, setSelectableItems] = useState<SelectableItem[]>([])
  const [selectedItemIds, setSelectedItemIds] = useState<Set<string>>(new Set())
  const [memo, setMemo] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 초기화: 메시지를 선택 가능한 항목으로 변환
  useEffect(() => {
    const items: SelectableItem[] = []

    messages.forEach((message) => {
      const time = new Date(message.sourceCreatedAt).toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
      })

      // USER 메시지
      items.push({
        id: `user-${message.messageId}`,
        type: 'user',
        messageId: message.messageId,
        time,
        summary: message.content.slice(0, 50) + (message.content.length > 50 ? '...' : ''),
        data: message,
      })

      // Agent 결과
      if (message.agentResults) {
        // Intimacy
        if (message.agentResults.intimacy) {
          const intimacy = message.agentResults.intimacy
          items.push({
            id: `intimacy-${message.messageId}`,
            type: 'intimacy',
            messageId: message.messageId,
            time,
            summary: `Level ${intimacy.detectedLevel} → ${intimacy.correctedSentence.slice(0, 30)}...`,
            data: message,
            queueItem: {
              type: 'intimacy',
              messageId: message.messageId,
              content: message.content,
              detectedLevel: intimacy.detectedLevel,
              correctedSentence: intimacy.correctedSentence,
              corrections: intimacy.corrections,
              feedback: intimacy.feedback,
            },
          })
        }

        // Conversation
        if (message.agentResults.conversation) {
          const conver = message.agentResults.conversation
          items.push({
            id: `conversation-${message.messageId}`,
            type: 'conversation',
            messageId: message.messageId,
            time,
            summary: conver.content.slice(0, 50) + (conver.content.length > 50 ? '...' : ''),
            data: message,
            queueItem: {
              type: 'conversation',
              messageId: message.messageId,
              content: conver.content,
            },
          })
        }

        // Vocabulary
        if (message.agentResults.vocabulary && message.agentResults.vocabulary.words.length > 0) {
          message.agentResults.vocabulary.words.forEach((word, idx) => {
            items.push({
              id: `voca-${message.messageId}-${idx}`,
              type: 'voca',
              messageId: message.messageId,
              time,
              summary: `${word.word} (난이도: ${word.difficulty})`,
              data: message,
              queueItem: {
                type: 'voca',
                messageId: message.messageId,
                word: word.word,
                difficulty: word.difficulty,
                context: word.context,
              },
            })
          })
        }
      }
    })

    setSelectableItems(items)

    // 기본 선택: intimacy 항목들
    const defaultSelected = new Set(
      items.filter((item) => item.type === 'intimacy').map((item) => item.id)
    )
    setSelectedItemIds(defaultSelected)
  }, [messages])

  // 항목 선택 토글
  const toggleItem = (itemId: string) => {
    setSelectedItemIds((prev) => {
      const next = new Set(prev)
      if (next.has(itemId)) {
        next.delete(itemId)
      } else {
        next.add(itemId)
      }
      return next
    })
  }

  // 전체 선택/해제
  const toggleAll = (checked: boolean) => {
    if (checked) {
      setSelectedItemIds(new Set(selectableItems.map((item) => item.id)))
    } else {
      setSelectedItemIds(new Set())
    }
  }

  // 등록하기
  const handleSubmit = async () => {
    if (selectedItemIds.size === 0) {
      setError('최소 1개 이상의 항목을 선택해주세요')
      return
    }

    try {
      setIsSubmitting(true)
      setError(null)

      // 선택된 항목들을 QueueItem으로 변환
      const queueItems: QueueItem[] = selectableItems
        .filter((item) => selectedItemIds.has(item.id))
        .filter((item) => item.queueItem)  // user 타입 제외
        .map((item) => item.queueItem!)

      const request: ManagementQueueRequest = {
        queueType: 'CORRECTION',
        requestData: {
          items: queueItems,
          memo: memo || undefined,
          chatroomId,
          messageId: selectedMessageIds[0],  // 첫 번째 메시지 ID
        },
      }

      await createManagementQueue(request)
      onSuccess()
    } catch (err) {
      console.error('등록 실패:', err)
      setError('등록에 실패했습니다')
    } finally {
      setIsSubmitting(false)
    }
  }

  // ESC 키로 닫기
  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    window.addEventListener('keydown', handleEsc)
    return () => window.removeEventListener('keydown', handleEsc)
  }, [onClose])

  const allSelected = selectableItems.length > 0 && selectedItemIds.size === selectableItems.length

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-800">
            관리 필요 내역으로 내보내기
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            관리가 필요한 항목을 선택해주세요
          </p>
        </div>

        {/* 본문 */}
        <div className="p-6 overflow-y-auto max-h-[calc(90vh-200px)]">
          {/* 항목 선택 */}
          <div className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-lg font-semibold text-gray-800">내보낼 항목</h3>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={(e) => toggleAll(e.target.checked)}
                  className="w-4 h-4"
                />
                <span className="text-sm text-gray-600">전체 선택</span>
              </label>
            </div>

            {/* 항목 리스트 */}
            <div className="border border-gray-200 rounded-lg divide-y divide-gray-200 max-h-80 overflow-y-auto">
              {selectableItems.length > 0 ? (
                selectableItems.map((item) => (
                  <label
                    key={item.id}
                    className="flex items-start gap-3 p-3 hover:bg-gray-50 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      checked={selectedItemIds.has(item.id)}
                      onChange={() => toggleItem(item.id)}
                      className="w-5 h-5 mt-0.5 flex-shrink-0"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-xs text-gray-500">{item.time}</span>
                        <span
                          className={`inline-block px-2 py-0.5 text-xs rounded ${
                            item.type === 'user'
                              ? 'bg-blue-100 text-blue-800'
                              : item.type === 'intimacy'
                              ? 'bg-yellow-100 text-yellow-800'
                              : item.type === 'conversation'
                              ? 'bg-purple-100 text-purple-800'
                              : 'bg-pink-100 text-pink-800'
                          }`}
                        >
                          {item.type}
                        </span>
                      </div>
                      <div className="text-sm text-gray-800 truncate">
                        {item.summary}
                      </div>
                    </div>
                  </label>
                ))
              ) : (
                <div className="p-4 text-center text-sm text-gray-500">
                  선택 가능한 항목이 없습니다
                </div>
              )}
            </div>

            <div className="mt-2 text-sm text-gray-600">
              선택됨: {selectedItemIds.size}개
            </div>
          </div>

          {/* 메모 입력 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              관리자 메모
            </label>
            <textarea
              value={memo}
              onChange={(e) => setMemo(e.target.value)}
              placeholder="관리가 필요한 부분을 상세히 입력해주세요 (선택 사항)"
              className="w-full h-32 px-3 py-2 border border-gray-300 rounded-lg resize-none"
              maxLength={500}
            />
            <div className="mt-1 text-sm text-gray-500 text-right">
              {memo.length} / 500
            </div>
          </div>

          {/* 에러 메시지 */}
          {error && (
            <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-600">{error}</p>
            </div>
          )}
        </div>

        {/* 푸터 */}
        <div className="p-6 border-t border-gray-200 flex justify-end gap-3">
          <Button onClick={onClose} variant="cancel">
            취소
          </Button>
          <Button
            onClick={handleSubmit}
            variant="confirm"
            disabled={selectedItemIds.size === 0 || isSubmitting}
          >
            {isSubmitting ? '등록 중...' : `등록 (${selectedItemIds.size})`}
          </Button>
        </div>
      </div>
    </div>
  )
}