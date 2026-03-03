#!/bin/bash
# Auth 서비스 배포 (서버에서 실행 시)
# 환경변수: /home/ec2-user/dorandoran-auth.env 에서 로드

set -e
ENV_FILE="${ENV_FILE:-/home/ec2-user/dorandoran-auth.env}"

docker stop dorandoran-auth 2>/dev/null || true
docker rm dorandoran-auth 2>/dev/null || true

docker run -d \
  --name dorandoran-auth \
  --network dorandoran-network \
  -p 8081:8081 \
  --restart=unless-stopped \
  --env-file "$ENV_FILE" \
  -e SPRING_PROFILES_ACTIVE=docker \
  dorandoran-auth:latest

echo "Auth deployment done."
