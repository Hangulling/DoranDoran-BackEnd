package com.dorandoran.user.admin.service;

import com.dorandoran.common.exception.DoranDoranException;
import com.dorandoran.common.exception.ErrorCode;
import com.dorandoran.user.entity.AdminHomePost;
import com.dorandoran.user.entity.SocialPostBackup;
import com.dorandoran.user.repository.AdminHomePostRepository;
import com.dorandoran.user.repository.SocialPostBackupRepository;
import com.dorandoran.user.service.InstagramPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPostsService {

    private static final int MAX_MAIN_HOME = 6;

    private final AdminHomePostRepository adminHomePostRepository;
    private final SocialPostBackupRepository socialPostBackupRepository;
    private final InstagramPostService instagramPostService;

    // ============================================================
    // 운영 원본 조회
    // ============================================================

    @Transactional(readOnly = true)
    public Page<AdminHomePostDto> getPosts(String scope, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        if (keyword != null && !keyword.isBlank()) {
            return adminHomePostRepository.searchByKeyword(keyword, pageable).map(this::toDto);
        }
        if ("main".equalsIgnoreCase(scope)) {
            return adminHomePostRepository.findByIsMainHome(true, pageable).map(this::toDto);
        }
        return adminHomePostRepository.findAllActive(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminHomePostDto getPost(Long id) {
        return toDto(findActiveOrThrow(id));
    }

    // ============================================================
    // 백업에서 운영 원본으로 가져오기
    // ============================================================

    @Transactional
    public AdminHomePostDto importFromBackup(Long backupId, boolean isMainHome, Integer displayOrder, String adminId) {
        if (isMainHome) {
            long currentMain = adminHomePostRepository.countMainHome();
            if (currentMain >= MAX_MAIN_HOME) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST,
                        "메인홈은 최대 " + MAX_MAIN_HOME + "개의 게시글만 가능합니다. 기존 게시글을 내린 후 다시 시도해주세요.");
            }
        }

        SocialPostBackup backup = socialPostBackupRepository.findById(backupId)
                .orElseThrow(() -> new DoranDoranException(ErrorCode.INVALID_REQUEST, "백업 게시글을 찾을 수 없습니다."));

        AdminHomePost post = AdminHomePost.builder()
                .sourceDomain(backup.getSourceDomain())
                .externalId(backup.getExternalId())
                .title(backup.getTitle())
                .description(backup.getDescription())
                .permalink(backup.getPermalink())
                .mediaType(backup.getMediaType())
                .coverImageUrl(backup.getCoverImageUrl())
                .assets(backup.getAssets())
                .isMainHome(isMainHome)
                .displayOrder(displayOrder)
                .isActive(true)
                .publishedAt(backup.getPublishedAt())
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();

        return toDto(adminHomePostRepository.save(post));
    }

    // ============================================================
    // 운영 원본 수정 (노출/순서/활성 상태만)
    // ============================================================

    @Transactional
    public AdminHomePostDto updatePost(Long id, Boolean isMainHome, Integer displayOrder, Boolean isActive, String adminId) {
        AdminHomePost post = findActiveOrThrow(id);

        if (isMainHome != null) {
            if (isMainHome && !post.isMainHome()) {
                long currentMain = adminHomePostRepository.countMainHome();
                if (currentMain >= MAX_MAIN_HOME) {
                    throw new DoranDoranException(ErrorCode.INVALID_REQUEST,
                            "메인홈은 최대 " + MAX_MAIN_HOME + "개의 게시글만 가능합니다.");
                }
            }
            post.setMainHome(isMainHome);
        }
        if (displayOrder != null) {
            post.setDisplayOrder(displayOrder);
        }
        if (isActive != null) {
            post.setActive(isActive);
        }
        post.setUpdatedBy(adminId);

        return toDto(adminHomePostRepository.save(post));
    }

    // ============================================================
    // 소프트 삭제
    // ============================================================

    @Transactional
    public void deletePost(Long id) {
        AdminHomePost post = findActiveOrThrow(id);
        post.setDeletedAt(LocalDateTime.now());
        adminHomePostRepository.save(post);
    }

    // ============================================================
    // 메인홈 순서 일괄 저장
    // ============================================================

    @Transactional
    public void reorder(List<Map<String, Object>> items, String adminId) {
        if (items.size() > MAX_MAIN_HOME) {
            throw new DoranDoranException(ErrorCode.INVALID_REQUEST,
                    "메인홈은 최대 " + MAX_MAIN_HOME + "개의 게시글만 가능합니다.");
        }
        for (Map<String, Object> item : items) {
            Long postId = Long.valueOf(item.get("id").toString());
            Integer order = Integer.valueOf(item.get("displayOrder").toString());
            AdminHomePost post = findActiveOrThrow(postId);
            if (!post.isMainHome()) {
                throw new DoranDoranException(ErrorCode.INVALID_REQUEST,
                        "메인홈 게시글만 순서 변경이 가능합니다. id=" + postId);
            }
            post.setDisplayOrder(order);
            post.setUpdatedBy(adminId);
            adminHomePostRepository.save(post);
        }
    }

    // ============================================================
    // 소셜 백업 조회
    // ============================================================

    @Transactional(readOnly = true)
    public Page<SocialPostBackupDto> getBackupPosts(String source, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<SocialPostBackup> result;
        if (source != null && !source.isBlank() && !source.equalsIgnoreCase("ALL")) {
            result = socialPostBackupRepository.findBySourceDomainPaged(source.toUpperCase(), pageable);
        } else {
            result = socialPostBackupRepository.findAllPaged(pageable);
        }
        return result.map(this::toBackupDto);
    }

    // ============================================================
    // 소셜 동기화 수동 트리거
    // ============================================================

    @Transactional
    public SyncResult syncBackup(String source) {
        int count = 0;
        String src = (source == null || source.isBlank() || source.equalsIgnoreCase("ALL"))
                ? "ALL" : source.toUpperCase();

        if ("ALL".equals(src) || "INSTAGRAM".equals(src)) {
            try {
                count += instagramPostService.syncToBackup();
            } catch (Exception e) {
                log.error("Instagram 동기화 실패: {}", e.getMessage(), e);
            }
        }
        return new SyncResult(count, src, LocalDateTime.now());
    }

    // ============================================================
    // DTO 변환 & 내부 유틸
    // ============================================================

    private AdminHomePost findActiveOrThrow(Long id) {
        return adminHomePostRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DoranDoranException(ErrorCode.INVALID_REQUEST, "게시글을 찾을 수 없습니다."));
    }

    private AdminHomePostDto toDto(AdminHomePost post) {
        return new AdminHomePostDto(
                post.getId(),
                post.getSourceDomain(),
                post.getExternalId(),
                post.getTitle(),
                post.getDescription(),
                post.getPermalink(),
                post.getMediaType(),
                post.getCoverImageUrl(),
                post.getAssets(),
                post.isMainHome(),
                post.getDisplayOrder(),
                post.isActive(),
                post.getPublishedAt(),
                post.getCreatedBy(),
                post.getUpdatedBy(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private SocialPostBackupDto toBackupDto(SocialPostBackup backup) {
        return new SocialPostBackupDto(
                backup.getId(),
                backup.getSourceDomain(),
                backup.getExternalId(),
                backup.getTitle(),
                backup.getCoverImageUrl(),
                backup.getMediaType(),
                backup.getPermalink(),
                backup.getFetchedAt()
        );
    }

    // ============================================================
    // DTO 레코드 정의
    // ============================================================

    public record AdminHomePostDto(
            Long id,
            String sourceDomain,
            String externalId,
            String title,
            String description,
            String permalink,
            String mediaType,
            String coverImageUrl,
            String assets,
            boolean isMainHome,
            Integer displayOrder,
            boolean isActive,
            LocalDateTime publishedAt,
            String createdBy,
            String updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record SocialPostBackupDto(
            Long id,
            String sourceDomain,
            String externalId,
            String title,
            String coverImageUrl,
            String mediaType,
            String permalink,
            LocalDateTime fetchedAt
    ) {}

    public record SyncResult(
            int syncedCount,
            String source,
            LocalDateTime latestFetchedAt
    ) {}
}
