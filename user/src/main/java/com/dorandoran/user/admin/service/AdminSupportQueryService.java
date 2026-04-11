package com.dorandoran.user.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.user.admin.dto.response.AdminSupportDetailResponse;
import com.dorandoran.user.admin.dto.response.AdminSupportPageResponse;
import com.dorandoran.user.admin.dto.response.AdminSupportSummaryResponse;
import com.dorandoran.user.entity.SupportRequest;
import com.dorandoran.user.entity.SupportRequest.SupportType;
import com.dorandoran.user.repository.SupportRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSupportQueryService {

    private final SupportRequestRepository supportRequestRepository;

    @Transactional(readOnly = true)
    public AdminSupportPageResponse getAllSupportRequests(
            String type,
            UUID userId,
            String category,
            Boolean replyRequested,
            String status,
            String from,
            String to,
            int page,
            int size
    ) {
        // size 제한 (최대 100)
        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<SupportRequest> spec = Specification.where(
                (root, query, cb) -> cb.isNull(root.get("deletedAt")));

        // type 필터
        if (type != null && !type.isBlank()) {
            try {
                SupportType supportType = SupportType.valueOf(type.trim().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), supportType));
            } catch (IllegalArgumentException e) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 유형이 올바르지 않습니다.");
            }
        }

        // userId 필터
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }

        // category 필터
        if (category != null && !category.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category.trim()));
        }

        // replyRequested 필터
        if (replyRequested != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("replyRequested"), replyRequested));
        }

        // status 필터
        if (status != null && !status.isBlank()) {
            String upperStatus = status.trim().toUpperCase();
            if (!upperStatus.equals("PENDING") && !upperStatus.equals("COMPLETED")) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "status는 PENDING 또는 COMPLETED 이어야 합니다.");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), upperStatus));
        }

        // 날짜 필터
        if (from != null && !from.isBlank()) {
            LocalDateTime fromDate = parseDateTime(from);
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }
        if (to != null && !to.isBlank()) {
            LocalDateTime toDate = parseDateTime(to);
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }

        Page<SupportRequest> pageResult = supportRequestRepository.findAll(spec, pageable);

        return AdminSupportPageResponse.builder()
                .content(pageResult.getContent().stream()
                        .map(this::toSummaryResponse)
                        .collect(Collectors.toList()))
                .page(AdminSupportPageResponse.PageInfo.builder()
                        .number(pageResult.getNumber())
                        .size(pageResult.getSize())
                        .totalPages(pageResult.getTotalPages())
                        .totalElements(pageResult.getTotalElements())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminSupportDetailResponse getSupportRequestDetail(Long id) {
        SupportRequest request = supportRequestRepository.findById(id)
                .orElseThrow(() -> new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 내역을 찾을 수 없습니다."));

        return toDetailResponse(request);
    }

    private AdminSupportSummaryResponse toSummaryResponse(SupportRequest request) {
        String contentPreview = request.getContent();
        if (contentPreview != null && contentPreview.length() > 200) {
            contentPreview = contentPreview.substring(0, 200) + "...";
        }

        return new AdminSupportSummaryResponse(
                request.getId(),
                request.getUserId(),
                request.getRequesterName(),
                request.getRequesterEmail(),
                request.getType().name(),
                request.getCategory(),
                contentPreview,
                request.getCreatedAt(),
                request.isReplyRequested(),
                request.getChatroomId(),
                request.getMessageId(),
                request.getStatus(),
                request.getAnsweredBy(),
                request.getAnsweredAt()
        );
    }

    private AdminSupportDetailResponse toDetailResponse(SupportRequest request) {
        return new AdminSupportDetailResponse(
                request.getId(),
                request.getUserId(),
                request.getRequesterName(),
                request.getRequesterEmail(),
                request.getType().name(),
                request.getCategory(),
                request.getContent(),
                request.getCreatedAt(),
                request.isReplyRequested(),
                request.getReplyEmail(),
                request.getChatroomId(),
                request.getMessageId(),
                request.getMessageContent(),
                request.getAiResponseSnapshot(),
                request.getStatus(),
                request.getAnswerContent(),
                request.getAnsweredBy(),
                request.getAnsweredAt()
        );
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            // ISO-8601 형식 시도 (YYYY-MM-DDTHH:mm:ss)
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            // 날짜만 있는 경우 (YYYY-MM-DD) -> 자정으로 설정
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "날짜 형식이 올바르지 않습니다. (예: YYYY-MM-DD 또는 YYYY-MM-DDTHH:mm:ss)");
        }
    }
}
