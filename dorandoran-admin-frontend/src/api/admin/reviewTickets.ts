const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export interface ReviewTicketResponse {
  id: number;
  conversationId: string;
  status: string;
  agentType?: string;
  note?: string;
  createdBy: string;
  createdByName?: string;
  assignee?: string;
  assigneeName?: string;
  createdAt: string;
  updatedAt: string;
  doneAt?: string;
}

export interface ReviewTicketItemResponse {
  messageId?: string;
  agentType?: string;
  snapshotJson?: Record<string, unknown>;
}

export interface ReviewTicketDetailResponse extends ReviewTicketResponse {
  items: ReviewTicketItemResponse[];
}

export interface ReviewTicketListResponse {
  content: ReviewTicketResponse[];
  page: {
    number: number;
    size: number;
    totalPages: number;
    totalElements: number;
  };
}

export interface ReviewTicketCountsResponse {
  total: number;
  byAgentType: Record<string, number>;
}

export interface ReviewTicketUpdateRequest {
  note: string;
}

export interface ReviewTicketCompleteRequest {
  ticketIds: number[];
}

export interface ReviewTicketCreateRequest {
  conversationId?: string;
  agentType?: string;
  note?: string;
  items?: {
    messageId?: string;
    agentType?: string;
    snapshotJson?: Record<string, unknown>;
  }[];
}

const getAuthHeaders = () => {
  const token = sessionStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
  };
};

/**
 * 티켓 목록 조회
 */
export const getReviewTickets = async (
  status: string = 'OPEN',
  agentType?: string,
  page: number = 0,
  size: number = 20
): Promise<ReviewTicketListResponse> => {
  const params = new URLSearchParams({
    status,
    page: page.toString(),
    size: size.toString(),
    ...(agentType && { agentType }),
  });

  const response = await fetch(`${API_BASE_URL}/api/admin/review-tickets?${params}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('티켓 목록 조회 실패');
  }

  return response.json();
};

/**
 * 탭별 카운트 조회
 */
export const getReviewTicketCounts = async (status: string = 'OPEN'): Promise<ReviewTicketCountsResponse> => {
  const params = new URLSearchParams({ status });

  const response = await fetch(`${API_BASE_URL}/api/admin/review-tickets/counts?${params}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('티켓 카운트 조회 실패');
  }

  return response.json();
};

/**
 * 티켓 메모 수정
 */
export const updateReviewTicket = async (
  ticketId: number,
  note: string
): Promise<ReviewTicketResponse> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/review-tickets/${ticketId}`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
    body: JSON.stringify({ note }),
  });

  if (!response.ok) {
    throw new Error('티켓 메모 수정 실패');
  }

  return response.json();
};

/**
 * 티켓 삭제
 */
export const deleteReviewTicket = async (ticketId: number): Promise<void> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/review-tickets/${ticketId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('티켓 삭제 실패');
  }
};

/**
 * 다건 처리 완료
 */
export const completeReviewTickets = async (ticketIds: number[]): Promise<void> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/review-tickets/complete`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ ticketIds }),
  });

  if (!response.ok) {
    throw new Error('티켓 처리 완료 실패');
  }
};

/**
 * 티켓 생성
 */
export const createReviewTicket = async (
  request: ReviewTicketCreateRequest
): Promise<ReviewTicketDetailResponse> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/review-tickets`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error('티켓 생성 실패');
  }

  return response.json();
};

/**
 * 티켓 상세 조회
 */
export const getReviewTicketDetail = async (ticketId: number): Promise<ReviewTicketDetailResponse> => {
  const response = await fetch(`${API_BASE_URL}/api/admin/review-tickets/${ticketId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('티켓 상세 조회 실패');
  }

  return response.json();
};
