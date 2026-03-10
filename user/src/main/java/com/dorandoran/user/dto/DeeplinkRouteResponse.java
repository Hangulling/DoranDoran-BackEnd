package com.dorandoran.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * GET /api/deeplink/route 응답.
 * path 파싱 결과: 이동할 화면(screen)과 파라미터(params).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeeplinkRouteResponse {
    /** 화면 식별자 (예: chatroom, archive, chatroomCreate) */
    private String screen;
    /** 화면에 전달할 파라미터 (예: chatroomId, storeId) */
    private Map<String, Object> params;
}
