#!/bin/bash
docker stop dorandoran-user 2>/dev/null || true
docker rm dorandoran-user 2>/dev/null || true
docker run -d \
  --name dorandoran-user \
  --network dorandoran-network \
  -p 8082:8082 \
  --restart=unless-stopped \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://dorandoran-shared-db:5432/dorandoran' \
  -e SPRING_DATASOURCE_USERNAME='doran' \
  -e SPRING_DATASOURCE_PASSWORD='doran' \
  -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=user_schema \
  -e SPRING_REDIS_HOST='dorandoran-redis' \
  -e SPRING_REDIS_PORT='6379' \
  dorandoran-user:latest






