package com.dorandoran.user.controller;

import com.dorandoran.user.dto.PushDeliveryLogResponse;
import com.dorandoran.user.service.PushDeliveryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/logs")
@RequiredArgsConstructor
public class PushDeliveryLogController {

    private final PushDeliveryLogService pushDeliveryLogService;

    @GetMapping
    public ResponseEntity<List<PushDeliveryLogResponse>> getLogs(
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) UUID chatroomId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sentDate,
        @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(pushDeliveryLogService.findLogs(userId, chatroomId, sentDate, limit));
    }
}
