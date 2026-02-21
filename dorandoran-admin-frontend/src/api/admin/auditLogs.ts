const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export interface AdminAuditLogResponse {
  id: number;
  adminUserId: string;
  actionType: string;
  targetType?: string | null;
  targetId?: number | null;
  summary?: string | null;
  beforeJson?: Record<string, unknown> | null;
  afterJson?: Record<string, unknown> | null;
  ip?: string | null;
  userAgent?: string | null;
  createdAt: string;
}

export interface AdminAuditLogListResponse {
  content: AdminAuditLogResponse[];
  page: {
    number: number;
    size: number;
    totalPages: number;
    totalElements: number;
  };
}

const getAuthHeaders = () => {
  const token = sessionStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
  };
};

export interface AuditLogQueryParams {
  adminUserId?: string;
  actionType?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export const getAuditLogs = async (params: AuditLogQueryParams): Promise<AdminAuditLogListResponse> => {
  const searchParams = new URLSearchParams();
  if (params.adminUserId) searchParams.set('adminUserId', params.adminUserId);
  if (params.actionType) searchParams.set('actionType', params.actionType);
  if (params.from) searchParams.set('from', params.from);
  if (params.to) searchParams.set('to', params.to);
  searchParams.set('page', String(params.page ?? 0));
  searchParams.set('size', String(params.size ?? 20));

  const response = await fetch(`${API_BASE_URL}/api/admin/audit-logs?${searchParams}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('감사 로그 조회 실패');
  }

  return response.json();
};
