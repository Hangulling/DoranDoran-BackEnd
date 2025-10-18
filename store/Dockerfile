FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY settings.gradle .
COPY build.gradle .
COPY common ./common
COPY shared ./shared
COPY store ./store
RUN gradle :store:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/store/build/libs/*.jar app.jar
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]