# 베이스 이미지 (경량 Java 21 JRE)
FROM eclipse-temurin:21-jre-alpine

# 타임존 설정
ENV TZ=Asia/Seoul

# 작업 디렉토리
WORKDIR /app

# 빌드된 jar 파일 복사 (각 모듈의 build/libs/*.jar)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# (선택) 모듈별 포트 문서용
EXPOSE 8080

# 실행 명령
ENTRYPOINT ["java","-jar","/app/app.jar"]