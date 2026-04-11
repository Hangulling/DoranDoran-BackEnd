package com.dorandoran.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.user.dto.SupportDetailResponse;
import com.dorandoran.user.dto.SupportPageResponse;
import com.dorandoran.user.dto.SupportSummaryResponse;
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
public class SupportQueryService {

    private final SupportRequestRepository supportRequestRepository;

    @Transactional(readOnly = true)
    public SupportPageResponse getUserSupportRequests(
            UUID userId,
            String type,
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

        Specification<SupportRequest> spec = Specification.where((root, query, cb) -> 
            cb.equal(root.get("userId"), userId));

        // type 필터
        if (type != null && !type.isBlank()) {
            try {
                SupportType supportType = SupportType.valueOf(type.trim().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), supportType));
            } catch (IllegalArgumentException e) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 유형이 올바르지 않습니다.");
            }
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

        return SupportPageResponse.builder()
                .content(pageResult.getContent().stream()
                        .map(this::toSummaryResponse)
                        .collect(Collectors.toList()))
                .page(SupportPageResponse.PageInfo.builder()
                        .number(pageResult.getNumber())
                        .size(pageResult.getSize())
                        .totalPages(pageResult.getTotalPages())
                        .totalElements(pageResult.getTotalElements())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public SupportDetailResponse getUserSupportRequestDetail(UUID userId, Long id) {
        SupportRequest request = supportRequestRepository.findById(id)
                .orElseThrow(() -> new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 내역을 찾을 수 없습니다."));

        // 본인 것만 조회 가능 (타인의 것은 404로 처리)
        if (!request.getUserId().equals(userId)) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 내역을 찾을 수 없습니다.");
        }

        return toDetailResponse(request);
    }

    private SupportSummaryResponse toSummaryResponse(SupportRequest request) {
        String contentPreview = request.getContent();
        if (contentPreview != null && contentPreview.length() > 200) {
            contentPreview = contentPreview.substring(0, 200) + "...";
        }

        return new SupportSummaryResponse(
                request.getId(),
                request.getType().name(),
                request.getCategory(),
                contentPreview,
                request.getCreatedAt(),
                request.isReplyRequested(),
                request.getChatroomId(),
                request.getMessageId()
        );
    }

    private SupportDetailResponse toDetailResponse(SupportRequest request) {
        return new SupportDetailResponse(
                request.getId(),
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
                request.getRequesterEmail(),
                request.getRequesterName()
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
