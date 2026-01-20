import { useState, useEffect } from 'react'
import AdminLayout from '../../components/admin/AdminLayout'
import Button from '../../components/common/Button'
import {
  getPromptVersions,
  getPromptVersion,
  activatePrompt,
  rollbackPrompt,
  getAgentTypeOptions,
  getConceptOptions,
  getIntimacyLevelOptions,
  type PromptVersionListResponse,
  type PromptVersionResponse,
  type AgentTypeOption,
  type ConceptOption,
  type IntimacyLevelOption,
} from '../../api/admin/prompts'

export default function PromptVersionsPage() {
  const [agentType, setAgentType] = useState('INTIMACY_ANALYSIS')
  const [concept, setConcept] = useState('friend')
  const [intimacyLevel, setIntimacyLevel] = useState(1)
  const [versions, setVersions] = useState<PromptVersionListResponse | null>(null)
  const [selectedVersion, setSelectedVersion] = useState<PromptVersionResponse | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isVersionListModalOpen, setIsVersionListModalOpen] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  
  // 드롭다운 옵션
  const [agentTypeOptions, setAgentTypeOptions] = useState<AgentTypeOption[]>([])
  const [conceptOptions, setConceptOptions] = useState<ConceptOption[]>([])
  const [intimacyLevelOptions, setIntimacyLevelOptions] = useState<IntimacyLevelOption[]>([])
  const [isLoadingOptions, setIsLoadingOptions] = useState(false)

  // 드롭다운 옵션 로드
  useEffect(() => {
    loadOptions()
  }, [])

  useEffect(() => {
    loadVersions()
  }, [agentType, concept, intimacyLevel, currentPage])

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

  const loadVersions = async () => {
    try {
      setIsLoading(true)
      const response = await getPromptVersions(agentType, concept, intimacyLevel, currentPage, 10, 'prod')
      setVersions(response)
    } catch (error) {
      console.error('버전 목록 로드 실패:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleViewVersion = async (versionId: number) => {
    try {
      const version = await getPromptVersion(versionId, 'prod')
      setSelectedVersion(version)
      setIsModalOpen(true)
    } catch (error) {
      console.error('버전 상세 조회 실패:', error)
      alert('버전 상세 조회에 실패했습니다.')
    }
  }

  const handleOpenVersionListModal = () => {
    setIsVersionListModalOpen(true)
  }

  const handleDeploy = async (versionId: number) => {
    if (!confirm('이 버전을 배포하시겠습니까?')) {
      return
    }

    try {
      setIsLoading(true)
      await activatePrompt('prod', agentType, concept, intimacyLevel, versionId)
      alert('버전이 배포되었습니다.')
      await loadVersions()
    } catch (error) {
      console.error('버전 배포 실패:', error)
      alert('버전 배포에 실패했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleRollback = async (versionId: number) => {
    if (!confirm('이 버전으로 롤백하시겠습니까?')) {
      return
    }

    try {
      setIsLoading(true)
      await rollbackPrompt('prod', agentType, concept, intimacyLevel, versionId)
      alert('롤백이 완료되었습니다.')
      await loadVersions()
    } catch (error) {
      console.error('롤백 실패:', error)
      alert('롤백에 실패했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <AdminLayout>
      <div className="w-full p-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">프롬프트 버전 관리</h1>
          <p className="text-lg text-gray-600">프롬프트 버전을 조회하고 관리할 수 있습니다.</p>
        </div>

        {/* 필터 영역 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200 mb-6">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">에이전트</label>
              <select
                value={agentType}
                onChange={(e) => {
                  setAgentType(e.target.value)
                  setCurrentPage(0)
                }}
                disabled={isLoadingOptions}
                className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
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
                onChange={(e) => {
                  setConcept(e.target.value)
                  setCurrentPage(0)
                }}
                disabled={isLoadingOptions}
                className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
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
                onChange={(e) => {
                  setIntimacyLevel(Number(e.target.value))
                  setCurrentPage(0)
                }}
                disabled={isLoadingOptions}
                className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
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
          </div>
        </div>

        {/* 버전 리스트 */}
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-semibold text-gray-800">버전 목록</h2>
            <Button onClick={handleOpenVersionListModal} variant="confirm">
              버전 관리 모달
            </Button>
          </div>
          {isLoading ? (
            <div className="text-center py-8 text-gray-500">로딩 중...</div>
          ) : versions && versions.content.length > 0 ? (
            <>
              <table className="w-full border-collapse">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">버전</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">생성 시간</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">작성자</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">메모</th>
                    <th className="text-left py-3 px-4 text-sm font-medium text-gray-700">작업</th>
                  </tr>
                </thead>
                <tbody>
                  {versions.content.map((version) => (
                    <tr 
                      key={version.id} 
                      className={`border-b border-gray-100 ${version.isActive ? 'bg-blue-50' : ''}`}
                    >
                      <td className="py-3 px-4 text-sm font-medium">
                        {version.version}
                        {version.isActive && (
                          <span className="ml-2 text-xs text-blue-600 font-semibold">(현재 버전)</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-sm">{new Date(version.createdAt).toLocaleString()}</td>
                      <td className="py-3 px-4 text-sm">{version.createdByName || version.createdBy}</td>
                      <td className="py-3 px-4 text-sm">{version.memo || '-'}</td>
                      <td className="py-3 px-4">
                        <div className="flex gap-2">
                          <Button
                            onClick={() => handleViewVersion(version.id)}
                            variant="text"
                            size="sm"
                          >
                            조회
                          </Button>
                          {!version.isActive && (
                            <>
                              <Button
                                onClick={() => handleDeploy(version.id)}
                                variant="confirm"
                                size="sm"
                                disabled={isLoading}
                              >
                                배포
                              </Button>
                              <Button
                                onClick={() => handleRollback(version.id)}
                                variant="cancel"
                                size="sm"
                                disabled={isLoading}
                              >
                                롤백
                              </Button>
                            </>
                          )}
                          {version.isActive && (
                            <Button
                              variant="confirm"
                              size="sm"
                              disabled
                              className="bg-blue-500 text-white"
                            >
                              현재 버전
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="mt-4 flex justify-between items-center">
                <div className="text-sm text-gray-600">
                  총 {versions.page.totalElements}개 중 {versions.page.number * versions.page.size + 1}-
                  {Math.min((versions.page.number + 1) * versions.page.size, versions.page.totalElements)}개
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
                    disabled={currentPage >= versions.page.totalPages - 1}
                    variant="cancel"
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <div className="text-center py-8 text-gray-500">버전이 없습니다.</div>
          )}
        </div>

        {/* 버전 목록 모달 (스크린샷 참고) */}
        {isVersionListModalOpen && versions && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl p-6 max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-800">프롬프트 버전 관리</h2>
                <button
                  onClick={() => setIsVersionListModalOpen(false)}
                  className="text-gray-500 hover:text-gray-700 text-2xl"
                >
                  ×
                </button>
              </div>
              <div className="space-y-3">
                {versions.content.map((version) => (
                  <div
                    key={version.id}
                    className={`p-4 rounded-lg border ${
                      version.isActive
                        ? 'bg-blue-50 border-blue-200'
                        : 'bg-white border-gray-200'
                    }`}
                  >
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <div className="text-lg font-semibold text-gray-800 mb-1">
                          {version.version}
                        </div>
                        <div className="text-sm text-gray-600 mb-1">
                          {new Date(version.createdAt).toLocaleString()}
                        </div>
                        <div className="text-sm text-gray-600">
                          작성자: {version.createdByName || version.createdBy}
                        </div>
                      </div>
                      <div className="flex gap-2 ml-4">
                        {version.isActive ? (
                          <Button
                            variant="confirm"
                            size="sm"
                            disabled
                            className="bg-blue-500 text-white"
                          >
                            현재 버전
                          </Button>
                        ) : (
                          <>
                            <Button
                              onClick={async () => {
                                try {
                                  const versionDetail = await getPromptVersion(version.id, 'prod')
                                  setSelectedVersion(versionDetail)
                                  setIsVersionListModalOpen(false)
                                  setIsModalOpen(true)
                                } catch (error) {
                                  console.error('버전 상세 조회 실패:', error)
                                  alert('버전 상세 조회에 실패했습니다.')
                                }
                              }}
                              variant="text"
                              size="sm"
                              className="bg-blue-50 text-blue-600 hover:bg-blue-100"
                            >
                              조회
                            </Button>
                            <Button
                              onClick={() => {
                                if (confirm('이 버전으로 롤백하시겠습니까?')) {
                                  handleRollback(version.id)
                                  setIsVersionListModalOpen(false)
                                }
                              }}
                              variant="text"
                              size="sm"
                              className="bg-blue-50 text-blue-600 hover:bg-blue-100"
                              disabled={isLoading}
                            >
                              롤백
                            </Button>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
              <div className="mt-6 flex justify-end">
                <Button onClick={() => setIsVersionListModalOpen(false)} variant="cancel">
                  닫기
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* 버전 상세 모달 */}
        {isModalOpen && selectedVersion && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-xl p-6 max-w-4xl w-full mx-4 max-h-[80vh] overflow-y-auto">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-800">버전 상세</h2>
                <button
                  onClick={() => setIsModalOpen(false)}
                  className="text-gray-500 hover:text-gray-700 text-2xl"
                >
                  ×
                </button>
              </div>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">버전</label>
                  <div className="text-base">{selectedVersion.version}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">생성 시간</label>
                  <div className="text-base">{new Date(selectedVersion.createdAt).toLocaleString()}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">작성자</label>
                  <div className="text-base">{selectedVersion.createdByName || selectedVersion.createdBy}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">메모</label>
                  <div className="text-base">{selectedVersion.memo || '-'}</div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">프롬프트 내용</label>
                  <div className="w-full h-96 px-4 py-3 border border-gray-300 rounded-lg bg-gray-50 overflow-y-auto">
                    <pre className="whitespace-pre-wrap text-sm font-mono">{selectedVersion.content}</pre>
                  </div>
                </div>
              </div>
              <div className="mt-6 flex justify-end">
                <Button onClick={() => setIsModalOpen(false)} variant="cancel">
                  닫기
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  )
}
