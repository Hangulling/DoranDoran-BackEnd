#!/bin/bash
# User 서비스 배포 (서버에서 실행 시)
# 환경변수: /home/ec2-user/dorandoran-user.env 에서 로드

set -e
ENV_FILE="${ENV_FILE:-/home/ec2-user/dorandoran-user.env}"

docker stop dorandoran-user 2>/dev/null || true
docker rm dorandoran-user 2>/dev/null || true

docker run -d \
  --name dorandoran-user \
  --network dorandoran-network \
  -p 8082:8082 \
  --restart=unless-stopped \
  --env-file "$ENV_FILE" \
  -e SPRING_PROFILES_ACTIVE=docker \
  dorandoran-user:latest

echo "User deployment done."
