# 메인홈 게시물 API 명세 (인스타그램 피드)

메인홈에 노출되는 인스타그램 게시물 조회 API입니다.  
기존 API(v1)와 캐러셀/비디오 확장 응답을 제공하는 v2 API를 함께 제공합니다.

**Base URL** (User 서비스): `http(s)://{host}:8082`  
**공통 Prefix**: `/api/home`

---

## 1. API 목록

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/home/posts` | 게시물 목록 (최대 6건, 기존 응답) |
| GET | `/api/home/posts/{externalId}` | 게시물 단건 조회 (기존 응답) |
| GET | `/api/home/v2/posts` | 게시물 목록 (최대 6건, **확장 응답**: mediaType, coverImageUrl, assets) |
| GET | `/api/home/v2/posts/{externalId}` | 게시물 단건 조회 (**확장 응답**) |

---

## 2. 기존 API (v1)

### 2.1 게시물 목록 조회

```
GET /api/home/posts
```

**Response**  
- **200 OK**  
  - Body: `PostResponse[]` (최대 6건)

**PostResponse**

| 필드 | 타입 | 설명 |
|------|------|------|
| externalId | string | 인스타그램 미디어 ID |
| title | string \| null | 제목(캡션 앞 60자) |
| imageUrl | string \| null | 대표 이미지 URL |
| description | string \| null | 캡션 전문 |
| permalink | string \| null | 인스타그램 게시물 URL |
| publishedAt | string \| null | 게시 일시 (ISO-8601 local date-time) |

**응답 예시**

```json
[
  {
    "externalId": "12345678901234567",
    "title": "오늘의 한 끼",
    "imageUrl": "https://scontent.cdninstagram.com/...",
    "description": "오늘의 한 끼 #맛집",
    "permalink": "https://www.instagram.com/p/xxx/",
    "publishedAt": "2025-01-28T14:30:00"
  }
]
```

---

### 2.2 게시물 단건 조회

```
GET /api/home/posts/{externalId}
```

**Path Parameters**

| 이름 | 타입 | 설명 |
|------|------|------|
| externalId | string | 인스타그램 미디어 ID |

**Response**  
- **200 OK**  
  - Body: `PostResponse` (2.1과 동일 스키마)
- **404 Not Found**  
  - 해당 게시물이 없거나 비활성화된 경우

---

## 3. 확장 API (v2) — 캐러셀/비디오

캐러셀 앨범·비디오 구성을 프론트에서 그대로 재배치할 수 있도록 `mediaType`, `coverImageUrl`, `assets`를 추가로 반환합니다.

### 3.1 게시물 목록 조회 (v2)

```
GET /api/home/v2/posts
```

**Response**  
- **200 OK**  
  - Body: `PostResponseV2[]` (최대 6건)

**PostResponseV2**

| 필드 | 타입 | 설명 |
|------|------|------|
| externalId | string | 인스타그램 미디어 ID |
| title | string \| null | 제목(캡션 앞 60자) |
| imageUrl | string \| null | 대표 이미지 URL (하위 호환, coverImageUrl과 동일할 수 있음) |
| description | string \| null | 캡션 전문 |
| permalink | string \| null | 인스타그램 게시물 URL |
| publishedAt | string \| null | 게시 일시 (ISO-8601 local date-time) |
| mediaType | string \| null | 미디어 유형: `IMAGE`, `VIDEO`, `CAROUSEL_ALBUM` |
| coverImageUrl | string \| null | 리스트/썸네일용 대표 이미지 URL |
| assets | array \| null | 캐러셀·비디오 구성 목록 (순서 유지) |

**PostAssetResponse** (assets 배열 요소)

| 필드 | 타입 | 설명 |
|------|------|------|
| type | string | `"IMAGE"` 또는 `"VIDEO"` |
| url | string | 미디어 URL (이미지 원본 또는 비디오 재생 URL) |
| thumbnailUrl | string \| null | 비디오일 때 썸네일 URL (IMAGE는 null 가능) |

**응답 예시 — 단일 이미지**

```json
[
  {
    "externalId": "12345678901234567",
    "title": "오늘의 한 끼",
    "imageUrl": "https://scontent.cdninstagram.com/...",
    "description": "오늘의 한 끼 #맛집",
    "permalink": "https://www.instagram.com/p/xxx/",
    "publishedAt": "2025-01-28T14:30:00",
    "mediaType": "IMAGE",
    "coverImageUrl": "https://scontent.cdninstagram.com/...",
    "assets": [
      {
        "type": "IMAGE",
        "url": "https://scontent.cdninstagram.com/...",
        "thumbnailUrl": null
      }
    ]
  }
]
```

**응답 예시 — 캐러셀 앨범**

```json
{
  "externalId": "98765432109876543",
  "title": "주말 브런치",
  "imageUrl": "https://scontent.cdninstagram.com/.../1.jpg",
  "description": "주말 브런치 모음",
  "permalink": "https://www.instagram.com/p/yyy/",
  "publishedAt": "2025-01-27T10:00:00",
  "mediaType": "CAROUSEL_ALBUM",
  "coverImageUrl": "https://scontent.cdninstagram.com/.../1.jpg",
  "assets": [
    { "type": "IMAGE", "url": "https://.../1.jpg", "thumbnailUrl": null },
    { "type": "IMAGE", "url": "https://.../2.jpg", "thumbnailUrl": null },
    { "type": "VIDEO", "url": "https://.../3.mp4", "thumbnailUrl": "https://.../3_thumb.jpg" }
  ]
}
```

**응답 예시 — 비디오**

```json
{
  "externalId": "11122233344455566",
  "title": "Vlog",
  "imageUrl": "https://scontent.cdninstagram.com/.../thumb.jpg",
  "description": "일상 브이로그",
  "permalink": "https://www.instagram.com/p/zzz/",
  "publishedAt": "2025-01-26T18:00:00",
  "mediaType": "VIDEO",
  "coverImageUrl": "https://scontent.cdninstagram.com/.../thumb.jpg",
  "assets": [
    {
      "type": "VIDEO",
      "url": "https://scontent.cdninstagram.com/.../video.mp4",
      "thumbnailUrl": "https://scontent.cdninstagram.com/.../thumb.jpg"
    }
  ]
}
```

---

### 3.2 게시물 단건 조회 (v2)

```
GET /api/home/v2/posts/{externalId}
```

**Path Parameters**

| 이름 | 타입 | 설명 |
|------|------|------|
| externalId | string | 인스타그램 미디어 ID |

**Response**  
- **200 OK**  
  - Body: `PostResponseV2` (3.1과 동일 스키마)
- **404 Not Found**  
  - 해당 게시물이 없거나 비활성화된 경우

---

## 4. 사용 가이드

- **기존 클라이언트**  
  - `/api/home/posts`, `/api/home/posts/{externalId}` 그대로 사용. 응답 스키마 변경 없음.
- **캐러셀/갤러리·비디오 UI가 필요한 클라이언트**  
  - `/api/home/v2/posts`, `/api/home/v2/posts/{externalId}` 사용.
  - 리스트: `coverImageUrl`(또는 `assets[0]`)로 썸네일 표시.
  - 상세: `assets` 순서대로 캐러셀/갤러리 또는 비디오 플레이어 구성.

---

## 5. 참고

- 데이터 소스: Instagram Graph API. User 서비스에서 캐시(`user_schema.posts_cache`)를 채우고 조회합니다.
- v2 응답의 `mediaType`/`assets`가 비어 있거나 null일 수 있습니다(구 캐시 데이터 또는 API 비활성 시). 이 경우 `imageUrl`/`coverImageUrl`만으로 표시하면 됩니다.
