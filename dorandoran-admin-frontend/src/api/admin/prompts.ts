const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export interface PromptTestRequest {
  agentType: string;
  concept: string;
  intimacyLevel: number;
  inputText: string;
  promptVersionId?: number;
}

export interface PromptTestResponse {
  outputText: string;
  latencyMs: number;
  tokens?: number;
}

export interface PromptActiveResponse {
  env: string;
  agentType: string;
  concept: string;
  intimacyLevel: number;
  versionId: number;
  version: string;
  content: string;
  activatedAt: string;
}

export interface PromptVersionCreateRequest {
  agentType: string;
  concept: string;
  intimacyLevel: number;
  content: string;
  memo?: string;
}

export interface PromptVersionResponse {
  id: number;
  agentType: string;
  concept: string;
  intimacyLevel: number;
  version: string;
  content: string;
  filePath: string;
  memo?: string;
  createdBy: string;
  createdByName?: string;
  createdAt: string;
  isActive?: boolean;
}

export interface AgentTypeOption {
  value: string;
  label: string;
}

export interface ConceptOption {
  value: string;
  label: string;
}

export interface IntimacyLevelOption {
  value: number;
  label: string;
}

export interface PromptSaveAndActivateRequest {
  env: string;
  agentType: string;
  concept: string;
  intimacyLevel: number;
  content: string;
  memo?: string;
}

export interface VersionActiveStatusResponse {
  versionId: number;
  isActive: boolean;
  env: string;
  activatedAt?: string;
  activatedBy?: string;
}

export interface PromptVersionListResponse {
  content: PromptVersionResponse[];
  page: {
    number: number;
    size: number;
    totalPages: number;
    totalElements: number;
  };
}

export interface PromptActivateRequest {
  env: string;
  agentType: string;
  concept: string;
  intimacyLevel: number;
  versionId: number;
}

export interface PromptRollbackRequest {
  env: string;
  agentType: string;
  concept: string;
  intimacyLevel: number;
  previousVersionId: number;
}

const getAuthHeaders = () => {
  const token = sessionStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
  };
};

/**
 * Active 프롬프트 조회
 */
export const getActivePrompt = async (
  agentType: string,
  concept: string,
  intimacyLevel: number,
  env?: string
): Promise<PromptActiveResponse> => {
  const params = new URLSearchParams({
    agentType,
    concept,
    intimacyLevel: intimacyLevel.toString(),
    ...(env && { env }),
  });

  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/active?${params}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Active 프롬프트 조회 실패');
  }

  return response.json();
};

/**
 * 프롬프트 테스트 실행
 */
export const testPrompt = async (
  agentType: string,
  concept: string,
  intimacyLevel: number,
  inputText: string,
  promptVersionId?: number
): Promise<PromptTestResponse> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/test`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({
      agentType,
      concept,
      intimacyLevel,
      inputText,
      ...(promptVersionId && { promptVersionId }),
    }),
  });

  if (!response.ok) {
    throw new Error('프롬프트 테스트 실패');
  }

  return response.json();
};

/**
 * 새 버전 생성
 */
export const createPromptVersion = async (
  agentType: string,
  concept: string,
  intimacyLevel: number,
  content: string,
  memo?: string
): Promise<PromptVersionResponse> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/versions`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({
      agentType,
      concept,
      intimacyLevel,
      content,
      memo,
    }),
  });

  if (!response.ok) {
    throw new Error('프롬프트 버전 생성 실패');
  }

  return response.json();
};

/**
 * Active 전환
 */
export const activatePrompt = async (
  env: string,
  agentType: string,
  concept: string,
  intimacyLevel: number,
  versionId: number
): Promise<void> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/activate`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({
      env,
      agentType,
      concept,
      intimacyLevel,
      versionId,
    }),
  });

  if (!response.ok) {
    throw new Error('프롬프트 활성화 실패');
  }
};

/**
 * 롤백
 */
export const rollbackPrompt = async (
  env: string,
  agentType: string,
  concept: string,
  intimacyLevel: number,
  previousVersionId: number
): Promise<void> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/rollback`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({
      env,
      agentType,
      concept,
      intimacyLevel,
      previousVersionId,
    }),
  });

  if (!response.ok) {
    throw new Error('프롬프트 롤백 실패');
  }
};

/**
 * 버전 목록 조회
 */
export const getPromptVersions = async (
  agentType: string,
  concept: string,
  intimacyLevel: number,
  page: number = 0,
  size: number = 10,
  env: string = 'prod'
): Promise<PromptVersionListResponse> => {
  const params = new URLSearchParams({
    agentType,
    concept,
    intimacyLevel: intimacyLevel.toString(),
    page: page.toString(),
    size: size.toString(),
    env,
  });

  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/versions?${params}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('프롬프트 버전 목록 조회 실패');
  }

  return response.json();
};

/**
 * 버전 상세 조회
 */
export const getPromptVersion = async (versionId: number, env: string = 'prod'): Promise<PromptVersionResponse> => {
  const params = new URLSearchParams({ env });
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/versions/${versionId}?${params}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('프롬프트 버전 상세 조회 실패');
  }

  return response.json();
};

/**
 * 현재 파일 내용 조회
 */
export const getPromptFileContent = async (
  agentType: string,
  concept: string,
  intimacyLevel: number
): Promise<string> => {
  const params = new URLSearchParams({
    agentType,
    concept,
    intimacyLevel: intimacyLevel.toString(),
  });

  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/file-content?${params}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('프롬프트 파일 내용 조회 실패');
  }

  const data = await response.json();
  return data.content;
};

/**
 * AgentType 옵션 조회
 */
export const getAgentTypeOptions = async (): Promise<AgentTypeOption[]> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/options/agent-types`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('AgentType 옵션 조회 실패');
  }

  return response.json();
};

/**
 * Concept 옵션 조회
 */
export const getConceptOptions = async (): Promise<ConceptOption[]> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/options/concepts`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Concept 옵션 조회 실패');
  }

  return response.json();
};

/**
 * IntimacyLevel 옵션 조회
 */
export const getIntimacyLevelOptions = async (): Promise<IntimacyLevelOption[]> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/options/intimacy-levels`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('IntimacyLevel 옵션 조회 실패');
  }

  return response.json();
};

/**
 * 저장 및 적용 (통합)
 */
export const saveAndActivatePrompt = async (
  request: PromptSaveAndActivateRequest
): Promise<PromptVersionResponse> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/save-and-activate`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error('프롬프트 저장 및 적용 실패');
  }

  return response.json();
};

/**
 * 버전 활성화 상태 조회
 */
export const getVersionActiveStatus = async (
  versionId: number,
  env: string = 'prod'
): Promise<VersionActiveStatusResponse> => {
  const params = new URLSearchParams({ env });
  const response = await fetch(`${API_BASE_URL}/api/admin/prompts/versions/${versionId}/active-status?${params}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('버전 활성화 상태 조회 실패');
  }

  return response.json();
};
