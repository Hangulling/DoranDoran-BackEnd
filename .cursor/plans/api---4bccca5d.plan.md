<!-- 4bccca5d-b55b-4c57-8e45-e91bfab6c3d1 11f23601-e4a6-45fe-964f-8c41b137941e -->
# 최신 API 명세서 완전 재작성

## 작업 개요

기존 파일을 삭제하고 최신 컨트롤러 코드 기반으로 완전히 새로운 API 명세서를 작성합니다.

## 작업 단계

### 1. 새 파일 생성

- **API_SPECIFICATION_V2.md** 새로 생성 (기존 파일은 보존)

### 2. 최신 코드 재분석

- User/Auth/Chat 컨트롤러의 최신 엔드포인트 확인
- DTO 최신 필드 구조 확인
- 응답 형식 재확인

### 3. 새 명세서 작성

- 서비스별 포트 정보 (Gateway:8080, Auth:8081, User:8082, Chat:8083)
- MSA 인증 구조 명시
- 각 엔드포인트별 상세 입출력 정보
- 실제 DTO 기반 정확한 필드 구조
- 인증 필요 여부 명확히 표시

### 4. 주요 반영 사항

- Auth Service: ApiResponse<T> 래퍼 사용
- User Service: 일부만 ApiResponse 사용
- Chat Service: 대부분 직접 DTO 반환
- 각 서비스의 응답 형식 차이 명확히 구분