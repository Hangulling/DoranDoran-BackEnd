# 인스타그램 게시물 노출 방식(2안) & API 구성 가이드 (Frontend 참고)

이 문서는 메인 홈(또는 컨텐츠 영역)에서 인스타그램 게시물을 노출하는 방법을 **2가지 렌더링 방식**으로 정리합니다.

- **방식 A (추천)**: Instagram Graph API → 백엔드 캐시/정규화 → **우리 서비스 카드 UI로 렌더링**
- **방식 B**: Instagram Embed(oEmbed/임베드) → **인스타 게시물 UI를 그대로 렌더링**

> 목표: “인스타 게시물”을 안정적으로 노출하면서도, 운영/정책/성능 리스크를 최소화하고 프론트 구현 기준을 명확히 합니다.

---

## 1) 공통 전제 (중요)

- **스크래핑(HTML 파싱) 방식은 권장하지 않습니다.**
  - 약관/차단/DOM 변경 리스크가 높고, 장애 시 홈 화면 품질이 크게 흔들립니다.
- Graph API 기반이라면 **백엔드가 인스타 데이터를 직접 호출하고(토큰/권한 관리), 프론트에는 “우리 API(JSON)”만 노출**하는 구조가 안정적입니다.
- Embed 기반이라면 프론트에 인스타 임베드 스크립트/iframe이 들어가며, **UI 커스터마이징/보안(CSP/XSS)/성능** 제약이 있습니다.

---

## 2) 방식 A: “우리 카드 UI”로 렌더링 (Graph API → 백엔드 캐시 → JSON)

### 2.1 화면에서 어떻게 보이나요?

- 사용자는 홈에서 **우리 앱의 카드 UI**로 인스타 게시물을 봅니다.
- 카드 클릭 시 보통 다음 중 하나로 동작합니다.
  - (권장) **인스타 원문으로 이동**: `permalink` 오픈
  - (선택) **우리 상세 페이지**에서 렌더 후 “인스타에서 보기” 버튼 제공

### 2.2 프론트에서 필요한 API

현재 문서 기준(`docs/API_SPECIFICATION_V3.md`) 홈 게시글 API는 아래와 같습니다.

- 리스트(최대 6개)
  - `GET /api/home/posts`
- 상세
  - `GET /api/home/posts/{externalId}`

프로덕션 호출 예시:

```text
GET https://api.doran-chat.com/api/home/posts
GET https://api.doran-chat.com/api/home/posts/post-001
```

응답 필드는 카드 렌더에 필요한 최소 필드로 구성됩니다.

```json
{
  "externalId": "post-001",
  "title": "AI 챗봇과 함께하는 일상 대화의 즐거움",
  "imageUrl": "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800",
  "description": "캡션/설명(전체 또는 요약)",
  "permalink": "https://www.instagram.com/p/XXXX/",
  "publishedAt": "2026-01-20T01:55:42.843263"
}
```

> 참고: `externalId`는 내부적으로는 인스타 `media_id`를 그대로 쓰는 방식이 일반적이지만, 현재는 `post-001`처럼 별도 키를 쓰는 형태도 가능합니다. 프론트는 문자열로 취급하면 됩니다.

### 2.2.1 여러 장(캐러셀) 이미지는 어떻게 받나요? (권장 응답 형태)

인스타 게시물은 `mediaType`에 따라 이미지/비디오/캐러셀로 나뉩니다. “피그마처럼 우리 UI로 렌더링”하려면 **단일 `imageUrl`만으로는 캐러셀(여러 장) 커버가 어려워서**, 아래처럼 **assets 배열**을 내려주는 구성이 가장 깔끔합니다.

신규(권장) 응답 예시:

```json
{
  "externalId": "17890000000000000",
  "title": "게시글 제목(옵션)",
  "description": "캡션 전체/요약",
  "permalink": "https://www.instagram.com/p/XXXX/",
  "publishedAt": "2026-01-20T01:55:42.843263",
  "mediaType": "CAROUSEL_ALBUM",
  "coverImageUrl": "https://.../cover.jpg",
  "assets": [
    { "type": "IMAGE", "url": "https://.../1.jpg" },
    { "type": "IMAGE", "url": "https://.../2.jpg" },
    { "type": "VIDEO", "url": "https://.../3.mp4", "thumbnailUrl": "https://.../3.jpg" }
  ]
}
```

프론트 렌더링 가이드:
- 홈 리스트에서는 `coverImageUrl`(또는 `assets[0]`)만 썸네일로 사용하면 단순합니다.
- 상세 화면에서만 `assets`를 이용해 캐러셀/갤러리 UI를 구성합니다.

### 2.3 프론트 렌더링 플로우(권장)

- 홈 진입
  - `GET /api/home/posts` 호출
  - 카드 리스트 렌더
    - 썸네일: `imageUrl`
    - 타이틀: `title` (없으면 description 앞부분을 대체로 사용 가능)
    - 설명: `description` (길면 2~3줄 ellipsis)
    - 게시일: `publishedAt` (로컬 포맷)
  - 카드 클릭
    - `permalink` 새 탭/인앱 브라우저로 오픈

### 2.4 프론트 구현 시 고려사항

- **이미지 최적화**
  - `imageUrl`은 외부 호스트(인스타/이미지 CDN)일 수 있으므로 로딩 실패 대비 placeholder 필요
  - lazy loading 권장
- **비디오/캐러셀**
  - 카드 UI에서는 “대표 이미지(썸네일)”만 보여주고, 클릭 시 `permalink`로 이동하는 구성이 단순/안정적
  - 비디오를 홈에서 직접 재생해야 한다면 API가 `mediaType`, `videoUrl` 등을 추가로 내려줘야 함(추가 논의 필요)
- **장애 내성**
  - 백엔드가 인스타에서 동기화 실패하더라도, 홈 API는 “마지막 캐시 데이터”로 응답하는 것이 UX에 유리

---

## 3) 방식 B: “인스타 게시물 UI 그대로” 렌더링 (Embed / oEmbed)

### 3.1 화면에서 어떻게 보이나요?

- 사용자는 홈에서 **인스타그램 임베드 박스(좋아요/작성자/댓글 UI 포함)** 형태를 보게 됩니다.
- 우리 서비스 카드와 완전히 동일한 UI로 맞추기는 어렵고(거의 불가), 인스타 임베드가 제공하는 형태를 따릅니다.

### 3.2 프론트에서 필요한 데이터(최소)

- `permalink` (예: `https://www.instagram.com/p/XXXX/`)

이 `permalink`를 어디서 받느냐에 따라 2가지가 있습니다.

#### B-1) “permalink만 받고” 프론트가 직접 임베드 처리

- API: 방식 A의 `GET /api/home/posts` / `GET /api/home/posts/{externalId}` 응답에 이미 `permalink`가 있으므로 추가 API 없이 가능
- 프론트:
  - 인스타 임베드 스크립트/iframe을 로드하고 permalink 기반으로 렌더

장점:
- 백엔드 변경 최소

주의:
- 임베드 스크립트 로딩/리렌더 타이밍 이슈가 있을 수 있음
- CSP 설정/외부 스크립트 허용 필요(환경에 따라)

#### B-2) 백엔드가 oEmbed 결과를 “프론트 안전 포맷”으로 내려줌 (추천)

프론트에서 “임베드 HTML을 직접 생성/조립”하는 대신, 백엔드가 oEmbed(또는 임베드 생성 로직)를 담당하고 프론트에는 렌더용 payload만 제공합니다.

예시 API(신규 제안):

- `GET /api/home/posts/{externalId}/embed`

예시 응답:

```json
{
  "externalId": "post-001",
  "permalink": "https://www.instagram.com/p/XXXX/",
  "provider": "INSTAGRAM",
  "embedHtml": "<blockquote class=\"instagram-media\">...</blockquote>",
  "width": 540,
  "height": null
}
```

프론트 구현 가이드:
- `embedHtml`은 **그대로 DOM에 삽입**해야 하므로(XSS), 다음 중 하나가 필요합니다.
  - (권장) 서버에서 신뢰 가능한 oEmbed 응답만 저장/전달 + 프론트에서 최소 삽입
  - 삽입 시 Sanitization(화이트리스트) 고려
  - React라면 `dangerouslySetInnerHTML`를 사용할 가능성이 큼

### 3.3 Embed 방식의 트레이드오프 요약

- 장점
  - 인스타 게시물 UI를 그대로 보여줄 수 있음(브랜드/신뢰감)
- 단점
  - 스타일 커스터마이징 제한(우리 카드 UI와 통일감 약해질 수 있음)
  - 성능: 외부 스크립트/리소스 로딩으로 초기 로딩 비용 증가 가능
  - 보안/정책: CSP, 외부 도메인 허용, 임베드 정책 변경 대응 필요

---

## 4) 어떤 방식을 선택하면 좋나요? (결론 가이드)

- **우리 UI로 “컨텐츠 카드”처럼 보여주고, 클릭 시 인스타로 보내는 UX**가 목표라면 → **방식 A (추천)**
- **홈 화면에서 “인스타 게시물 UI 그대로”가 반드시 필요**하다면 → **방식 B**
  - 가능하면 B-2(백엔드가 embed payload 제공)로 안전성/일관성 확보 권장

---

## 5) 프론트 체크리스트

- [ ] 홈 카드형(방식 A)인지, 임베드형(방식 B)인지 화면 요구사항 확정
- [ ] 카드형이라면: `GET /api/home/posts`로 리스트 렌더 + 클릭 시 `permalink` 이동
- [ ] 임베드형이라면: (선택) `embedHtml` 수신 방식(B-2) 여부 결정
- [ ] 이미지/임베드 로딩 실패 시 fallback UI 적용
- [ ] 외부 링크 오픈 정책(새 탭/인앱 브라우저/딥링크) 결정

