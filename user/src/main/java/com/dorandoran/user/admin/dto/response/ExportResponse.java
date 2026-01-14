package com.dorandoran.user.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내보내기 응답 DTO
 */
@Getter
@Builder
public class ExportResponse {

  // 내보내기 요청 ID
  private UUID exportId;

  // 상태
  private ExportStatus status;

  // 요청 시간
  private LocalDateTime requestedAt;

  // 상태 메시지
  private String message;

  // 다운로드 URL (완료 시)
  private String downloadUrl;

  // 내보내기 상태
  public enum ExportStatus {
    PENDING,        // 대기 중
    PROCESSING,     // 처리 중
    COMPLETED,      // 완료
    FAILED          // 실패
  }
}