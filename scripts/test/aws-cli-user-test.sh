#!/bin/bash

# User 서비스 API 테스트 - AWS CLI용
# EC2 인스턴스에서 직접 실행하거나 AWS CLI로 원격 실행

# EC2 인스턴스의 공인 IP (실제 IP로 변경 필요)
EC2_PUBLIC_IP="YOUR_EC2_PUBLIC_IP"
USER_SERVICE_PORT="8081"

echo "=== User 서비스 API 테스트 ==="

# 1. 헬스체크 테스트
echo -e "\n1. 헬스체크 테스트"
HEALTH_URL="http://${EC2_PUBLIC_IP}:${USER_SERVICE_PORT}/api/users/health"
echo "URL: $HEALTH_URL"
curl -X GET "$HEALTH_URL" || echo "헬스체크 실패"

# 2. 이메일 중복확인 테스트
echo -e "\n2. 이메일 중복확인 테스트"
TEST_EMAIL="test@example.com"
EMAIL_CHECK_URL="http://${EC2_PUBLIC_IP}:${USER_SERVICE_PORT}/api/users/check-email/${TEST_EMAIL}"
echo "URL: $EMAIL_CHECK_URL"
curl -X GET "$EMAIL_CHECK_URL" || echo "이메일 중복확인 실패"

# 3. 사용자 ID로 조회 테스트
echo -e "\n3. 사용자 ID 조회 테스트 (존재하지 않는 ID)"
TEST_USER_ID="123e4567-e89b-12d3-a456-426614174000"
USER_BY_ID_URL="http://${EC2_PUBLIC_IP}:${USER_SERVICE_PORT}/api/users/${TEST_USER_ID}"
echo "URL: $USER_BY_ID_URL"
curl -X GET "$USER_BY_ID_URL" || echo "사용자 조회 실패 (예상됨)"

# 4. 이메일로 사용자 조회 테스트
echo -e "\n4. 이메일로 사용자 조회 테스트 (존재하지 않는 이메일)"
TEST_EMAIL2="nonexistent@example.com"
USER_BY_EMAIL_URL="http://${EC2_PUBLIC_IP}:${USER_SERVICE_PORT}/api/users/email/${TEST_EMAIL2}"
echo "URL: $USER_BY_EMAIL_URL"
curl -X GET "$USER_BY_EMAIL_URL" || echo "이메일로 사용자 조회 실패 (예상됨)"

echo -e "\n=== 테스트 완료 ==="
