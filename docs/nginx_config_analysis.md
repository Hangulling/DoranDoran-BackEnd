# Nginx 설정 분석 보고서

**분석 일시**: 2025-01-15  
**서버**: EC2 (3.21.177.186)  
**Nginx 버전**: 확인 필요 (systemd 기반)

---

## 1. 현재 설정 개요

### 1.1 메인 설정 파일 (`/etc/nginx/nginx.conf`)

**기본 설정**:
- `worker_processes auto`: CPU 코어 수에 맞춰 자동 설정
- `worker_connections 1024`: 워커당 최대 연결 수
- `keepalive_timeout 65`: Keep-Alive 타임아웃 65초
- `sendfile on`: 효율적인 파일 전송 활성화

**설정 파일 구조**:
- 메인 설정: `/etc/nginx/nginx.conf`
- 서비스별 설정: `/etc/nginx/conf.d/*.conf` (모듈화)

### 1.2 서비스별 설정 (`/etc/nginx/conf.d/dorandoran.conf`)

**구성된 서비스**:
1. **API Gateway** (api.doran-chat.com:443 → localhost:8080)
2. **Chat Service** (chat.doran-chat.com:443 → localhost:8083)
3. **Auth Service** (auth.doran-chat.com:443 → localhost:8081)
4. **User Service** (user.doran-chat.com:443 → localhost:8082)
5. **Store Service** (store.doran-chat.com:443 → localhost:8084)
6. **HTTP → HTTPS 리다이렉트** (포트 80)

---

## 2. 상세 분석

### 2.1 API Gateway 설정 (SSE 지원)

**현재 설정**:
```nginx
server {
    listen 443 ssl;
    http2 on;
    server_name api.doran-chat.com;
    
    # SSL 인증서
    ssl_certificate /etc/letsencrypt/live/api.doran-chat.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.doran-chat.com/privkey.pem;
    
    # 일반 API 요청
    location / {
        proxy_pass http://localhost:8080;
        proxy_read_timeout 600s;      # SSE용 긴 타임아웃
        proxy_buffering off;          # SSE용 버퍼링 비활성화
        proxy_cache off;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }
    
    # SSE 스트림 엔드포인트 특별 설정
    location ~ ^/api/chat/stream/ {
        proxy_pass http://localhost:8080;
        proxy_read_timeout 600s;
        proxy_buffering off;
        proxy_cache off;
        proxy_request_buffering off;  # 요청 버퍼링도 비활성화
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header X-Accel-Buffering "no";
    }
}
```

**분석**:
- ✅ SSE 지원을 위한 설정이 잘 되어 있음
- ✅ 타임아웃 600초 (10분)로 충분
- ✅ 버퍼링 완전 비활성화로 실시간 스트리밍 지원
- ⚠️ SSE 엔드포인트에 중복 설정 (일반 location과 거의 동일)

**개선 제안**:
- SSE 엔드포인트 설정을 더 구체화하거나 일반 location에서 충분할 수 있음
- `proxy_request_buffering off`는 SSE에 유용 (요청 버퍼링도 비활성화)

### 2.2 Chat Service 설정 (WebSocket 지원)

**현재 설정**:
```nginx
server {
    listen 443 ssl;
    http2 on;
    server_name chat.doran-chat.com;
    
    location / {
        proxy_pass http://localhost:8083;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 600s;
        proxy_buffering off;
    }
}
```

**분석**:
- ✅ WebSocket 업그레이드 헤더 설정됨
- ✅ 타임아웃 600초로 충분
- ⚠️ `X-Forwarded-For` 등 프록시 헤더 누락
- ⚠️ SSE도 지원해야 하는데 WebSocket만 설정

**개선 제안**:
- 프록시 헤더 추가 (X-Real-IP, X-Forwarded-For 등)
- SSE와 WebSocket 모두 지원하도록 설정 개선

### 2.3 기타 서비스 설정 (Auth, User, Store)

**현재 설정**:
- 기본 프록시 설정만 있음
- 타임아웃 설정 없음 (기본값 사용)
- 버퍼링 설정 없음

**분석**:
- ✅ 기본 기능은 동작
- ⚠️ 타임아웃 명시적 설정 권장
- ⚠️ 성능 최적화 설정 부족

**개선 제안**:
- 타임아웃 명시적 설정
- 버퍼링 최적화
- 연결 풀 설정

### 2.4 HTTP → HTTPS 리다이렉트

**현재 설정**:
```nginx
server {
    listen 80;
    server_name api.doran-chat.com chat.doran-chat.com ...;
    return 301 https://$server_name$request_uri;
}
```

**분석**:
- ✅ 모든 HTTP 요청을 HTTPS로 리다이렉트
- ✅ 301 영구 리다이렉트 사용 (SEO 친화적)

---

## 3. 문제점 및 개선 사항

### 3.1 발견된 문제

1. **SSE 엔드포인트 중복 설정**:
   - 일반 location과 SSE location이 거의 동일
   - SSE location만 `proxy_request_buffering off` 추가

2. **Chat Service 헤더 누락**:
   - `X-Real-IP`, `X-Forwarded-For` 등 프록시 헤더 누락
   - 클라이언트 IP 추적 어려움

3. **기타 서비스 최적화 부족**:
   - Auth, User, Store 서비스에 타임아웃/버퍼링 설정 없음

4. **보안 헤더 부족**:
   - `X-Forwarded-Proto` 등 보안 관련 헤더 일부 누락

### 3.2 개선 제안

**1. SSE 설정 최적화**:
```nginx
# SSE 엔드포인트를 더 구체적으로 설정
location ~ ^/api/chat/stream/ {
    proxy_pass http://localhost:8080;
    proxy_read_timeout 600s;
    proxy_connect_timeout 60s;
    proxy_send_timeout 600s;
    
    # 버퍼링 완전 비활성화
    proxy_buffering off;
    proxy_cache off;
    proxy_request_buffering off;
    
    # HTTP/1.1 강제
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_set_header X-Accel-Buffering "no";
    
    # 프록시 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

**2. Chat Service 개선**:
```nginx
location / {
    proxy_pass http://localhost:8083;
    proxy_http_version 1.1;
    
    # WebSocket 업그레이드
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    
    # 프록시 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # 타임아웃
    proxy_read_timeout 600s;
    proxy_connect_timeout 60s;
    proxy_send_timeout 600s;
    
    # 버퍼링
    proxy_buffering off;
}
```

**3. 기타 서비스 최적화**:
```nginx
# Auth, User, Store 서비스에 공통 설정 추가
location / {
    proxy_pass http://localhost:8081;  # 또는 8082, 8084
    
    # 프록시 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # 타임아웃
    proxy_read_timeout 45s;      # 일반 API는 45초
    proxy_connect_timeout 10s;
    proxy_send_timeout 45s;
    
    # 버퍼링 최적화
    proxy_buffering on;          # 일반 API는 버퍼링 활성화
    proxy_buffer_size 4k;
    proxy_buffers 8 4k;
}
```

**4. 메인 설정 개선**:
```nginx
http {
    # 연결 업그레이드 맵 (WebSocket용)
    map $http_upgrade $connection_upgrade {
        default upgrade;
        '' close;
    }
    
    # 기타 최적화
    client_max_body_size 10M;
    client_body_buffer_size 128k;
    
    # Gzip 압축
    gzip on;
    gzip_vary on;
    gzip_min_length 1000;
    gzip_types text/plain text/css application/json application/javascript;
}
```

---

## 4. 보안 검토

### 4.1 현재 보안 설정

**양호한 점**:
- ✅ HTTPS 강제 (HTTP → HTTPS 리다이렉트)
- ✅ SSL 인증서 사용 (Let's Encrypt)
- ✅ HTTP/2 활성화

**개선 필요**:
- ⚠️ 보안 헤더 부족 (HSTS, X-Frame-Options 등)
- ⚠️ SSL 설정 최적화 필요

### 4.2 보안 헤더 추가 제안

```nginx
# 모든 서버 블록에 추가
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
```

---

## 5. 성능 최적화

### 5.1 현재 성능 설정

**양호한 점**:
- ✅ `sendfile on`: 효율적인 파일 전송
- ✅ `keepalive_timeout 65`: 적절한 Keep-Alive 설정
- ✅ `worker_processes auto`: CPU 코어에 맞춰 자동 설정

**개선 필요**:
- ⚠️ `worker_connections 1024`: 트래픽 증가 시 부족할 수 있음
- ⚠️ Gzip 압축 미설정
- ⚠️ 캐싱 설정 없음

### 5.2 성능 최적화 제안

```nginx
http {
    # 연결 수 증가
    worker_connections 2048;
    
    # Gzip 압축
    gzip on;
    gzip_vary on;
    gzip_min_length 1000;
    gzip_comp_level 6;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    
    # 정적 파일 캐싱
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 6. 모니터링 및 로깅

### 6.1 현재 로깅 설정

**현재**:
- Access log: `/var/log/nginx/access.log`
- Error log: `/var/log/nginx/error.log` (notice 레벨)

**로그 포맷**:
```
$remote_addr - $remote_user [$time_local] "$request" 
$status $body_bytes_sent "$http_referer" 
"$http_user_agent" "$http_x_forwarded_for"
```

**개선 제안**:
- JSON 형식 로그로 변경 (Loki 연동 용이)
- 응답 시간 추가
- 요청 ID 추가

---

## 7. 종합 평가

### 7.1 현재 상태

**점수**: 7/10

**강점**:
- ✅ SSE 지원 설정이 잘 되어 있음
- ✅ HTTPS 강제 및 SSL 인증서 사용
- ✅ 서비스별 도메인 분리
- ✅ HTTP/2 활성화

**약점**:
- ⚠️ 일부 서비스 설정 불완전
- ⚠️ 보안 헤더 부족
- ⚠️ 성능 최적화 설정 부족
- ⚠️ 모니터링을 위한 로그 포맷 개선 필요

### 7.2 우선순위별 개선 사항

**높은 우선순위**:
1. Chat Service 프록시 헤더 추가
2. 기타 서비스 타임아웃 설정 추가
3. 보안 헤더 추가

**중간 우선순위**:
4. Gzip 압축 활성화
5. 로그 포맷 개선 (JSON)
6. 성능 최적화 (worker_connections 증가)

**낮은 우선순위**:
7. 정적 파일 캐싱
8. 연결 업그레이드 맵 추가

---

## 8. 권장 액션 아이템

1. **즉시 적용** (보안):
   - 보안 헤더 추가
   - SSL 설정 최적화

2. **단기 개선** (1주일 내):
   - Chat Service 헤더 추가
   - 기타 서비스 타임아웃 설정
   - Gzip 압축 활성화

3. **중기 개선** (1개월 내):
   - 로그 포맷 개선
   - 성능 최적화
   - 모니터링 연동

---

**분석 완료**: 2025-01-15
















