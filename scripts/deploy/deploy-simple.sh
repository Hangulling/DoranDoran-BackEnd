#!/bin/bash

echo "=== DoranDoran Simple Deployment ==="

# 기존 컨테이너 정리
echo "Cleaning up existing containers..."
docker stop $(docker ps -aq --filter "name=dorandoran") 2>/dev/null || true
docker rm $(docker ps -aq --filter "name=dorandoran") 2>/dev/null || true

# Docker 이미지 로드
echo "Loading Docker images..."
docker load -i dorandoran-gateway.tar
docker load -i dorandoran-user.tar
docker load -i dorandoran-chat.tar
docker load -i dorandoran-auth.tar
docker load -i dorandoran-batch.tar

# Redis 시작
echo "Starting Redis..."
docker run -d --name dorandoran-redis -p 6379:6379 redis:7-alpine

# RDS 정보
RDS_HOST="dorandoran-postgres.cpw00a6ga2uv.us-east-2.rds.amazonaws.com"
RDS_DB="dorandoran"
RDS_USER="doran"
RDS_PASS="doran"

# 서비스들 시작
echo "Starting services..."

# Auth Service
docker run -d --name dorandoran-auth -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://$RDS_HOST:5432/$RDS_DB" \
  -e SPRING_DATASOURCE_USERNAME=$RDS_USER \
  -e SPRING_DATASOURCE_PASSWORD=$RDS_PASS \
  -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=auth_schema \
  -e SPRING_REDIS_HOST=localhost \
  -e SPRING_REDIS_PORT=6379 \
  dorandoran-auth:latest

# User Service
docker run -d --name dorandoran-user -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://$RDS_HOST:5432/$RDS_DB" \
  -e SPRING_DATASOURCE_USERNAME=$RDS_USER \
  -e SPRING_DATASOURCE_PASSWORD=$RDS_PASS \
  -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=user_schema \
  -e SPRING_REDIS_HOST=localhost \
  -e SPRING_REDIS_PORT=6379 \
  dorandoran-user:latest

# Chat Service
docker run -d --name dorandoran-chat -p 8083:8083 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://$RDS_HOST:5432/$RDS_DB" \
  -e SPRING_DATASOURCE_USERNAME=$RDS_USER \
  -e SPRING_DATASOURCE_PASSWORD=$RDS_PASS \
  -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=chat_schema \
  -e SPRING_REDIS_HOST=localhost \
  -e SPRING_REDIS_PORT=6379 \
  -e OPENAI_API_KEY=$OPENAI_API_KEY \
  dorandoran-chat:latest

# Batch Service
docker run -d --name dorandoran-batch -p 8085:8085 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://$RDS_HOST:5432/$RDS_DB" \
  -e SPRING_DATASOURCE_USERNAME=$RDS_USER \
  -e SPRING_DATASOURCE_PASSWORD=$RDS_PASS \
  -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=batch_schema \
  dorandoran-batch:latest

# Gateway Service
docker run -d --name dorandoran-gateway -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_REDIS_HOST=localhost \
  -e SPRING_REDIS_PORT=6379 \
  dorandoran-gateway:latest

echo "=== Deployment Complete ==="
echo "Services running on:"
echo "- Gateway: http://localhost:8080"
echo "- Auth: http://localhost:8081"
echo "- User: http://localhost:8082"
echo "- Chat: http://localhost:8083"
echo "- Batch: http://localhost:8085"

docker ps --filter "name=dorandoran"
