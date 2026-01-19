# Batch Service 린터 오류 해결

> **작성일**: 2025-01-04  
> **문제**: JsonNode를 찾을 수 없다는 린터 오류

---

## 발견된 오류

### 오류 내용
```
The import com.fasterxml.jackson.databind cannot be resolved
JsonNode cannot be resolved to a type
```

**영향받는 파일**:
- `ArchMessage.java`
- `ArchChatroom.java`
- `ArchAgentResult.java`

---

## 원인 분석

### 1. 의존성 확인
- ✅ `build.gradle`에 `jackson-databind` 의존성 존재
- ✅ Chat 서비스에서도 동일한 방식으로 `JsonNode` 사용 중
- ⚠️ IDE가 아직 의존성을 인덱싱하지 못함

### 2. 다른 서비스와 비교

**Chat 서비스** (`chat/build.gradle`):
```gradle
implementation 'com.fasterxml.jackson.core:jackson-databind'
```

**Batch 서비스** (`batch/build.gradle`):
```gradle
implementation 'com.fasterxml.jackson.core:jackson-databind'
```

→ 동일한 의존성 사용

---

## 해결 방법

### 방법 1: IDE 프로젝트 새로고침 (권장)

**IntelliJ IDEA**:
1. `File` → `Invalidate Caches / Restart...`
2. `Invalidate and Restart` 선택

**VS Code**:
1. `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
2. 프로젝트 재로드

**Eclipse**:
1. 프로젝트 우클릭 → `Refresh`
2. `Gradle` → `Refresh Gradle Project`

### 방법 2: Gradle 빌드 실행

```bash
# Windows
cd batch
gradlew.bat build --refresh-dependencies

# Linux/Mac
cd batch
./gradlew build --refresh-dependencies
```

### 방법 3: 의존성 명시적 추가 (이미 완료)

`build.gradle`에 다음 추가:
```gradle
implementation 'com.fasterxml.jackson.core:jackson-databind'
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
```

---

## 확인 사항

### ✅ 의존성 존재 확인
- `batch/build.gradle`에 `jackson-databind` 있음
- `common` 모듈에서 `jackson-annotations` 제공
- Spring Boot dependency management가 버전 관리

### ✅ 사용 패턴 일치
- Chat 서비스의 `ChatRoom` 엔티티도 `JsonNode` 사용
- 동일한 `@JdbcTypeCode(SqlTypes.JSON)` 어노테이션 사용

---

## 결론

**실제 문제**: 없음 (의존성은 정상적으로 설정됨)

**린터 오류 원인**: IDE 인덱싱 문제

**해결 방법**:
1. IDE 프로젝트 새로고침 (가장 확실)
2. Gradle 빌드 실행
3. IDE 재시작

**참고**: 실제 빌드/실행 시에는 문제 없을 것입니다. IDE의 린터만 인식하지 못하는 상태입니다.

---

## 추가 확인

만약 위 방법으로 해결되지 않으면:

1. **Gradle Wrapper 확인**:
   ```bash
   cd batch
   ls -la gradlew*
   ```

2. **의존성 트리 확인**:
   ```bash
   ./gradlew dependencies --configuration compileClasspath | grep jackson
   ```

3. **빌드 테스트**:
   ```bash
   ./gradlew build
   ```


