package com.dorandoran.user.admin.service;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.user.admin.dto.response.AdminSupportDetailResponse;
import com.dorandoran.user.entity.SupportRequest;
import com.dorandoran.user.repository.SupportRequestRepository;
import com.dorandoran.user.service.SupportEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSupportCommandService {

    private final SupportRequestRepository supportRequestRepository;
    private final SupportEmailService supportEmailService;

    /**
     * 답변 저장 + 이메일 발송 + status=COMPLETED 자동 전환
     */
    @Transactional
    public AdminSupportReplyResult reply(Long id, String answerContent, String adminName) {
        SupportRequest request = findActiveOrThrow(id);

        request.setAnswerContent(answerContent);
        request.setAnsweredBy(adminName);
        request.setAnsweredAt(LocalDateTime.now());
        request.setStatus("COMPLETED");

        supportRequestRepository.save(request);

        String toEmail = (request.getReplyEmail() != null && !request.getReplyEmail().isBlank())
                ? request.getReplyEmail()
                : request.getRequesterEmail();

        boolean emailSent = false;
        try {
            emailSent = supportEmailService.sendReplyEmail(
                    toEmail, request.getRequesterName(), request.getCategory(), answerContent);
        } catch (Exception e) {
            log.error("답변 이메일 발송 중 예외: supportId={}, to={}, error={}", id, toEmail, e.getMessage(), e);
        }

        return new AdminSupportReplyResult(toDetailResponse(request), emailSent);
    }

    /**
     * 상태만 수동 변경
     */
    @Transactional
    public AdminSupportDetailResponse updateStatus(Long id, String status) {
        String upper = status.trim().toUpperCase();
        if (!upper.equals("PENDING") && !upper.equals("COMPLETED")) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "status는 PENDING 또는 COMPLETED 이어야 합니다.");
        }
        SupportRequest request = findActiveOrThrow(id);
        request.setStatus(upper);
        supportRequestRepository.save(request);
        return toDetailResponse(request);
    }

    /**
     * 소프트 삭제
     */
    @Transactional
    public void delete(Long id) {
        SupportRequest request = findActiveOrThrow(id);
        request.setDeletedAt(LocalDateTime.now());
        supportRequestRepository.save(request);
    }

    private SupportRequest findActiveOrThrow(Long id) {
        SupportRequest request = supportRequestRepository.findById(id)
                .orElseThrow(() -> new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 내역을 찾을 수 없습니다."));
        if (request.getDeletedAt() != null) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "이미 삭제된 문의입니다.");
        }
        return request;
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

    public record AdminSupportReplyResult(AdminSupportDetailResponse detail, boolean emailSent) {}
}
