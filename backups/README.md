# 데이터베이스 백업 파일

이 폴더에는 DoranDoran 프로젝트의 데이터베이스 백업 파일이 저장되어 있습니다.

## 백업 파일 목록

### 1. rds_backup.dump (386 KB)
- **형식**: PostgreSQL custom format
- **내용**: 스키마 + 데이터 전체
- **백업 날짜**: 2025-10-28 (RDS 마이그레이션 시점)
- **복원 명령어**:
  ```bash
  docker exec dorandoran-shared-db pg_restore -U doran -d dorandoran /path/to/rds_backup.dump
  ```

### 2. schema_dump.sql (41 KB)
- **형식**: SQL plain text
- **내용**: 스키마 정의만 (데이터 제외)
- **백업 날짜**: 2025-10-28
- **복원 명령어**:
  ```bash
  docker exec dorandoran-shared-db psql -U doran -d dorandoran -f /path/to/schema_dump.sql
  ```

## 파일 용도

### rds_backup.dump
- ✅ 완전한 데이터베이스 복원에 사용
- 스키마와 모든 데이터 포함
- 문제 발생 시 전체 DB 재생성 가능

### schema_dump.sql
- ✅ 스키마만 필요한 경우 사용
- 테이블 구조만 복원
- 데이터는 빈 상태로 시작

## 복원 시나리오

### 시나리오 1: 전체 복원 (데이터 포함)
```bash
# 1. 컨테이너 재생성 (필요시)
docker rm -f dorandoran-shared-db
docker run -d --name dorandoran-shared-db \
  --network dorandoran-network \
  -e POSTGRES_DB=dorandoran \
  -e POSTGRES_USER=doran \
  -e POSTGRES_PASSWORD=doran \
  -v dorandoran_db_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:17-alpine

# 2. 백업 복원
docker cp backups/rds_backup.dump dorandoran-shared-db:/tmp/
docker exec dorandoran-shared-db pg_restore -U doran -d dorandoran /tmp/rds_backup.dump
```

### 시나리오 2: 스키마만 복원
```bash
# 1. 빈 데이터베이스 준비
docker exec dorandoran-shared-db psql -U doran -d dorandoran -c "DROP SCHEMA IF EXISTS chat_schema CASCADE;"
docker exec dorandoran-shared-db psql -U doran -d dorandoran -c "DROP SCHEMA IF EXISTS auth_schema CASCADE;"
# (필요한 스키마만 선택적으로 DROP)

# 2. 스키마 복원
docker cp backups/schema_dump.sql dorandoran-shared-db:/tmp/
docker exec dorandoran-shared-db psql -U doran -d dorandoran -f /tmp/schema_dump.sql
```

## 중요 사항

### 백업 파일 관리
1. **정기적 업데이트 권장**: 데이터가 추가될 때마다 백업 갱신
2. **안전한 위치 보관**: Git에 커밋하지 말 것 (민감 데이터 포함 가능)
3. **암호화 권장**: 민감한 사용자 데이터가 포함될 수 있음

### Git에서 제외
`.gitignore`에 이미 추가되어 있어 Git에 커밋되지 않습니다:
```
backups/
```

### 자동 백업 스크립트 (서버에서 실행)
```bash
# daily-backup.sh
#!/bin/bash
BACKUP_DIR="/home/ec2-user/backups"
DATE=$(date +%Y%m%d)

# 현재 DB 백업
docker exec dorandoran-shared-db pg_dump -U doran -d dorandoran -F c > $BACKUP_DIR/dorandoran_${DATE}.dump

# 오래된 백업 삭제 (30일 이상)
find $BACKUP_DIR -name "dorandoran_*.dump" -mtime +30 -delete

echo "Backup completed: dorandoran_${DATE}.dump"
```

## 문제 발생 시 복원 순서

1. **문제 확인**
   - 데이터베이스가 손상되었는지 확인
   - 서비스가 정상 작동하지 않는지 확인

2. **백업 선택**
   - 데이터를 보존해야 하면: `rds_backup.dump` 사용
   - 스키마만 복원하면 되면: `schema_dump.sql` 사용

3. **복원 실행**
   - 위의 복원 명령어 실행

4. **검증**
   - 서비스가 정상 작동하는지 확인
   - 데이터 무결성 확인

## 관련 문서

- `../DATABASE_RECOVERY_GUIDE.md`: 복원 가이드 상세 설명
- `../docker/scripts/init-shared-db.sql`: 초기 스키마 생성 스크립트

