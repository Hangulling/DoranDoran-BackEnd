# 베이스 이미지 (경량 Java 21 JRE)
FROM eclipse-temurin:21-jre-alpine

# 타임존 설정
ENV TZ=Asia/Seoul

# 작업 디렉토리
WORKDIR /app

# 빌드된 jar 파일 복사 (fat jar만 복사)
COPY chat/build/libs/chat-0.1.0-SNAPSHOT.jar app.jar

# (선택) 모듈별 포트 문서용
EXPOSE 8083

# 실행 명령
ENTRYPOINT ["java","-jar","/app/app.jar"]