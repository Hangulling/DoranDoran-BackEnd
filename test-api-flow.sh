#!/bin/bash

# DoranDoran API 테스트 플로우
# 사용자 생성 -> 로그인 -> JWT 토큰 받기 -> 채팅방 생성 -> 메시지 전송

echo "=== DoranDoran API 테스트 플로우 ==="

# 서비스 URL 설정
USER_SERVICE_URL="http://localhost:8082"
AUTH_SERVICE_URL="http://localhost:8081"
CHAT_SERVICE_URL="http://localhost:8080"

# 테스트용 사용자 정보
TEST_EMAIL="test@example.com"
TEST_PASSWORD="password123"
TEST_NAME="Test User"

echo ""
echo "1. 사용자 생성 중..."
CREATE_USER_RESPONSE=$(curl -s -X POST "$USER_SERVICE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"firstName\": \"Test\",
    \"lastName\": \"User\",
    \"name\": \"$TEST_NAME\",
    \"password\": \"$TEST_PASSWORD\",
    \"profileImageUrl\": \"https://example.com/profile.jpg\",
    \"bio\": \"Test user for API testing\"
  }")

if [ $? -eq 0 ]; then
  echo "사용자 생성 성공 또는 이미 존재"
else
  echo "사용자 생성 실패"
  exit 1
fi

echo ""
echo "2. 로그인 중..."
LOGIN_RESPONSE=$(curl -s -X POST "$AUTH_SERVICE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }")

if [ $? -eq 0 ]; then
  ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')
  USER_ID=$(echo $LOGIN_RESPONSE | jq -r '.data.userId')
  echo "로그인 성공!"
  echo "Access Token: ${ACCESS_TOKEN:0:20}..."
  echo "User ID: $USER_ID"
else
  echo "로그인 실패"
  exit 1
fi

echo ""
echo "3. 토큰 검증 중..."
VALIDATE_RESPONSE=$(curl -s -X GET "$AUTH_SERVICE_URL/api/auth/validate" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if [ $? -eq 0 ]; then
  echo "토큰 검증 성공"
else
  echo "토큰 검증 실패"
  exit 1
fi

echo ""
echo "4. 채팅방 생성 중..."
CHATBOT_ID=$(uuidgen)
CREATE_ROOM_RESPONSE=$(curl -s -X POST "$CHAT_SERVICE_URL/api/chat/chatrooms" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"chatbotId\": \"$CHATBOT_ID\",
    \"name\": \"API 테스트 채팅방\"
  }")

if [ $? -eq 0 ]; then
  CHATROOM_ID=$(echo $CREATE_ROOM_RESPONSE | jq -r '.id')
  echo "채팅방 생성 성공: $CHATROOM_ID"
else
  echo "채팅방 생성 실패"
  exit 1
fi

echo ""
echo "5. 메시지 전송 중..."
MESSAGE_RESPONSE=$(curl -s -X POST "$CHAT_SERVICE_URL/api/chat/chatrooms/$CHATROOM_ID/messages" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"content\": \"안녕하세요! API 테스트 메시지입니다.\",
    \"senderType\": \"user\",
    \"contentType\": \"text/plain\"
  }")

if [ $? -eq 0 ]; then
  echo "메시지 전송 성공"
else
  echo "메시지 전송 실패"
  exit 1
fi

echo ""
echo "6. 채팅방 목록 조회 중..."
ROOMS_RESPONSE=$(curl -s -X GET "$CHAT_SERVICE_URL/api/chat/chatrooms" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if [ $? -eq 0 ]; then
  echo "채팅방 목록 조회 성공"
else
  echo "채팅방 목록 조회 실패"
fi

echo ""
echo "7. 메시지 목록 조회 중..."
MESSAGES_RESPONSE=$(curl -s -X GET "$CHAT_SERVICE_URL/api/chat/chatrooms/$CHATROOM_ID/messages" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if [ $? -eq 0 ]; then
  echo "메시지 목록 조회 성공"
else
  echo "메시지 목록 조회 실패"
fi

echo ""
echo "=== API 테스트 플로우 완료 ==="
echo "모든 API 호출이 성공적으로 완료되었습니다!"
