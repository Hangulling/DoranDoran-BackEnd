# User 서비스 Docker 배포 (푸시 알림 환경변수 포함)

User 서비스(dorandoran-user) 배포 시 필요한 환경변수와 최종 실행 명령어입니다.  
푸시(FCM)를 사용하려면 **FIREBASE_ADMIN_JSON**을 반드시 설정합니다.

## 1. 서버 준비 (최초 1회)

### 1.1 Firebase JSON 복사

로컬 PowerShell:

```powershell
scp -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" "C:\Users\KDH\Downloads\dorandoran-firebase-e40ce3eb38e9.json" ec2-user@3.21.177.186:/home/ec2-user/dorandoran-firebase.json
```

### 1.2 env 파일 생성

SSH 접속 후:

```bash
echo -n 'FIREBASE_ADMIN_JSON=' > /home/ec2-user/dorandoran-user.env
cat /home/ec2-user/dorandoran-firebase.json | tr -d '\n' >> /home/ec2-user/dorandoran-user.env
chmod 600 /home/ec2-user/dorandoran-firebase.json /home/ec2-user/dorandoran-user.env
```

### 1.3 이미지 준비

서버에 `dorandoran-user-latest.tar`가 있어야 합니다 (빌드 후 `docker save`로 복사).

---

## 2. 환경변수 요약

| 변수 | 필수 | 설명 |
|------|------|------|
| `FIREBASE_ADMIN_JSON` | 푸시 사용 시 | Firebase 서비스 계정 JSON 전체 (env 파일 권장) |
| `SPRING_PROFILES_ACTIVE` | O | `docker` |
| `SPRING_DATASOURCE_*` | O | DB 연결 |
| `SPRING_REDIS_*` | O | Redis |
| `AUTH_SERVICE_URL` | O | `http://dorandoran-auth:8081` |
| `CHAT_SERVICE_URL` | O | `http://dorandoran-chat:8083` |
| `APP_DEEPLINK_*` | 선택 | 기본값: dorandoran://chat, https://www.doran-chat.com/chat |
| `INSTAGRAM_*` | 선택 | 인스타 연동 시 |

---

## 3. 최종 실행 명령어 (PowerShell)

로컬에서 이미지 로드 + 컨테이너 재생성:

```powershell
ssh -i "$env:USERPROFILE\Downloads\dorandoran-key.pem" ec2-user@3.21.177.186 "docker load -i /home/ec2-user/dorandoran-user-latest.tar && docker stop dorandoran-user 2>/dev/null || true && docker rm dorandoran-user 2>/dev/null || true && docker run -d --name dorandoran-user --network dorandoran-network -p 8082:8082 --restart=unless-stopped --env-file /home/ec2-user/dorandoran-user.env -e SPRING_PROFILES_ACTIVE=docker -e SPRING_DATASOURCE_URL='jdbc:postgresql://dorandoran-shared-db:5432/dorandoran' -e SPRING_DATASOURCE_USERNAME='doran' -e SPRING_DATASOURCE_PASSWORD='doran' -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=user_schema -e SPRING_REDIS_HOST='dorandoran-redis' -e SPRING_REDIS_PORT='6379' -e AUTH_SERVICE_URL='http://dorandoran-auth:8081' -e CHAT_SERVICE_URL='http://dorandoran-chat:8083' -e SPRING_FLYWAY_ENABLED=false -e INSTAGRAM_ENABLED=true -e INSTAGRAM_ACCESS_TOKEN='IGAARluUwuEiRBZAFprRWlOWXNBQlozenN0Q3NIMVZAhZAU51Nkp1NGZAEWUNvSHl2WEtvUDVXNzZAiX084aFh1NHItOXdvcmdqTmZA6SXlWTm85b2U2dWFWdmp0VUlQZAkpoYU5vMFFYV29QUTFZAc1pFd3RqN2M3ZA3U5MFZAkMnBoVXbiZAwZDZD' -e APP_DEEPLINK_SCHEME='dorandoran://chat' -e APP_DEEPLINK_UNIVERSAL_BASE='https://www.doran-chat.com/chat' dorandoran-user:latest"
```

- `--env-file /home/ec2-user/dorandoran-user.env`: FIREBASE_ADMIN_JSON 주입.
- 나머지는 `-e`로 전달. 값 변경 시 위 명령에서 해당 `-e`만 수정하면 됨.

---

## 4. 보안

- Firebase JSON·env 파일은 **chmod 600** 유지 (소유자만 읽기/쓰기).
- 해당 파일은 Git/배포 패키지에 포함하지 말 것.
