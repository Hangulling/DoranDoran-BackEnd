# syntax=docker/dockerfile:1.6   # BuildKit 기능 사용(캐시 마운트 등)

### 1) 빌더 스테이지: Gradle + JDK
FROM gradle:8.5-jdk21 AS build
WORKDIR /workspace

# 의존성 캐시를 최대 활용하기 위해 설정 파일을 먼저 복사
COPY settings.gradle .
COPY build.gradle .

# 멀티모듈 소스 복사
COPY common ./common
COPY shared ./shared
COPY store  ./store

# Gradle 캐시 마운트로 의존성 재사용 → 빌드 속도 개선
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle :store:bootJar --no-daemon

### 2) 런타임 스테이지: 경량 JRE
FROM eclipse-temurin:21-jre-alpine
ENV TZ=Asia/Seoul \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
WORKDIR /app

# 빌드 산출물 복사
COPY --from=build /workspace/store/build/libs/*.jar ./app.jar

# 서비스 포트
EXPOSE 8084

# 헬스체크: Spring Actuator health
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8084/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]