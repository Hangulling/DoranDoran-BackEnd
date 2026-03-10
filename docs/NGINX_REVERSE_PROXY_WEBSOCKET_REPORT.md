# Nginx 리버스 프록시 설정 분석 보고서

**서버**: ec2-user@3.21.177.186  
**분석 일시**: 2026-02-26  
**설정 경로**: `/etc/nginx/conf.d/dorandoran.conf`

---

## 1. 현재 구도

| 도메인 | 백엔드 | 용도 | WebSocket 설정 |
|--------|--------|------|----------------|
| **api.doran-chat.com** | localhost:8080 (Gateway) | API, SSE, **WebSocket** | ❌ 없음 |
| chat.doran-chat.com | localhost:8083 (Chat) | Chat 직접 연결 | ✅ 있음 (미사용) |
| auth.doran-chat.com | localhost:8081 | Auth | - |
| user.doran-chat.com | localhost:8082 | User | - |
| store.doran-chat.com | localhost:8084 | Store | - |

프론트엔드는 `wss://api.doran-chat.com/ws/chat/{chatroomId}`로 연결 → **api.doran-chat.com**을 사용합니다.

---

## 2. api.doran-chat.com 설정 (WebSocket 경로)

### 2.1 현재 설정 (location /)

```nginx
location / {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    proxy_read_timeout 600s;
    proxy_connect_timeout 60s;
    proxy_send_timeout 600s;
    proxy_buffering off;
    proxy_cache off;
    
    proxy_http_version 1.1;
    proxy_set_header Connection "";   # ⚠️ 문제: Connection을 빈 문자열로 덮어씀
    proxy_set_header X-Accel-Buffering "no";
}
```

### 2.2 문제점

| 항목 | 내용 |
|------|------|
| `proxy_set_header Connection "";` | 클라이언트의 `Connection: Upgrade`를 `Connection: `(빈 값)으로 덮어씀 |
| `Upgrade` 헤더 미전달 | `proxy_set_header Upgrade $http_upgrade;` 없음 |

WebSocket 업그레이드 요청에는 다음 헤더가 필요합니다.

- `Upgrade: websocket`
- `Connection: Upgrade`

현재 설정은 Connection을 비워 두고 Upgrade를 별도로 전달하지 않아, WebSocket 핸드셰이크에 필요한 헤더가 Gateway/백엔드까지 전달되지 않습니다.

---

## 3. chat.doran-chat.com 설정 (참고용)

chat.doran-chat.com에는 WebSocket용 설정이 이미 있습니다.

```nginx
location / {
    proxy_pass http://localhost:8083;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    ...
}
```

다만, 실제 클라이언트는 api.doran-chat.com으로 연결하므로 이 블록은 WebSocket 트래픽에 사용되지 않습니다.

---

## 4. 권장 조치

### 4.1 api.doran-chat.com에 `/ws/chat/` 전용 location 추가

WebSocket 경로만 별도로 처리하는 location을 추가합니다.

```nginx
# api.doran-chat.com server 블록 내부에 추가

# WebSocket 채팅 엔드포인트 (api.doran-chat.com → Gateway → Chat)
location /ws/chat/ {
    proxy_pass http://localhost:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # WebSocket 긴 연결용 타임아웃
    proxy_read_timeout 600s;
    proxy_connect_timeout 60s;
    proxy_send_timeout 600s;
    
    proxy_buffering off;
    proxy_cache off;
    proxy_set_header X-Accel-Buffering "no";
}
```

이 location은 `/ws/chat/`보다 더 구체적이므로 `location /`보다 우선 적용됩니다.

### 4.2 적용 절차

```bash
# 1. 수정 전 백업
sudo cp /etc/nginx/conf.d/dorandoran.conf /etc/nginx/conf.d/dorandoran.conf.backup.$(date +%Y%m%d)

# 2. 설정 파일 편집
sudo vi /etc/nginx/conf.d/dorandoran.conf

# 3. 문법 확인
sudo nginx -t

# 4. 적용
sudo systemctl reload nginx
```

---

## 5. 적용 후 확인

1. iOS 앱에서 채팅방 진입 → WebSocket 연결
2. Chat 서비스 로그 확인: `docker logs dorandoran-chat --tail 50`
3. `"Handshake failed due to invalid Upgrade header: null"` 미출력 시 정상 처리

---

## 6. 참고: `Connection` 헤더 차이

| 설정 | SSE | WebSocket |
|------|-----|-----------|
| `proxy_set_header Connection "";` | ✅ (keep-alive 비활성화로 의도된 경우) | ❌ 업그레이드 불가 |
| `proxy_set_header Connection "upgrade";` | ❌ SSE와 함께 사용 시 문제 가능 | ✅ 필요 |
| `proxy_set_header Upgrade $http_upgrade;` | 무관 | ✅ 필요 |

SSE용 `location ~ ^/api/chat/stream/`과 WebSocket용 `location /ws/chat/`을 분리하면 충돌 없이 둘 다 동작시킬 수 있습니다.
