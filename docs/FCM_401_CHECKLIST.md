# FCM 401 점검: 서비스 계정·프로젝트 확인

> 푸시 발송 시 `Unexpected HTTP response with status: 401` 원인 점검용 체크리스트.

---

## 1. 서버에 적용된 Firebase 정보 (확인된 값)

| 항목 | 값 |
|------|-----|
| **project_id** | `dorandoran-firebase` |
| **client_email** | env에서 추출 실패 (디코딩 길이 1881자 → **전체 JSON이 잘렸을 가능성 있음**) |

- 확인 방법: 서버에서 `python3 /home/ec2-user/check_firebase_project.py` 실행  
- **정상이면** `project_id`와 `client_email`이 **둘 다** 출력됨.  
- **client_email이 (not found)이고 decoded_len ≈ 1881**이면, Base64 값이 **한 줄 전체가 아닌 잘린 상태**로 들어갔을 수 있음 → **한 줄로 된 전체 Base64**로 다시 넣고 재기동 필요.

- **앱(모바일)의 Firebase 프로젝트**가 위 `project_id`와 **동일**해야 FCM 발송이 동작합니다.

---

## 2. 앱과 서버의 프로젝트 일치 여부

- **모바일 앱**  
  - `google-services.json`(Android) / `GoogleService-Info.plist`(iOS) 안의 **프로젝트 ID**가 `dorandoran-firebase`인지 확인.
- **서버(User 서비스)**  
  - `FIREBASE_ADMIN_JSON_BASE64`에 넣은 서비스 계정 JSON의 **project_id**가 `dorandoran-firebase`인지 확인.  
  - 위 스크립트로 확인했을 때 **project_id: dorandoran-firebase** 로 나오면 서버 설정은 이 프로젝트를 가리키고 있는 상태입니다.

---

## 3. 401이 나는 경우 점검 순서

1. **서비스 계정 키가 올바른 프로젝트 것인지**  
   - GCP / Firebase 콘솔에서  
     **프로젝트: dorandoran-firebase** → 프로젝트 설정 → 서비스 계정 → 해당 서비스 계정의 **키**인지 확인.

2. **키가 손상/잘리지 않았는지**  
   - `FIREBASE_ADMIN_JSON_BASE64` 값을 **한 줄**로 넣었는지 확인.  
   - 줄바꿈이 들어가면 디코딩 시 JSON이 깨질 수 있음.  
   - 서버에 올린 뒤 `check_firebase_project.py`를 다시 실행해  
     `project_id`와 `client_email`이 **둘 다** 출력되는지 확인.

3. **서비스 계정 권한**  
   - GCP 콘솔 → IAM → 해당 서비스 계정에  
     **Firebase Admin** 또는 **Editor** 등 FCM 사용에 필요한 역할이 있는지 확인.

4. **Cloud Messaging API 사용 설정**  
   - GCP 콘솔 → **API 및 서비스** → **Firebase Cloud Messaging API** (또는 **FCM API**)가 **사용** 상태인지 확인.

5. **키 재발급**  
   - 위가 모두 맞는데도 401이면,  
     동일 서비스 계정으로 **새 키를 만들고** → JSON 전체를 Base64 인코딩(한 줄) →  
     `dorandoran-user.env`의 `FIREBASE_ADMIN_JSON_BASE64`를 교체한 뒤 컨테이너 재기동.

---

## 4. 서버에서 적용 값 재확인

```bash
# 서버 접속 후
python3 /home/ec2-user/check_firebase_project.py
```

- `project_id`와 `client_email`이 둘 다 나와야 정상.
- `client_email`이 `(not found)`이거나 `(decoded_len: ...)` 메시지가 나오면  
  env에 넣은 Base64가 잘렸거나 줄바꿈이 섞인 가능성이 있으므로, **한 줄 Base64**로 다시 넣기.

---

## 5. User 서비스 초기화 로그로 확인 (선택)

- `FirebaseConfig`에 **project_id / client_email**만 로그로 남기도록 수정해 두었음.  
- User 서비스를 재배포하면 시작 시 로그에 예시처럼 출력됩니다.  
  - `Firebase Admin 초기화: project_id=dorandoran-firebase, client_email=...`  
- 이 로그로 실제로 로드된 프로젝트/계정을 한 번 더 확인할 수 있습니다.

---

## 6. 요약

| 확인 항목 | 내용 |
|-----------|------|
| 서버 project_id | `dorandoran-firebase` (스크립트로 확인됨) |
| 앱 프로젝트 | `google-services.json` 등에서 동일한지 확인 |
| env 값 | Base64 한 줄, 잘리지 않았는지 확인 |
| 권한/API | 서비스 계정 역할, FCM API 사용 설정 |
| 401 지속 시 | 같은 프로젝트·서비스 계정으로 키 재발급 후 env 교체·재기동 |
