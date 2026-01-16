package com.dorandoran.store.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 봇 타입별 보관 수 응답 DTO
 *
 * 사용자 대시보드에서 각 봇 타입별로 몇 개씩 저장했는지 표시
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "봇 타입별 보관 수 응답")
public class BookmarkCountByBotResponse implements Serializable {

  @Schema(description = "친구 봇 보관 수", example = "15")
  private Long friendCount;

  @Schema(description = "연인 봇 보관 수", example = "8")
  private Long honeyCount;

  @Schema(description = "동료 봇 보관 수", example = "3")
  private Long coworkerCount;

  @Schema(description = "선배 봇 보관 수", example = "12")
  private Long seniorCount;

  @Schema(description = "전체 보관 수", example = "38")
  private Long totalCount;

  /**
   * 전체 개수 계산
   */
  public void calculateTotal() {
    this.totalCount = (friendCount != null ? friendCount : 0L)
        + (honeyCount != null ? honeyCount : 0L)
        + (coworkerCount != null ? coworkerCount : 0L)
        + (seniorCount != null ? seniorCount : 0L);
  }
}