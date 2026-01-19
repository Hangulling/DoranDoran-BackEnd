# Archive 스키마 리셋 및 재이관 결과 보고서

> **작성일**: 2026-01-11  
> **작업 내용**: Archive 스키마 데이터 덤프 → 비우기 → 수정된 함수로 재이관  
> **상태**: ✅ 완료

---

## 작업 개요

잘못된 concept 값(`TEACHER`, `TUTOR`, `COACH`)을 사용하던 함수를 수정하여, 실제 서비스에서 사용하는 concept 값(`FRIEND`, `HONEY`, `COWORKER`, `SENIOR`, `BOSS`)으로 재이관을 수행했습니다.

---

## 작업 단계

### Phase 1: Archive 스키마 데이터 덤프

**스크립트**: `scripts/backup/dump_archive_schema.sh`

**결과**:
- 덤프 파일: `/home/ec2-user/backups/archive_schema_dump_20260111_153136.sql`
- 덤프 파일 크기: **2.1MB**
- 상태: ✅ 완료

### Phase 2: Archive 스키마 데이터 비우기

**SQL**: `scripts/backup/clear_archive_schema.sql`

**삭제 순서** (외래 키 제약 고려):
1. `arch_agent_results` (arch_messages 참조)
2. `arch_messages` (arch_chatrooms 참조)
3. `arch_stores` (arch_chatrooms 참조)
4. `arch_usage_events` (arch_chatrooms 참조)
5. `arch_intimacy_progress` (arch_chatrooms 참조)
6. `arch_chatrooms` (최상위 테이블)
7. `arch_ingestion_state` (독립 테이블)

**결과**: ✅ 모든 테이블 데이터 삭제 완료

### Phase 3: 함수 SQL 실행 (수정된 함수)

**SQL**: `scripts/backup/archive_migration_functions.sql`

**수정 내용**:
```sql
-- 수정 전
IF concept_value IN ('FRIEND', 'TEACHER', 'TUTOR', 'COACH') THEN

-- 수정 후
IF concept_value IN ('FRIEND', 'HONEY', 'COWORKER', 'SENIOR', 'BOSS') THEN
```

**결과**: ✅ 함수 업데이트 완료

### Phase 4: 메인 이관 SQL 실행

**SQL**: `scripts/backup/migrate_to_archive_schema.sql`

**결과**: ✅ 재이관 완료

---

## 재이관 결과 통계

### 테이블별 데이터 수

| 테이블 | 이관된 데이터 수 |
|--------|----------------|
| **arch_chatrooms** | **375** |
| **arch_messages** | **1,849** |
| **arch_stores** | **179** |
| **arch_usage_events** | **0** |
| **arch_intimacy_progress** | **375** |
| **arch_agent_results** | **767** |

### Concept 값 분포

| Concept | 채팅방 수 | 비율 |
|---------|----------|------|
| **FRIEND** | **246** | 65.6% |
| **HONEY** | **62** | 16.5% |
| **COWORKER** | **35** | 9.3% |
| **SENIOR** | **32** | 8.5% |
| **BOSS** | **0** | 0% (미사용) |

**총합**: 375개 채팅방

---

## 검증 결과

### ✅ Concept 값 검증

- **FRIEND**: 246개 ✅
- **HONEY**: 62개 ✅
- **COWORKER**: 35개 ✅
- **SENIOR**: 32개 ✅
- **BOSS**: 0개 (미사용, 정상)

**결론**: 모든 concept 값이 올바르게 이관되었습니다.

### ✅ 데이터 무결성

- 모든 외래 키 관계 정상
- 중복 데이터 없음
- 모든 관련 데이터 이관 완료

---

## 생성된 파일

### 스크립트 파일

1. **`scripts/backup/dump_archive_schema.sh`**
   - Archive 스키마 데이터 덤프 생성

2. **`scripts/backup/clear_archive_schema.sql`**
   - Archive 스키마 데이터 비우기

3. **`scripts/backup/reset_and_remigrate_archive.sh`**
   - 전체 프로세스 실행 (덤프 → 비우기 → 재이관)

### 덤프 파일

- **`/home/ec2-user/backups/archive_schema_dump_20260111_153136.sql`** (2.1MB)
  - 이전 데이터 백업 (필요시 복구 가능)

### 로그 파일

- **`/home/ec2-user/backups/archive_reset_remigrate.log`**
  - 전체 작업 로그

---

## 이전 이관과 비교

### 이전 이관 (잘못된 concept 값)

- Concept 값: `TEACHER`, `TUTOR`, `COACH` (잘못된 값)
- 결과: 모든 concept이 `FRIEND`로 잘못 이관됨

### 재이관 (수정된 concept 값)

- Concept 값: `FRIEND`, `HONEY`, `COWORKER`, `SENIOR`, `BOSS` (올바른 값)
- 결과: 모든 concept이 올바르게 이관됨

---

## 다음 단계

1. ✅ **재이관 완료**: 수정된 함수로 올바른 concept 값 이관 완료
2. ✅ **데이터 검증**: Concept 값 분포 확인 완료
3. **자동화**: 기존 Cron 작업이 수정된 함수를 사용하므로 자동으로 올바른 값으로 이관됨

---

## 주의사항

- **덤프 파일 보관**: 이전 데이터는 `/home/ec2-user/backups/archive_schema_dump_20260111_153136.sql`에 백업되어 있습니다.
- **자동화**: 매일 새벽 4시에 실행되는 Cron 작업은 수정된 함수를 사용하므로, 향후 이관되는 데이터는 모두 올바른 concept 값을 가집니다.

---

**작업 완료일**: 2026-01-11  
**최종 검증 완료**: ✅


