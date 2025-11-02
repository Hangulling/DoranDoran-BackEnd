# Gmail SMTP 설정 가이드

## 문제 상황
- Gmail SMTP 인증 실패: `535-5.7.8 Username and Password not accepted`
- 현재 App Password: `dpruceprdvenamkx`

## Gmail App Password 설정 확인 방법

### 1. Gmail 2단계 인증 확인 및 활성화

**1-1. 2단계 인증 상태 확인**
1. Google 계정으로 이동: https://myaccount.google.com/
2. 왼쪽 메뉴에서 **"보안"** 클릭
3. **"Google에 로그인"** 섹션에서 **"2단계 인증"** 확인
4. 활성화되어 있지 않으면 **"시작하기"** 클릭하여 활성화

**참고**: App Password를 사용하려면 **반드시 2단계 인증이 활성화**되어 있어야 합니다.

---

### 2. App Password 생성 및 확인

**2-1. 기존 App Password 확인**
1. Google 계정 보안 페이지: https://myaccount.google.com/security
2. **"Google에 로그인"** 섹션에서 **"2단계 인증"** 클릭
3. 페이지 하단의 **"앱 비밀번호"** 클릭
   - 또는 직접 이동: https://myaccount.google.com/apppasswords
4. 현재 생성된 App Password 목록 확인

**2-2. 새 App Password 생성**
1. https://myaccount.google.com/apppasswords 접속
2. **"앱 선택"** 드롭다운에서 **"기타(사용자 지정 이름)"** 선택
3. 이름 입력: `DoranDoran SMTP` (또는 원하는 이름)
4. **"생성"** 클릭
5. 16자리 비밀번호가 생성됩니다 (예: `abcd efgh ijkl mnop`)
6. **공백을 제거하고 한 줄로 사용**: `abcdefghijklmnop`

**⚠️ 중요**: App Password는 한 번만 표시됩니다. 생성 후 즉시 복사하세요!

---

### 3. App Password 업데이트 방법

**3-1. Docker 환경 변수 업데이트**

현재 서버에 설정된 값 확인:
```bash
# SSH로 접속
ssh -i "C:\Users\KDH\Downloads\dorandoran-key.pem" ec2-user@3.21.177.186

# 환경 변수 확인
docker exec dorandoran-auth printenv | grep MAIL
```

**3-2. 새 App Password로 컨테이너 재시작**

```bash
# 1. 컨테이너 중지 및 삭제
docker stop dorandoran-auth
docker rm dorandoran-auth

# 2. 새 App Password로 컨테이너 시작
docker run -d --name dorandoran-auth \
  --network dorandoran-network \
  -p 8081:8081 \
  --restart=unless-stopped \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://dorandoran-shared-db:5432/dorandoran' \
  -e SPRING_DATASOURCE_USERNAME='doran' \
  -e SPRING_DATASOURCE_PASSWORD='doran' \
  -e SPRING_REDIS_HOST='dorandoran-redis' \
  -e SPRING_REDIS_PORT='6379' \
  -e SPRING_MAIL_HOST='smtp.gmail.com' \
  -e SPRING_MAIL_PORT='587' \
  -e SPRING_MAIL_USERNAME='kdhdaniel0506@gmail.com' \
  -e SPRING_MAIL_PASSWORD='새로_생성한_App_Password' \
  -e FRONTEND_URL='http://localhost:3000' \
  -e BACKEND_URL='http://3.21.177.186:8080' \
  -e USER_SERVICE_URL='http://dorandoran-user:8082' \
  dorandoran-auth:latest
```

**또는 기존 컨테이너에 환경 변수만 업데이트:**
```bash
# 컨테이너 재시작하며 환경 변수 업데이트
docker stop dorandoran-auth
docker rm dorandoran-auth
# 위의 docker run 명령어 실행
```

---

### 4. 설정 확인 체크리스트

- [ ] Gmail 2단계 인증 활성화됨
- [ ] App Password 생성 완료 (16자리)
- [ ] App Password 공백 제거 확인
- [ ] Docker 환경 변수에 올바른 값 설정됨
  - `SPRING_MAIL_USERNAME`: `kdhdaniel0506@gmail.com`
  - `SPRING_MAIL_PASSWORD`: `새로_생성한_App_Password` (공백 없이)

---

### 5. 테스트 방법

**5-1. 로컬에서 SMTP 연결 테스트 (선택사항)**
```bash
# telnet으로 SMTP 연결 테스트
telnet smtp.gmail.com 587

# 또는 openssl 사용
openssl s_client -connect smtp.gmail.com:587 -starttls smtp
```

**5-2. API 테스트**
```bash
# Postman 또는 curl로 테스트
curl -X POST http://3.21.177.186:8080/api/auth/email/request-verification \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

**5-3. 로그 확인**
```bash
# Auth 서비스 로그 확인
docker logs dorandoran-auth --tail 50 | grep -E '(이메일|mail|Authentication)'
```

---

### 6. 일반적인 문제 해결

**문제 1: "Username and Password not accepted"**
- ✅ 해결: App Password가 올바른지 확인, 공백 제거 확인
- ✅ 해결: 2단계 인증이 활성화되어 있는지 확인

**문제 2: "Could not connect to SMTP host"**
- ✅ 해결: 방화벽/네트워크 설정 확인 (포트 587)
- ✅ 해결: Docker 네트워크 설정 확인

**문제 3: "Daily sending quota exceeded"**
- ⚠️ Gmail 일일 500통 제한 초과
- ✅ 해결: 다음날까지 대기하거나, AWS SES 등 다른 SMTP 서비스 사용 고려

---

### 7. App Password 삭제 (필요시)

1. https://myaccount.google.com/apppasswords 접속
2. 불필요한 App Password 선택
3. 삭제 버튼 클릭

---

## 빠른 참조 링크

- Google 계정 보안: https://myaccount.google.com/security
- 2단계 인증: https://myaccount.google.com/signinoptions/two-step-verification
- App Password: https://myaccount.google.com/apppasswords

