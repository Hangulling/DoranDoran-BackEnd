package com.dorandoran.store.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 에러 응답 DTO
 * 모든 에러를 통일된 형식으로 응답
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

  // 타임스탬프
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  // HTTP 상태 코드
  private int status;

  // 에러 코드 (커스텀)
  private String code;

  // 에러 메시지
  private String message;

  // 에러 상세 설명
  private String detail;

  // 요청 경로
  private String path;

  // 유효성 검증 에러들 (Validation)
  private List<FieldError> errors;

  /**
   * 필드별 유효성 검증 에러
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FieldError {
    private String field;
    private String message;
    private Object rejectedValue;
  }
}