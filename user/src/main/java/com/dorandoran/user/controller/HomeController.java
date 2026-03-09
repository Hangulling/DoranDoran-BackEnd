package com.dorandoran.user.controller;

import com.dorandoran.user.dto.PostResponse;
import com.dorandoran.user.dto.PostResponseV2;
import com.dorandoran.user.service.InstagramPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final InstagramPostService instagramPostService;

    @GetMapping("/posts")
    public ResponseEntity<List<PostResponse>> getHomePosts() {
        return ResponseEntity.ok(instagramPostService.getHomePosts());
    }

    /** v2 API: 확장 응답 (mediaType, coverImageUrl, assets) */
    @GetMapping("/v2/posts")
    public ResponseEntity<List<PostResponseV2>> getHomePostsV2() {
        return ResponseEntity.ok(instagramPostService.getHomePostsV2());
    }

    @GetMapping("/posts/{externalId}")
    public ResponseEntity<PostResponse> getHomePost(@PathVariable String externalId) {
        return instagramPostService.getPostByExternalId(externalId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/v2/posts/{externalId}")
    public ResponseEntity<PostResponseV2> getHomePostV2(@PathVariable String externalId) {
        return instagramPostService.getPostByExternalIdV2(externalId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
