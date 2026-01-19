package com.dorandoran.user.service;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.user.dto.SupportCreateRequest;
import com.dorandoran.user.entity.SupportRequest;
import com.dorandoran.user.entity.SupportRequest.SupportType;
import com.dorandoran.user.repository.SupportRequestRepository;
import com.dorandoran.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {

    private final SupportRequestRepository supportRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public SupportRequest createSupportRequest(UUID userId, String requesterEmail, String requesterName, SupportCreateRequest request) {
        SupportType type = parseType(request.type());
        if (request.content() == null || request.content().isBlank()) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 내용을 입력해주세요.");
        }
        if (type == SupportType.REPORT) {
            if (request.category() == null || request.category().isBlank()) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "신고 이슈를 선택해주세요.");
            }
            if (request.messageId() == null || request.messageContent() == null || request.messageContent().isBlank()) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "신고 대상 메시지 정보를 포함해야 합니다.");
            }
        }

        String resolvedEmail = resolveEmail(userId, requesterEmail);
        String resolvedName = resolveName(userId, requesterName);
        boolean replyRequested = request.replyRequested() != null && request.replyRequested();

        SupportRequest entity = SupportRequest.builder()
            .userId(userId)
            .requesterName(resolvedName)
            .requesterEmail(resolvedEmail)
            .type(type)
            .category(request.category())
            .content(request.content())
            .replyRequested(replyRequested)
            .replyEmail(request.replyEmail())
            .chatroomId(request.chatroomId())
            .messageId(request.messageId())
            .messageContent(request.messageContent())
            .aiResponseSnapshot(request.aiResponseSnapshot())
            .build();

        return supportRequestRepository.save(entity);
    }

    private SupportType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 유형을 입력해주세요.");
        }
        try {
            return SupportType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST, "문의 유형이 올바르지 않습니다.");
        }
    }

    private String resolveEmail(UUID userId, String headerEmail) {
        if (headerEmail != null && !headerEmail.isBlank()) {
            return headerEmail.trim();
        }
        return userRepository.findById(userId)
            .map(user -> user.getEmail())
            .orElseThrow(() -> new DoranDoranException(ErrorCode.USER_NOT_FOUND));
    }

    private String resolveName(UUID userId, String headerName) {
        if (headerName != null && !headerName.isBlank()) {
            return headerName.trim();
        }
        return userRepository.findById(userId)
            .map(user -> user.getName())
            .orElse(null);
    }
}
