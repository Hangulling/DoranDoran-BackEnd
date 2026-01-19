import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import {
  getActivePrompt,
  testPrompt,
  createPromptVersion,
  getPromptFileContent,
  saveAndActivatePrompt,
  getAgentTypeOptions,
  getConceptOptions,
  getIntimacyLevelOptions,
  type PromptActiveResponse,
  type PromptTestResponse,
  type AgentTypeOption,
  type ConceptOption,
  type IntimacyLevelOption,
} from '../../api/admin/prompts'
import {
  getReviewTickets,
  completeReviewTickets,
  type ReviewTicketListResponse,
} from '../../api/admin/reviewTickets'


export default function PromptsTestAndApplyPage() {
  const navigate = useNavigate()
  const [agentType, setAgentType] = useState('INTIMACY_ANALYSIS')
  const [concept, setConcept] = useState('friend')
  const [intimacyLevel, setIntimacyLevel] = useState(1)
  const [promptContent, setPromptContent] = useState('')
  const [memo, setMemo] = useState('')
  const [testInput, setTestInput] = useState('')
  const [testOutput, setTestOutput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isTesting, setIsTesting] = useState(false)
  const [activePrompt, setActivePrompt] = useState<PromptActiveResponse | null>(null)
  const [reviewTickets, setReviewTickets] = useState<ReviewTicketListResponse | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [selectedTicketIds, setSelectedTicketIds] = useState<number[]>([])
  
  // 드롭다운 옵션
  const [agentTypeOptions, setAgentTypeOptions] = useState<AgentTypeOption[]>([])
  const [conceptOptions, setConceptOptions] = useState<ConceptOption[]>([])
  const [intimacyLevelOptions, setIntimacyLevelOptions] = useState<IntimacyLevelOption[]>([])
  const [isLoadingOptions, setIsLoadingOptions] = useState(false)

  // 드롭다운 옵션 로드
  useEffect(() => {
    loadOptions()
  }, [])

  // Active 프롬프트 로드
  useEffect(() => {
    loadActivePrompt()
  }, [agentType, concept, intimacyLevel])

  // 관리 필요 내역 로드
  useEffect(() => {
    loadReviewTickets()
  }, [currentPage])

  const loadOptions = async () => {
    try {
      setIsLoadingOptions(true)
      const [agentTypes, concepts, levels] = await Promise.all([
        getAgentTypeOptions(),
        getConceptOptions(),
        getIntimacyLevelOptions(),
      ])
      setAgentTypeOptions(agentTypes)
      setConceptOptions(concepts)
      setIntimacyLevelOptions(levels)
      
      // 첫 번째 옵션으로 초기값 설정 (옵션이 로드된 후)
      if (agentTypes.length > 0 && !agentType) {
        setAgentType(agentTypes[0].value)
      }
      if (concepts.length > 0 && !concept) {
        setConcept(concepts[0].value)
      }
      if (levels.length > 0 && !intimacyLevel) {
        setIntimacyLevel(levels[0].value)
      }
    } catch (error) {
      console.error('드롭다운 옵션 로드 실패:', error)
    } finally {
      setIsLoadingOptions(false)
    }
  }

  const loadActivePrompt = async () => {
    try {
      setIsLoading(true)
      const response = await getActivePrompt(agentType, concept, intimacyLevel)
      setActivePrompt(response)
      setPromptContent(response.content)
    } catch (error) {
      console.error('Active 프롬프트 로드 실패:', error)
      // 파일에서 로드 시도
      try {
        const fileContent = await getPromptFileContent(agentType, concept, intimacyLevel)
        setPromptContent(fileContent)
      } catch (fileError) {
        console.error('파일 내용 로드 실패:', fileError)
      }
    } finally {
      setIsLoading(false)
    }
  }

  const loadReviewTickets = async () => {
    try {
      const response = await getReviewTickets('OPEN', undefined, currentPage, 10)
      setReviewTickets(response)
    } catch (error) {
      console.error('관리 필요 내역 로드 실패:', error)
    }
  }

  const handleTest = async () => {
    if (!testInput.trim()) {
      alert('테스트 입력을 입력해주세요.')
      return
    }

    console.log('[프론트엔드] 프롬프트 테스트 시작', {
      agentType,
      concept,
      intimacyLevel,
      inputText: testInput.substring(0, 100) + (testInput.length > 100 ? '...' : ''),
      inputLength: testInput.length,
    })

    try {
      setIsTesting(true)
      const startTime = Date.now()
      const response: PromptTestResponse = await testPrompt(
        agentType,
        concept,
        intimacyLevel,
        testInput
      )
      const endTime = Date.now()
      
      console.log('[프론트엔드] 프롬프트 테스트 완료', {
        latencyMs: response.latencyMs,
        clientLatencyMs: endTime - startTime,
        outputLength: response.outputText?.length || 0,
        outputPreview: response.outputText?.substring(0, 200) + (response.outputText?.length > 200 ? '...' : ''),
        tokens: response.tokens,
      })
      
      setTestOutput(response.outputText)
    } catch (error) {
      console.error('[프론트엔드] 프롬프트 테스트 실패:', error)
      alert('프롬프트 테스트에 실패했습니다.')
    } finally {
      setIsTesting(false)
    }
  }

  const handleReset = () => {
    setTestInput('')
    setTestOutput('')
  }

  const handleSave = async () => {
    if (!promptContent.trim()) {
      alert('프롬프트 내용을 입력해주세요.')
      return
    }

    console.log('[프론트엔드] 프롬프트 저장 시작', {
      agentType,
      concept,
      intimacyLevel,
      contentLength: promptContent.length,
      memo,
    })

    try {
      setIsLoading(true)
      const version = await createPromptVersion(agentType, concept, intimacyLevel, promptContent, memo)
      console.log('[프론트엔드] 프롬프트 저장 완료', {
        versionId: version.id,
        version: version.version,
        filePath: version.filePath,
        createdAt: version.createdAt,
      })
      alert('프롬프트 버전이 생성되었습니다.')
      await loadActivePrompt()
    } catch (error) {
      console.error('[프론트엔드] 프롬프트 저장 실패:', error)
      alert('프롬프트 저장에 실패했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleApply = async () => {
    if (!promptContent.trim()) {
      alert('프롬프트 내용을 입력해주세요.')
      return
    }

    if (!confirm('프롬프트를 저장하고 적용하시겠습니까?')) {
      return
    }

    console.log('[프론트엔드] 프롬프트 저장 및 적용 시작', {
      agentType,
      concept,
      intimacyLevel,
      contentLength: promptContent.length,
      memo,
      env: 'prod',
    })

    try {
      setIsLoading(true)
      const applyStartTime = Date.now()
      
      const version = await saveAndActivatePrompt({
        env: 'prod',
        agentType,
        concept,
        intimacyLevel,
        content: promptContent,
        memo,
      })
      
      console.log('[프론트엔드] 프롬프트 저장 및 적용 완료', {
        totalTime: Date.now() - applyStartTime,
        versionId: version.id,
        version: version.version,
      })
      
      alert('프롬프트가 저장되고 적용되었습니다.')
      await loadActivePrompt()
    } catch (error) {
      console.error('[프론트엔드] 프롬프트 저장 및 적용 실패:', error)
      alert('프롬프트 저장 및 적용에 실패했습니다.')
    } finally {
      setIsLoading(false)
    }
  }


  const handleCompleteTickets = async () => {
    if (selectedTicketIds.length === 0) {
      alert('처리할 항목을 선택해주세요.')
      return
    }

    try {
      await completeReviewTickets(selectedTicketIds)
      alert('처리 완료되었습니다.')
      setSelectedTicketIds([])
      await loadReviewTickets()
    } catch (error) {
      console.error('티켓 처리 완료 실패:', error)
      alert('처리 완료에 실패했습니다.')
    }
  }

  return (
    <AdminLayout>
      <div className="w-full p-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">프롬프트 테스트 및 적용</h1>
          <p className="text-lg text-gray-600">프롬프트를 테스트하고 적용할 수 있습니다.</p>
        </div>

        {/* 선택 영역 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <div className="grid grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">에이전트</label>
              <select
                value={agentType}
                onChange={(e) => setAgentType(e.target.value)}
                className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {isLoadingOptions ? (
                  <option>로딩 중...</option>
                ) : (
                  agentTypeOptions.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))
                )}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">컨셉</label>
              <select
                value={concept}
                onChange={(e) => setConcept(e.target.value)}
                className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {isLoadingOptions ? (
                  <option>로딩 중...</option>
                ) : (
                  conceptOptions.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))
                )}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">친밀도 레벨</label>
              <select
                value={intimacyLevel}
                onChange={(e) => setIntimacyLevel(Number(e.target.value))}
                className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {isLoadingOptions ? (
                  <option>로딩 중...</option>
                ) : (
                  intimacyLevelOptions.map((level) => (
                    <option key={level.value} value={level.value}>
                      {level.label}
                    </option>
                  ))
                )}
              </select>
            </div>
            <div className="flex items-end">
              <Button
                onClick={() => navigate('/prompts/versions')}
                variant="confirm"
                className="w-full h-12"
              >
                버전 관리
              </Button>
            </div>
          </div>
        </div>

        {/* 프롬프트 편집 영역 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">프롬프트 내용</label>
            <textarea
              value={promptContent}
              onChange={(e) => setPromptContent(e.target.value)}
              className="w-full h-64 px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
              placeholder="프롬프트 내용을 입력하세요..."
            />
          </div>
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">메모 (선택)</label>
            <input
              type="text"
              value={memo}
              onChange={(e) => setMemo(e.target.value)}
              className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="메모를 입력하세요..."
            />
          </div>
          <div className="flex gap-3">
            <Button onClick={handleSave} disabled={isLoading}>
              저장
            </Button>
            <Button onClick={handleApply} disabled={isLoading}>
              저장 및 적용
            </Button>
          </div>
        </div>

        {/* 프롬프트 테스트 영역 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">프롬프트 테스트</h2>
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">테스트 입력</label>
            <textarea
              value={testInput}
              onChange={(e) => setTestInput(e.target.value)}
              className="w-full h-32 px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="테스트할 입력을 입력하세요..."
            />
          </div>
          <div className="flex gap-3 mb-4">
            <Button onClick={handleTest} disabled={isTesting || !testInput.trim()}>
              {isTesting ? '테스트 중...' : '테스트 실행'}
            </Button>
            <Button onClick={handleReset} variant="cancel">
              초기화
            </Button>
          </div>
          {testOutput && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">테스트 결과</label>
              <div className="w-full h-48 px-4 py-3 border border-gray-300 rounded-lg bg-gray-50 overflow-y-auto">
                <pre className="whitespace-pre-wrap text-sm">{testOutput}</pre>
              </div>
            </div>
          )}
        </div>

        {/* 관리 필요 내역 영역 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-semibold text-gray-800">관리 필요 내역</h2>
            {selectedTicketIds.length > 0 && (
              <Button onClick={handleCompleteTickets} variant="primary">
                처리 완료 ({selectedTicketIds.length})
              </Button>
            )}
          </div>
          {reviewTickets && reviewTickets.content.length > 0 ? (
            <>
              <table className="w-full border-collapse">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4">
                      <input
                        type="checkbox"
                        checked={selectedTicketIds.length === reviewTickets.content.length}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedTicketIds(reviewTickets.content.map((t) => t.id))
                          } else {
                            setSelectedTicketIds([])
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
                  {reviewTickets.content.map((ticket) => (
                    <tr key={ticket.id} className="border-b border-gray-100">
                      <td className="py-3 px-4">
                        <input
                          type="checkbox"
                          checked={selectedTicketIds.includes(ticket.id)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedTicketIds([...selectedTicketIds, ticket.id])
                            } else {
                              setSelectedTicketIds(selectedTicketIds.filter((id) => id !== ticket.id))
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
                  총 {reviewTickets.page.totalElements}개 중 {reviewTickets.page.number * reviewTickets.page.size + 1}-
                  {Math.min((reviewTickets.page.number + 1) * reviewTickets.page.size, reviewTickets.page.totalElements)}개
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
                    disabled={currentPage >= reviewTickets.page.totalPages - 1}
                    variant="cancel"
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <div className="text-center py-8 text-gray-500">관리 필요 내역이 없습니다.</div>
          )}
        </div>
      </div>
    </AdminLayout>
  )
}
