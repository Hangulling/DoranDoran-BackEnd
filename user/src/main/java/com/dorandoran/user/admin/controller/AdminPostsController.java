package com.dorandoran.user.admin.controller;

import com.dorandoran.common.response.ApiResponse;
import com.dorandoran.user.admin.service.AdminPostsService;
import com.dorandoran.user.admin.service.AdminPostsService.AdminHomePostDto;
import com.dorandoran.user.admin.service.AdminPostsService.SocialPostBackupDto;
import com.dorandoran.user.admin.service.AdminPostsService.SyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 관리자용 게시글 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@Tag(name = "Admin Posts", description = "관리자 게시글 관리 API")
public class AdminPostsController {

    private final AdminPostsService adminPostsService;

    @GetMapping
    @Operation(summary = "운영 원본 게시글 목록 조회")
    public ResponseEntity<ApiResponse<Page<AdminHomePostDto>>> getPosts(
            @RequestHeader("X-User-Id") String adminId,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminPostsService.getPosts(scope, keyword, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "운영 원본 게시글 단건 조회")
    public ResponseEntity<ApiResponse<AdminHomePostDto>> getPost(
            @RequestHeader("X-User-Id") String adminId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminPostsService.getPost(id)));
    }

    @PostMapping("/import")
    @Operation(summary = "백업에서 운영 원본으로 가져오기")
    public ResponseEntity<ApiResponse<AdminHomePostDto>> importPost(
            @RequestHeader("X-User-Id") String adminId,
            @RequestBody Map<String, Object> body
    ) {
        Long backupId = Long.valueOf(body.get("backupId").toString());
        boolean isMainHome = Boolean.parseBoolean(body.getOrDefault("isMainHome", false).toString());
        Integer displayOrder = body.containsKey("displayOrder") && body.get("displayOrder") != null
                ? Integer.valueOf(body.get("displayOrder").toString())
                : null;
        AdminHomePostDto result = adminPostsService.importFromBackup(backupId, isMainHome, displayOrder, adminId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "운영 원본 게시글 수정 (노출/순서/활성 상태)")
    public ResponseEntity<ApiResponse<AdminHomePostDto>> updatePost(
            @RequestHeader("X-User-Id") String adminId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        Boolean isMainHome = body.containsKey("isMainHome") ? Boolean.parseBoolean(body.get("isMainHome").toString()) : null;
        Integer displayOrder = (body.containsKey("displayOrder") && body.get("displayOrder") != null)
                ? Integer.valueOf(body.get("displayOrder").toString()) : null;
        Boolean isActive = body.containsKey("isActive") ? Boolean.parseBoolean(body.get("isActive").toString()) : null;
        AdminHomePostDto result = adminPostsService.updatePost(id, isMainHome, displayOrder, isActive, adminId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "운영 원본 게시글 소프트 삭제")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @RequestHeader("X-User-Id") String adminId,
            @PathVariable Long id
    ) {
        adminPostsService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/reorder")
    @Operation(summary = "메인홈 게시글 순서 일괄 저장")
    public ResponseEntity<ApiResponse<Void>> reorder(
            @RequestHeader("X-User-Id") String adminId,
            @RequestBody Map<String, Object> body
    ) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        adminPostsService.reorder(items, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/backup")
    @Operation(summary = "소셜 백업 목록 조회")
    public ResponseEntity<ApiResponse<Page<SocialPostBackupDto>>> getBackupPosts(
            @RequestHeader("X-User-Id") String adminId,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminPostsService.getBackupPosts(source, page, size)));
    }

    @PostMapping("/sync")
    @Operation(summary = "소셜 백업 수동 동기화 트리거")
    public ResponseEntity<ApiResponse<SyncResult>> syncBackup(
            @RequestHeader("X-User-Id") String adminId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String source = (body != null) ? body.get("source") : "ALL";
        SyncResult result = adminPostsService.syncBackup(source);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
