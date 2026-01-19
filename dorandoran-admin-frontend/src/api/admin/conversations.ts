const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export interface AdminConversationListItem {
  conversationId: string;
  userId?: string | null;
  roomKey?: string | null;
  intimacyLevel?: number | null;
  lastMessageAt?: string | null;
  lastSequenceNumber?: number | null;
}

export interface AdminConversationListResponse {
  content: AdminConversationListItem[];
  page: {
    number: number;
    size: number;
    totalPages: number;
    totalElements: number;
  };
}

export interface AdminConversationMessageResponse {
  messageId?: string | null;
  senderType: string;
  content: string;
  contentType?: string | null;
  metadata?: string | null;
  sequenceNumber: number;
  turnNumber: number;
  createdAt?: string | null;
}

export interface AdminConversationDetailResponse {
  conversationId: string;
  timeline: AdminConversationMessageResponse[];
}

const getAuthHeaders = () => {
  const token = sessionStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
  };
};

export interface ConversationQueryParams {
  userEmail?: string;
  from?: string;
  to?: string;
  roomKey?: string;
  intimacyLevel?: number;
  dataSource?: 'chat' | 'archive';
  page?: number;
  size?: number;
}

export const getAdminConversations = async (
  params: ConversationQueryParams
): Promise<AdminConversationListResponse> => {
  const searchParams = new URLSearchParams();

  if (params.userEmail) searchParams.set('userEmail', params.userEmail);
  if (params.from) searchParams.set('from', params.from);
  if (params.to) searchParams.set('to', params.to);
  if (params.roomKey) searchParams.set('roomKey', params.roomKey);
  if (params.intimacyLevel !== undefined) searchParams.set('intimacyLevel', String(params.intimacyLevel));
  if (params.dataSource) searchParams.set('dataSource', params.dataSource);

  searchParams.set('page', String(params.page ?? 0));
  searchParams.set('size', String(params.size ?? 20));

  const response = await fetch(`${API_BASE_URL}/api/admin/conversations?${searchParams}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('대화 목록 조회 실패');
  }

  return response.json();
};

export const getAdminConversationDetail = async (
  conversationId: string,
  dataSource: 'chat' | 'archive' = 'chat'
): Promise<AdminConversationDetailResponse> => {
  const searchParams = new URLSearchParams({ dataSource });
  const response = await fetch(
    `${API_BASE_URL}/api/admin/conversations/${conversationId}?${searchParams}`,
    {
      method: 'GET',
      headers: getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error('대화 상세 조회 실패');
  }

  return response.json();
};
