package com.dorandoran.user.service;

import com.dorandoran.user.dto.PostResponse;
import com.dorandoran.user.dto.PostResponseV2;
import com.dorandoran.user.entity.PostCache;
import com.dorandoran.user.repository.PostCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramPostService {

    private final RestTemplate restTemplate;
    private final PostCacheRepository postCacheRepository;

    @Value("${instagram.enabled:false}")
    private boolean enabled;

    @Value("${instagram.access-token:}")
    private String accessToken;

    @Value("${instagram.user-id:}")
    private String userId;

    @Transactional(readOnly = true)
    public List<PostResponse> getHomePosts() {
        List<PostResponse> cached = mapToResponses(postCacheRepository.findTop6ByOrderByFetchedAtDesc());
        if (!enabled || accessToken == null || accessToken.isBlank()) {
            return cached;
        }
        List<PostCache> fetched = fetchFromInstagram(6);
        if (!fetched.isEmpty()) {
            return mapToResponses(fetched);
        }
        return cached;
    }

    @Transactional(readOnly = true)
    public Optional<PostResponse> getPostByExternalId(String externalId) {
        Optional<PostCache> cached = postCacheRepository.findById(externalId);
        if (cached.isPresent()) {
            return cached.map(this::mapToResponse);
        }
        if (!enabled || accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        return fetchSingleFromInstagram(externalId).map(this::mapToResponse);
    }

    /** v2 API: 확장 응답 (mediaType, coverImageUrl, assets). v1과 동일하게 Instagram API 우선, 실패 시 DB 캐시 fallback. */
    @Transactional(readOnly = true)
    public List<PostResponseV2> getHomePostsV2() {
        List<PostCache> cached = postCacheRepository.findTop6ByOrderByFetchedAtDesc();
        if (!enabled || accessToken == null || accessToken.isBlank()) {
            return cached.stream().map(this::mapToResponseV2).toList();
        }
        List<PostCache> fetched = fetchFromInstagram(6);
        if (!fetched.isEmpty()) {
            return fetched.stream().map(this::mapToResponseV2).toList();
        }
        return cached.stream().map(this::mapToResponseV2).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PostResponseV2> getPostByExternalIdV2(String externalId) {
        Optional<PostCache> cached = postCacheRepository.findById(externalId);
        if (cached.isPresent()) {
            return cached.map(this::mapToResponseV2);
        }
        if (!enabled || accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        return fetchSingleFromInstagram(externalId).map(this::mapToResponseV2);
    }

    @Transactional
    protected List<PostCache> fetchFromInstagram(int limit) {
        try {
            String target = (userId == null || userId.isBlank()) ? "me" : userId;
            String url = String.format(
                "https://graph.instagram.com/%s/media?fields=id,caption,media_url,permalink,timestamp&access_token=%s&limit=%d",
                target,
                accessToken,
                limit
            );
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("data")) {
                return List.of();
            }
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            List<PostCache> saved = new ArrayList<>();
            for (Map<String, Object> item : data) {
                String id = safeString(item.get("id"));
                if (id == null || id.isBlank()) {
                    continue;
                }
                PostCache cache = PostCache.builder()
                    .externalId(id)
                    .title(extractTitle(safeString(item.get("caption"))))
                    .description(safeString(item.get("caption")))
                    .imageUrl(safeString(item.get("media_url")))
                    .permalink(safeString(item.get("permalink")))
                    .publishedAt(parseTimestamp(safeString(item.get("timestamp"))))
                    .build();
                postCacheRepository.save(cache);
                saved.add(cache);
            }
            return saved;
        } catch (Exception e) {
            log.warn("인스타그램 피드 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @Transactional
    protected Optional<PostCache> fetchSingleFromInstagram(String externalId) {
        try {
            String url = String.format(
                "https://graph.instagram.com/%s?fields=id,caption,media_url,permalink,timestamp&access_token=%s",
                externalId,
                accessToken
            );
            Map<String, Object> item = restTemplate.getForObject(url, Map.class);
            if (item == null || !item.containsKey("id")) {
                return Optional.empty();
            }
            String id = safeString(item.get("id"));
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            PostCache cache = PostCache.builder()
                .externalId(id)
                .title(extractTitle(safeString(item.get("caption"))))
                .description(safeString(item.get("caption")))
                .imageUrl(safeString(item.get("media_url")))
                .permalink(safeString(item.get("permalink")))
                .publishedAt(parseTimestamp(safeString(item.get("timestamp"))))
                .build();
            return Optional.of(postCacheRepository.save(cache));
        } catch (Exception e) {
            log.warn("인스타그램 게시글 단건 조회 실패: externalId={}, error={}", externalId, e.getMessage());
            return Optional.empty();
        }
    }

    @Scheduled(cron = "${instagram.refresh.cron:0 5 9 * * *}")
    public void refreshCache() {
        if (!enabled) {
            return;
        }
        fetchFromInstagram(6);
    }

    private List<PostResponse> mapToResponses(List<PostCache> caches) {
        return caches.stream().map(this::mapToResponse).toList();
    }

    private PostResponse mapToResponse(PostCache cache) {
        return new PostResponse(
            cache.getExternalId(),
            cache.getTitle(),
            cache.getImageUrl(),
            cache.getDescription(),
            cache.getPermalink(),
            cache.getPublishedAt()
        );
    }

    private PostResponseV2 mapToResponseV2(PostCache cache) {
        String imageUrl = cache.getImageUrl();
        List<com.dorandoran.user.dto.PostAssetResponse> assets = imageUrl != null && !imageUrl.isBlank()
            ? List.of(new com.dorandoran.user.dto.PostAssetResponse("IMAGE", imageUrl, null))
            : Collections.emptyList();
        return new PostResponseV2(
            cache.getExternalId(),
            cache.getTitle(),
            imageUrl,
            cache.getDescription(),
            cache.getPermalink(),
            cache.getPublishedAt(),
            "IMAGE",
            imageUrl,
            assets
        );
    }

    private String extractTitle(String caption) {
        if (caption == null || caption.isBlank()) {
            return null;
        }
        String trimmed = caption.trim();
        return trimmed.length() > 60 ? trimmed.substring(0, 60) : trimmed;
    }

    private String safeString(Object value) {
        return value == null ? null : value.toString();
    }

    private java.time.LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ex) {
            return null;
        }
    }
}
