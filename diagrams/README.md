# DoranDoran 아키텍처 다이어그램

이 폴더에는 DoranDoran 프로젝트의 모든 아키텍처 다이어그램이 고해상도 SVG 형식으로 저장되어 있습니다.

## 📊 다이어그램 목록

### 1. 핵심 아키텍처 다이어그램

#### 1.1 ERD (Entity Relationship Diagram)
- **파일**: `erd_diagram.svg`
- **설명**: 데이터베이스 스키마 구조 및 테이블 간 관계
- **해상도**: 2000x1500 (scale 2x)
- **특징**: 4개 스키마 분리, JSONB 활용, 친밀도 학습 진척도 추적

#### 1.2 MSA 아키텍처
- **파일**: `msa_diagram.svg`
- **설명**: 마이크로서비스 아키텍처 전체 구조
- **해상도**: 2000x1200 (scale 2x)
- **특징**: 5개 마이크로서비스, API Gateway, 외부 서비스 통합

#### 1.3 챗봇 아키텍처
- **파일**: `chatbot_diagram.svg`
- **설명**: Multi-Agent AI 시스템 구조
- **해상도**: 2000x1400 (scale 2x)
- **특징**: 4개 AI Agent, 실시간 통신, OpenAI 통합

### 2. 상세 흐름 다이어그램

#### 2.1 Multi-Agent 처리 흐름
- **파일**: `multi_agent_flow.svg`
- **설명**: AI Agent들의 병렬/순차 처리 시퀀스
- **해상도**: 2000x1000 (scale 2x)
- **특징**: 병렬 처리, SSE 스트리밍, 이벤트 기반 통신

#### 2.2 서비스 간 통신 패턴
- **파일**: `service_communication.svg`
- **설명**: 마이크로서비스 간 HTTP 통신 시퀀스
- **해상도**: 2000x800 (scale 2x)
- **특징**: JWT 인증, Feign Client, Circuit Breaker

### 3. 인프라 및 배포 다이어그램

#### 3.1 Docker 컨테이너 아키텍처
- **파일**: `docker_architecture.svg`
- **설명**: Docker Compose 기반 컨테이너 구성
- **해상도**: 2000x1200 (scale 2x)
- **특징**: 데이터베이스, 애플리케이션, 모니터링 컨테이너

#### 3.2 보안 아키텍처
- **파일**: `security_architecture.svg`
- **설명**: 다층 보안 구조 및 위협 대응
- **해상도**: 2000x1000 (scale 2x)
- **특징**: 네트워크, 애플리케이션, 데이터, 인프라 보안

#### 3.3 모니터링 스택
- **파일**: `monitoring_stack.svg`
- **설명**: 메트릭 수집, 로깅, 시각화, 알림 시스템
- **해상도**: 2000x1200 (scale 2x)
- **특징**: Prometheus, Grafana, ELK Stack, Alert Manager

#### 3.4 CI/CD 파이프라인
- **파일**: `cicd_pipeline.svg`
- **설명**: 코드 커밋부터 프로덕션 배포까지의 자동화 흐름
- **해상도**: 2000x400 (scale 2x)
- **특징**: GitHub Actions, Docker, 자동 테스트, 배포

## 🎨 다이어그램 특징

### 고해상도 렌더링
- **해상도**: 최소 2000px 너비
- **스케일**: 2x 배율로 선명도 향상
- **형식**: SVG 벡터 형식으로 확대/축소 시 품질 유지

### 색상 및 스타일
- **일관된 색상 팔레트**: 서비스별 구분
- **명확한 레이블**: 각 컴포넌트의 역할 명시
- **계층적 구조**: 시각적 계층으로 아키텍처 이해 용이

### 사용 목적별 분류
- **설계 문서**: ERD, MSA, 챗봇 아키텍처
- **개발 가이드**: 서비스 통신, Multi-Agent 흐름
- **운영 가이드**: Docker, 보안, 모니터링, CI/CD

## 📁 파일 구조

```
diagrams/
├── README.md                    # 이 파일
├── erd_diagram.svg             # ERD 다이어그램
├── msa_diagram.svg             # MSA 아키텍처
├── chatbot_diagram.svg         # 챗봇 아키텍처
├── multi_agent_flow.svg        # Multi-Agent 흐름
├── service_communication.svg   # 서비스 통신
├── docker_architecture.svg     # Docker 아키텍처
├── security_architecture.svg   # 보안 아키텍처
├── monitoring_stack.svg        # 모니터링 스택
├── cicd_pipeline.svg           # CI/CD 파이프라인
├── erd_diagram.mmd             # ERD Mermaid 소스
├── msa_diagram.mmd             # MSA Mermaid 소스
├── chatbot_diagram.mmd         # 챗봇 Mermaid 소스
├── multi_agent_flow.mmd        # Multi-Agent Mermaid 소스
├── service_communication.mmd   # 서비스 통신 Mermaid 소스
├── docker_architecture.mmd     # Docker Mermaid 소스
├── security_architecture.mmd   # 보안 Mermaid 소스
├── monitoring_stack.mmd        # 모니터링 Mermaid 소스
└── cicd_pipeline.mmd           # CI/CD Mermaid 소스
```

## 🔧 사용 방법

### SVG 파일 사용
1. **웹 브라우저**: 직접 열어서 확인
2. **문서 삽입**: Word, PowerPoint, Confluence 등에 삽입
3. **인쇄**: 고해상도로 인쇄 가능
4. **편집**: Inkscape, Adobe Illustrator 등으로 편집 가능

### Mermaid 소스 수정
1. `.mmd` 파일을 텍스트 에디터로 열기
2. Mermaid 문법에 따라 수정
3. `mmdc` 명령어로 다시 SVG 생성:
   ```bash
   mmdc -i [파일명].mmd -o [파일명].svg -w 2000 -H [높이] --scale 2
   ```

## 📝 업데이트 가이드

### 새로운 다이어그램 추가
1. `.mmd` 파일로 Mermaid 소스 작성
2. `mmdc` 명령어로 SVG 생성
3. 이 README.md에 다이어그램 정보 추가

### 기존 다이어그램 수정
1. 해당 `.mmd` 파일 수정
2. `mmdc` 명령어로 SVG 재생성
3. 필요시 README.md 정보 업데이트

## 🎯 활용 사례

- **기술 문서**: 아키텍처 설계서, API 문서
- **프레젠테이션**: 기술 발표, 팀 미팅
- **개발 가이드**: 온보딩 문서, 개발 가이드
- **운영 매뉴얼**: 배포 가이드, 모니터링 가이드
- **보고서**: 프로젝트 진행 보고서, 시스템 현황 보고서
