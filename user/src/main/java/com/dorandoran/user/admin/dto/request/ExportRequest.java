package com.dorandoran.user.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 채팅 로그 내보내기 요청 DTO
 */
@Getter
@Setter
public class ExportRequest {

  // 시작일 (필수)
  @NotNull(message = "시작일은 필수입니다")
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  // 종료일 (선택, 미입력 시 오늘)
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  // 채팅방 필터 (선택)
  private UUID chatroomId;

  // 친밀도 필터 (선택)
  private Integer intimacyLevel;

  // 내보내기 형식 (필수)
  @NotNull(message = "내보내기 형식은 필수입니다")
  private ExportFormat format;

  // 내보내기 범위 (필수)
  @NotNull(message = "내보내기 범위는 필수입니다")
  private ExportScope scope;

  /**
   * 내보내기 형식
   */
  public enum ExportFormat {
    CSV,
    JSON
  }

  /**
   * 내보내기 범위
   */
  public enum ExportScope {
    MESSAGES_ONLY,          // 메시지만
    WITH_AGENT_RESULTS      // Agent 결과 포함
  }
}