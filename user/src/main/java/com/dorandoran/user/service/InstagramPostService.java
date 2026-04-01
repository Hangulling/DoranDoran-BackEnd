package com.dorandoran.user.service;

import com.dorandoran.user.dto.PostAssetResponse;
import com.dorandoran.user.dto.PostResponse;
import com.dorandoran.user.dto.PostResponseV2;
import com.dorandoran.user.entity.PostCache;
import com.dorandoran.user.repository.PostCacheRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramPostService {

    private static final String GRAPH_API_BASE_URL = "https://graph.instagram.com";
    private static final String MEDIA_FIELDS = "id,caption,media_url,media_type,thumbnail_url,permalink,timestamp,children{media_type,media_url,thumbnail_url}";
    private static final String CHILDREN_FIELDS = "media_type,media_url,thumbnail_url";

    private final RestTemplate restTemplate;
    private final PostCacheRepository postCacheRepository;
    private final ObjectMapper objectMapper;

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
            URI uri = UriComponentsBuilder.fromHttpUrl(GRAPH_API_BASE_URL + "/" + target + "/media")
                .queryParam("fields", MEDIA_FIELDS)
                .queryParam("access_token", accessToken)
                .queryParam("limit", limit)
                .build()
                .encode()
                .toUri();
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
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
                PostCache cache = buildPostCacheFromItem(id, item);
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
            URI uri = UriComponentsBuilder.fromHttpUrl(GRAPH_API_BASE_URL + "/" + externalId)
                .queryParam("fields", MEDIA_FIELDS)
                .queryParam("access_token", accessToken)
                .build()
                .encode()
                .toUri();
            Map<String, Object> item = restTemplate.getForObject(uri, Map.class);
            if (item == null || !item.containsKey("id")) {
                return Optional.empty();
            }
            String id = safeString(item.get("id"));
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            PostCache cache = buildPostCacheFromItem(id, item);
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
        String mediaType = (cache.getMediaType() != null && !cache.getMediaType().isBlank())
            ? cache.getMediaType()
            : inferMediaType(imageUrl);
        String coverImageUrl = (cache.getCoverImageUrl() != null && !cache.getCoverImageUrl().isBlank())
            ? cache.getCoverImageUrl()
            : imageUrl;
        List<PostAssetResponse> assets = parseAssets(cache.getAssets());
        if (assets.isEmpty() && imageUrl != null && !imageUrl.isBlank()) {
            assets = List.of(new PostAssetResponse(mediaType, imageUrl, null));
        }
        return new PostResponseV2(
            cache.getExternalId(),
            cache.getTitle(),
            imageUrl,
            cache.getDescription(),
            cache.getPermalink(),
            cache.getPublishedAt(),
            mediaType,
            coverImageUrl,
            assets
        );
    }

    /** DB assets JSON → List<PostAssetResponse>, 실패 시 빈 리스트 */
    private List<PostAssetResponse> parseAssets(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("assets JSON 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /** Instagram API 응답 → PostCache (assets, coverImageUrl 포함) */
    private PostCache buildPostCacheFromItem(String id, Map<String, Object> item) {
        String mediaTypeRaw = safeString(item.get("media_type"));
        String mediaType = normalizeMediaType(mediaTypeRaw);
        String mediaUrl = safeString(item.get("media_url"));
        String thumbnailUrl = safeString(item.get("thumbnail_url"));

        List<PostAssetResponse> assetList;
        String coverImageUrl;

        if ("CAROUSEL_ALBUM".equals(mediaType)) {
            assetList = buildAssetsFromChildren(item.get("children"));
            if (assetList.isEmpty()) {
                assetList = fetchChildrenFromApi(id);
            }
            coverImageUrl = assetList.isEmpty() ? mediaUrl : (assetList.get(0).thumbnailUrl() != null ? assetList.get(0).thumbnailUrl() : assetList.get(0).url());
        } else if ("VIDEO".equals(mediaType)) {
            assetList = List.of(new PostAssetResponse("VIDEO", mediaUrl, thumbnailUrl));
            coverImageUrl = thumbnailUrl != null && !thumbnailUrl.isBlank() ? thumbnailUrl : mediaUrl;
        } else {
            assetList = List.of(new PostAssetResponse("IMAGE", mediaUrl, null));
            coverImageUrl = mediaUrl;
        }

        String assetsJson = toAssetsJson(assetList);
        String imageUrl = coverImageUrl != null ? coverImageUrl : mediaUrl;

        return PostCache.builder()
            .externalId(id)
            .title(extractTitle(safeString(item.get("caption"))))
            .description(safeString(item.get("caption")))
            .imageUrl(imageUrl)
            .mediaType(mediaType)
            .coverImageUrl(coverImageUrl)
            .assets(assetsJson)
            .permalink(safeString(item.get("permalink")))
            .publishedAt(parseTimestamp(safeString(item.get("timestamp"))))
            .build();
    }

    /** CAROUSEL_ALBUM용: /{media-id}/children API 별도 호출 (인라인 children 미지원 시 fallback) */
    @SuppressWarnings("unchecked")
    private List<PostAssetResponse> fetchChildrenFromApi(String mediaId) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(GRAPH_API_BASE_URL + "/" + mediaId + "/children")
                .queryParam("fields", CHILDREN_FIELDS)
                .queryParam("access_token", accessToken)
                .build()
                .encode()
                .toUri();
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response == null || !response.containsKey("data")) {
                return List.of();
            }
            Object dataObj = response.get("data");
            if (!(dataObj instanceof List)) {
                return List.of();
            }
            return buildAssetsFromRawChildren((List<?>) dataObj);
        } catch (Exception e) {
            log.warn("캐러셀 children 조회 실패: mediaId={}, error={}", mediaId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<PostAssetResponse> buildAssetsFromRawChildren(List<?> dataList) {
        List<PostAssetResponse> result = new ArrayList<>();
        for (Object childObj : dataList) {
            if (!(childObj instanceof Map)) {
                continue;
            }
            Map<String, Object> child = (Map<String, Object>) childObj;
            String childType = normalizeMediaType(safeString(child.get("media_type")));
            if (childType == null || (!"IMAGE".equals(childType) && !"VIDEO".equals(childType))) {
                childType = "IMAGE";
            }
            String url = safeString(child.get("media_url"));
            String thumb = safeString(child.get("thumbnail_url"));
            if (url != null && !url.isBlank()) {
                result.add(new PostAssetResponse(childType, url, thumb));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<PostAssetResponse> buildAssetsFromChildren(Object childrenObj) {
        if (childrenObj == null || !(childrenObj instanceof Map)) {
            return List.of();
        }
        Object dataObj = ((Map<String, Object>) childrenObj).get("data");
        if (!(dataObj instanceof List)) {
            return List.of();
        }
        return buildAssetsFromRawChildren((List<?>) dataObj);
    }

    private String toAssetsJson(List<PostAssetResponse> assets) {
        if (assets == null || assets.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(assets);
        } catch (Exception e) {
            log.warn("assets JSON 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    /** API media_type 정규화: IMAGE, VIDEO, CAROUSEL_ALBUM */
    private String normalizeMediaType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim().toUpperCase();
        return ("IMAGE".equals(v) || "VIDEO".equals(v) || "CAROUSEL_ALBUM".equals(v)) ? v : null;
    }

    /** URL에서 미디어 타입 추론 (.mp4 → VIDEO, 그 외 IMAGE) - media_type 미저장 시 fallback */
    private String inferMediaType(String url) {
        if (url == null || url.isBlank()) {
            return "IMAGE";
        }
        String path = url.split("\\?")[0].toLowerCase();
        if (path.endsWith(".mp4") || path.contains(".mp4") || path.contains("/video")) {
            return "VIDEO";
        }
        return "IMAGE";
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
