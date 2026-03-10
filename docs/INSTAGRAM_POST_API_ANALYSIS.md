# INSTAGRAM_POST_RENDERING_GUIDE 적용 대상 API 분석 보고

`docs/INSTAGRAM_POST_RENDERING_GUIDE.md`에서 정의한 **홈 인스타그램 게시물 API**에 해당하는 igu 트리 구현을 분석한 결과입니다.

---

## 1. 적용 대상 API (igu 트리)

가이드에서 말하는 “홈 게시글 API”와 **1:1 대응**하는 엔드포인트는 아래 두 개입니다.

| 가이드 명세 | igu 구현 | 컨트롤러·서비스 |
|-------------|----------|------------------|
| `GET /api/home/posts` (리스트, 최대 6개) | **동일** | `HomeController.getHomePosts()` → `InstagramPostService.getHomePosts()` |
| `GET /api/home/posts/{externalId}` (상세) | **동일** | `HomeController.getHomePost()` → `InstagramPostService.getPostByExternalId()` |

- **컨트롤러**: [user/.../controller/HomeController.java](user/src/main/java/com/dorandoran/user/controller/HomeController.java)  
  - `@RequestMapping("/api/home")`  
  - `GET /posts`, `GET /posts/{externalId}`
- **서비스**: [user/.../service/InstagramPostService.java](user/src/main/java/com/dorandoran/user/service/InstagramPostService.java)  
  - Graph API 호출, `PostCache` 저장/조회, `PostResponse` 반환
- **엔티티**: [user/.../entity/PostCache.java](user/src/main/java/com/dorandoran/user/entity/PostCache.java)  
  - `posts_cache` 테이블 매핑
- **응답 DTO**: [user/.../dto/PostResponse.java](user/src/main/java/com/dorandoran/user/dto/PostResponse.java)

---

## 2. 가이드 대비 현재 구현 상태

### 2.1 방식 A – “우리 카드 UI” (기본 필드)

가이드 2.2에서 제시한 **최소 응답 필드**와 현재 igu 응답은 다음과 같습니다.

| 필드 | 가이드 | igu PostResponse | 비고 |
|------|--------|-------------------|------|
| externalId | ○ | ○ | 일치 |
| title | ○ | ○ | 일치 |
| imageUrl | ○ | ○ | 일치 |
| description | ○ | ○ | 일치 |
| permalink | ○ | ○ | 일치 |
| publishedAt | ○ | ○ | 일치 |

→ **기본 “카드 UI” 렌더링용 필드는 가이드와 일치합니다.**

### 2.2 방식 A – 캐러셀/비디오 확장 (가이드 2.2.1 권장)

가이드 2.2.1에서 권장하는 **확장 응답**은 아래와 같습니다.

| 항목 | 가이드 권장 | igu 현재 | 비고 |
|------|-------------|----------|------|
| mediaType | CAROUSEL_ALBUM, VIDEO 등 | **없음** | 미구현 |
| coverImageUrl | 대표 썸네일 | **없음** (imageUrl만 있음) | 미구현 |
| assets | `[{ type, url, thumbnailUrl? }]` | **없음** | 미구현 |

- **PostCache / posts_cache**: `external_id`, `title`, `image_url`, `description`, `permalink`, `published_at`, `fetched_at` 만 존재.  
  `media_type`, `cover_image_url`, `assets`(JSON 등) 컬럼 없음.
- **Instagram Graph API 호출**:  
  - 현재 요청 필드: `id,caption,media_url,permalink,timestamp`  
  - `media_type`, `thumbnail_url`, 캐러셀용 `children`(및 children의 `media_url` 등) 미요청.

→ **캐러셀/비디오/멀티미디어를 “가이드 권장 형태”로 내려주려면**,  
  Graph API 요청 확장 + PostCache/PostResponse/DB 스키마 확장이 필요합니다.

### 2.3 방식 B – Embed (가이드 3.2 B-2)

가이드에서 제안한 **임베드 전용 API**:

- `GET /api/home/posts/{externalId}/embed`  
  - 응답 예: `externalId`, `permalink`, `provider`, `embedHtml`, `width`, `height`

→ igu에는 **해당 경로 및 embed 관련 로직 없음**.  
  (방식 B를 쓰지 않으면 불필요하고, 쓸 경우 신규 구현 필요.)

---

## 3. 요약

| 구분 | 내용 |
|------|------|
| **가이드 적용 대상 API** | `GET /api/home/posts`, `GET /api/home/posts/{externalId}` (igu에 동일 경로·역할로 존재) |
| **기본 응답(방식 A 최소)** | 가이드 2.2와 일치. 현재 igu 구현으로 “카드 UI + permalink 이동”까지 적용 가능. |
| **확장 응답(방식 A 권장)** | mediaType, coverImageUrl, assets 미구현. 적용하려면 Graph API·엔티티·DTO·DB 확장 필요. |
| **Embed API(방식 B-2)** | 미구현. 필요 시 `GET /api/home/posts/{externalId}/embed` 신규 구현. |

**결론**:  
- **“적용할 만한 API”**는 **`GET /api/home/posts`** 와 **`GET /api/home/posts/{externalId}`** 두 개가 igu 트리에서 그대로 해당합니다.  
- 이미 가이드의 **기본 노출 방식(방식 A 최소)**에는 맞춰져 있고,  
- **캐러셀/비디오(2.2.1)** 또는 **Embed(3.2 B-2)**까지 가이드대로 적용하려면 위와 같은 확장/신규 작업이 필요합니다.
