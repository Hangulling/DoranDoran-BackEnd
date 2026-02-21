// Auth endpoints
export const AUTH_ENDPOINTS = {
  // 로그인
  LOGIN: '/api/auth/login',

  // 소셜 로그인
  OAUTH_LOGIN: '/api/auth/oauth/login',

  // 로그아웃
  LOGOUT: '/api/auth/logout',

  // 토큰 검증
  VALIDATE_TOKEN: '/api/auth/validate',

  // 토큰 갱신
  REFRESH_TOKEN: '/api/auth/refresh',

  // 비밀번호 재설정 코드 요청
  PASSWORD_RESET_REQUEST: '/api/auth/password/reset/request-code',

  // 비밀번호 재설정 코드 검증
  PASSWORD_RESET_VERIFY: '/api/auth/password/reset/verify-code',

  // 비밀번호 재설정 실행
  PASSWORD_RESET_EXECUTE: '/api/auth/password/reset/execute',

  // 현재 사용자 정보 조회
  CURRENT_USER: '/api/auth/me',

  // 헬스체크
  HEALTH: '/api/auth/health',
}

// Users endpoints (어드민에서 사용자 정보 조회용)
export const USER_ENDPOINTS = {
  // 사용자 조회
  GET_BY_ID: (userId: string) => `/api/users/${userId}`,
}
