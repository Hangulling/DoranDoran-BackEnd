# Archive 스키마 FK 제약조건 분석

> **분석일**: 2025-01-04  
> **제안**: FK 제약 제거, UUID만 저장, 인덱스 유지  
> **목적**: Archive 독립성 강화 및 관리 편의성

---

## 📊 현재 상황

### 현재 FK 제약조건

```sql
-- arch_chatrooms
ALTER TABLE archive_schema.arch_chatrooms
  ADD CONSTRAINT fk_arch_chatrooms_user
  FOREIGN KEY (user_id) REFERENCES user_schema.app_user(id) ON DELETE SET NULL;

ALTER TABLE archive_schema.arch_chatrooms
  ADD CONSTRAINT fk_arch_chatrooms_chatbot
  FOREIGN KEY (chatbot_id) REFERENCES chat_schema.chatbots(id) ON DELETE SET NULL;
```

### 현재 구조
- ✅ **스냅샷 컬럼 존재**: `user_email_snapshot`, `chatbot_name_snapshot` 등
- ✅ **ON DELETE SET NULL**: 원본 삭제 시 NULL로 변경 (archive 유지)
- ⚠️ **FK 제약 존재**: 운영 스키마와 연결

---

## ✅ 제안의 장점

### 1. 완전한 독립성
- **운영 스키마 변경/삭제 시 영향 없음**
  - 운영 DB 마이그레이션 시 FK 제약으로 인한 오류 방지
  - 운영 스키마 재구성 시에도 archive 안정적 유지
- **별도 DB로 분리 가능**
  - Archive를 별도 DB로 이전 시 FK 제약 없이 가능
  - 감사 목적에 적합한 완전한 독립 저장소

### 2. 관리 편의성
- **운영 스키마 변경 시 FK 제약 수정 불필요**
  - 운영 스키마 컬럼 변경, 테이블 재구성 시 FK 제약으로 인한 제약 없음
- **Archive 전용 스키마 관리**
  - Archive만의 독립적인 스키마 관리 가능

### 3. 감사 목적 적합성
- **과거 데이터 보존**
  - 원본이 삭제되어도 archive 데이터 유지
  - 스냅샷 컬럼으로 이미 필요한 정보 보존
- **법적/규제 요구사항 대응**
  - 완전한 독립 저장소로 감사 목적에 적합

### 4. 성능
- **인덱스 유지로 검색 성능 보장**
  - FK 제약 제거해도 인덱스는 유지 가능
  - 조회 성능 영향 없음

---

## ⚠️ 제안의 단점

### 1. 데이터 무결성 검증 불가
- **잘못된 UUID 삽입 가능**
  - FK 제약이 없으면 존재하지 않는 UUID 삽입 가능
  - 데이터 검증은 애플리케이션 레벨에서만 가능

### 2. 참조 무결성 보장 불가
- **DB 레벨 검증 불가**
  - FK 제약이 없으면 DB 레벨에서 참조 무결성 보장 불가
  - 애플리케이션 로직에 의존

### 3. 조인 불가 (하지만 스냅샷으로 보완)
- **원본 테이블과 JOIN 불가**
  - FK가 없으면 JOIN으로 원본 데이터 조회 불가
  - **하지만**: 스냅샷 컬럼이 이미 있어서 실질적 문제 없음

---

## 🔄 반박 가능한 부분

### 1. ON DELETE SET NULL이 이미 독립성 보장?

**반박**:
- ✅ ON DELETE SET NULL은 원본 삭제 시 NULL로 변경하므로 archive 유지
- ❌ 하지만 FK 제약 자체가 운영 스키마와의 연결을 의미
- ❌ 운영 스키마 변경/마이그레이션 시 FK 제약으로 인한 제약 발생 가능

**결론**: ON DELETE SET NULL은 부분적 독립성만 보장, 완전한 독립성은 아님

### 2. 스냅샷 컬럼이 이미 있어서 FK 불필요?

**반박**:
- ✅ 스냅샷 컬럼으로 필요한 정보는 이미 보존
- ✅ FK 없어도 archive 목적 달성 가능
- ❌ 하지만 FK는 데이터 무결성 검증에 유용

**결론**: Archive 목적(관리/감사)에는 스냅샷으로 충분, FK는 선택적

### 3. 데이터 무결성 검증 필요?

**반박**:
- ✅ Archive는 과거 데이터 보존이 목적
- ✅ Archive 시점에 이미 검증된 데이터만 저장
- ❌ Archive 생성 후 잘못된 데이터 삽입 가능성은 낮음

**결론**: Archive 특성상 데이터 무결성 검증은 애플리케이션 레벨에서 충분

---

## 💡 최종 권장안

### ✅ **FK 제약 제거 권장**

**이유**:
1. **Archive의 목적**: 관리/감사용 독립 저장소
2. **스냅샷 컬럼 존재**: 필요한 정보는 이미 보존
3. **독립성 강화**: 운영 스키마와 완전 분리
4. **관리 편의성**: 운영 스키마 변경 시 영향 없음

### 📋 구현 방안

```sql
-- FK 제약 제거
ALTER TABLE archive_schema.arch_chatrooms
  DROP CONSTRAINT IF EXISTS fk_arch_chatrooms_user;

ALTER TABLE archive_schema.arch_chatrooms
  DROP CONSTRAINT IF EXISTS fk_arch_chatrooms_chatbot;

-- 인덱스는 유지 (검색 성능)
CREATE INDEX IF NOT EXISTS idx_arch_chatrooms_user_created
  ON archive_schema.arch_chatrooms(user_id, source_created_at DESC);

CREATE INDEX IF NOT EXISTS idx_arch_chatrooms_chatbot_created
  ON archive_schema.arch_chatrooms(chatbot_id, source_created_at DESC);
```

### ⚠️ 주의사항

1. **애플리케이션 레벨 검증**
   - Archive 생성 시 UUID 유효성 검증 필요
   - 잘못된 UUID 삽입 방지

2. **스냅샷 컬럼 활용**
   - FK 없어도 스냅샷 컬럼으로 필요한 정보 보존
   - 조회 시 스냅샷 컬럼 활용

3. **문서화**
   - FK 제약이 없음을 명시
   - UUID는 참조용이며 검증되지 않음을 문서화

---

## 📊 비교표

| 항목 | FK 제약 유지 | FK 제약 제거 |
|------|-------------|-------------|
| **독립성** | ⚠️ 부분적 (ON DELETE SET NULL) | ✅ 완전한 독립성 |
| **관리 편의성** | ⚠️ 운영 스키마 변경 시 제약 | ✅ 운영 스키마 변경 영향 없음 |
| **데이터 무결성** | ✅ DB 레벨 검증 | ⚠️ 애플리케이션 레벨 검증 |
| **감사 목적** | ⚠️ 운영 스키마와 연결 | ✅ 완전한 독립 저장소 |
| **성능** | ✅ 인덱스 + FK | ✅ 인덱스만으로도 충분 |
| **별도 DB 분리** | ❌ FK 제약으로 제한 | ✅ 자유롭게 분리 가능 |

---

## 🎯 결론

**Archive의 목적(관리/감사)을 고려할 때 FK 제약 제거를 권장합니다.**

- ✅ 완전한 독립성 확보
- ✅ 관리 편의성 향상
- ✅ 감사 목적에 적합
- ✅ 스냅샷 컬럼으로 정보 보존 가능
- ⚠️ 데이터 무결성은 애플리케이션 레벨에서 검증

**다만**, 데이터 무결성 검증을 강화하기 위해 애플리케이션 레벨에서 UUID 유효성 검증 로직을 추가하는 것을 권장합니다.


