# 모니터링 시스템 보안 그룹 포트 설정 가이드

## 외부 접근이 필요한 포트

### 필수 포트

#### 1. Grafana (포트 3000) ⭐ 필수
- **용도**: 대시보드 접근
- **URL**: http://3.21.177.186:3000
- **접근 필요성**: 높음 (주요 모니터링 인터페이스)
- **보안 권장사항**: 
  - 가능하면 SSH 터널링 사용
  - 또는 특정 IP만 허용 (0.0.0.0/0 대신)

**AWS 보안 그룹 설정:**
```
유형: 사용자 지정 TCP
포트 범위: 3000
소스: 0.0.0.0/0 (또는 특정 IP)
설명: Grafana Dashboard
```

### 선택적 포트 (디버깅/관리용)

#### 2. Prometheus (포트 9090) - 선택적
- **용도**: Prometheus UI 접근, 쿼리 테스트, 타겟 상태 확인
- **URL**: http://3.21.177.186:9090
- **접근 필요성**: 중간 (디버깅 시 유용)
- **보안 권장사항**: 
  - 개발/디버깅 시에만 열기
  - 가능하면 SSH 터널링 사용
  - 또는 특정 IP만 허용

**AWS 보안 그룹 설정:**
```
유형: 사용자 지정 TCP
포트 범위: 9090
소스: 0.0.0.0/0 (또는 특정 IP)
설명: Prometheus UI
```

#### 3. AlertManager (포트 9093) - 선택적
- **용도**: 알림 상태 확인, 알림 그룹 관리
- **URL**: http://3.21.177.186:9093
- **접근 필요성**: 낮음 (Grafana에서도 확인 가능)
- **보안 권장사항**: 
  - 필요 시에만 열기
  - 가능하면 SSH 터널링 사용

**AWS 보안 그룹 설정:**
```
유형: 사용자 지정 TCP
포트 범위: 9093
소스: 0.0.0.0/0 (또는 특정 IP)
설명: AlertManager UI
```

## 내부 전용 포트 (외부 노출 불필요)

다음 포트들은 Docker 네트워크 내부에서만 접근하면 되므로 **외부 노출 불필요**:

- **PostgreSQL Exporter (9187)**: Prometheus가 내부에서 수집
- **Redis Exporter (9121)**: Prometheus가 내부에서 수집

## 보안 권장사항

### 1. SSH 터널링 사용 (가장 안전) ⭐ 권장

모든 포트를 SSH 터널링으로 접근:

```powershell
# Grafana 접근
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" -L 3000:localhost:3000 ec2-user@3.21.177.186

# Prometheus 접근
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" -L 9090:localhost:9090 ec2-user@3.21.177.186

# AlertManager 접근
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" -L 9093:localhost:9093 ec2-user@3.21.177.186

# 여러 포트 동시 터널링
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" -L 3000:localhost:3000 -L 9090:localhost:9090 -L 9093:localhost:9093 ec2-user@3.21.177.186
```

터널링 후 로컬에서 접근:
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- AlertManager: http://localhost:9093

### 2. 특정 IP만 허용

보안 그룹에서 소스를 특정 IP로 제한:

```
소스: <YOUR_IP>/32
```

### 3. Nginx 리버스 프록시 + HTTPS

- 포트 443만 외부에 노출
- Nginx에서 인증 추가
- HTTPS 적용

## 포트별 접근 우선순위

| 포트 | 서비스 | 우선순위 | 외부 노출 | 권장 방법 |
|------|--------|---------|----------|----------|
| 3000 | Grafana | ⭐⭐⭐ 필수 | 선택 | SSH 터널링 또는 특정 IP |
| 9090 | Prometheus | ⭐⭐ 선택 | 선택 | SSH 터널링 (디버깅 시) |
| 9093 | AlertManager | ⭐ 선택 | 불필요 | SSH 터널링 (필요 시) |
| 9187 | PostgreSQL Exporter | - | ❌ 불필요 | 내부 전용 |
| 9121 | Redis Exporter | - | ❌ 불필요 | 내부 전용 |

## AWS 보안 그룹 설정 예시

### 최소 설정 (Grafana만)
```
인바운드 규칙:
- 포트 3000 (TCP) - 소스: 특정 IP 또는 0.0.0.0/0
```

### 표준 설정 (Grafana + Prometheus)
```
인바운드 규칙:
- 포트 3000 (TCP) - 소스: 특정 IP 또는 0.0.0.0/0
- 포트 9090 (TCP) - 소스: 특정 IP 또는 0.0.0.0/0
```

### 전체 설정 (모든 UI 접근)
```
인바운드 규칙:
- 포트 3000 (TCP) - 소스: 특정 IP 또는 0.0.0.0/0
- 포트 9090 (TCP) - 소스: 특정 IP 또는 0.0.0.0/0
- 포트 9093 (TCP) - 소스: 특정 IP 또는 0.0.0.0/0
```

## 현재 상태 확인

### 서버에서 포트 확인
```bash
# 리스닝 중인 포트 확인
ss -tlnp | grep -E '3000|9090|9093'

# 컨테이너 포트 매핑 확인
docker ps --format 'table {{.Names}}\t{{.Ports}}' | grep -E 'prometheus|grafana|alertmanager'
```

### 외부에서 접근 테스트
```bash
# Grafana 접근 테스트
curl -I http://3.21.177.186:3000

# Prometheus 접근 테스트
curl -I http://3.21.177.186:9090

# AlertManager 접근 테스트
curl -I http://3.21.177.186:9093
```

## 보안 체크리스트

- [ ] Grafana 포트 3000 열기 (필수)
- [ ] Prometheus 포트 9090 열기 (선택)
- [ ] AlertManager 포트 9093 열기 (선택)
- [ ] 가능하면 SSH 터널링 사용
- [ ] 특정 IP만 허용하도록 설정
- [ ] Exporter 포트는 외부 노출하지 않음
- [ ] 정기적으로 보안 그룹 규칙 검토

## 참고

- **SSH 포트 (22)**: 이미 열려있어야 함 (서버 접근용)
- **애플리케이션 포트**: 8080-8084 등은 별도로 관리
- **데이터베이스 포트**: 5432 (PostgreSQL)는 외부 노출 금지

