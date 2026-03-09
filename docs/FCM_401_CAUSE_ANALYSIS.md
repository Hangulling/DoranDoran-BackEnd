# FCM 401 원인 분석 (서비스 로직 관점)

> 401 Unauthorized는 FCM 서버가 **요청에 붙은 OAuth2 액세스 토큰을 거부**할 때 발생한다.  
> 여기서는 **우리 코드에서 자격 증명을 어떻게 쓰는지**만 정리한다.

---

## 1. 현재 플로우 요약

```
FIREBASE_ADMIN_JSON_BASE64 (env)
  → FirebaseConfig.resolveJson(): trim() 후 Base64 디코딩 → JSON 문자열
  → replace("\\n", "\n") 후 ByteArrayInputStream
  → GoogleCredentials.fromStream(stream)  // 여기서 한 번만 읽음
  → FirebaseOptions.builder().setCredentials(...).build()  // projectId 미지정
  → FirebaseApp.initializeApp(options)
  → PushNotificationService: firebaseAppProvider.getIfAvailable()
  → FirebaseMessaging.getInstance(firebaseApp).send(message)  // 여기서 401 발생
```

- **401이 나는 위치**: `FirebaseMessaging.getInstance(firebaseApp).send(message)` 호출 시, SDK가 `https://fcm.googleapis.com/v1/projects/{projectId}/messages:send` 로 보내는 요청에 대한 응답으로 401을 받는 상황.

---

## 2. 서비스 로직 내에서 의심할 수 있는 지점

### 2.1 자격 증명이 “한 번만” 쓰이는지

- `GoogleCredentials.fromStream(stream)` 호출 시 **스트림은 한 번 읽고 닫힌다**.
- `FirebaseOptions`는 이렇게 만들어진 **Credentials 객체**만 들고 있고, 스트림을 다시 읽지 않는다.
- 따라서 “스트림을 두 번 읽어서 잘못된 값이 들어갔다”는 가능성은 낮다.

### 2.2 JSON·Base64 처리

- `resolveJson()`: `firebaseAdminJsonBase64.trim()` 후 `Base64.getDecoder().decode()`.
  - Base64 디코딩이 실패하면 여기서 예외 → 앱 기동 실패.  
  - **앱이 떠 있다면 디코딩은 성공한 상태**이므로, “완전히 깨진 Base64”로 인한 401 가능성은 낮다.
- `normalized = jsonToUse.replace("\\n", "\n")`: JSON 안의 `\n`(역슬래시+n)을 실제 줄바꿈으로 바꾼다.
  - 서비스 계정 JSON의 `private_key` 필드는 보통 이 이스케이프된 `\n`을 쓰므로, **이 변환은 필요**하다.
  - 이 변환을 하지 않으면 `GoogleCredentials.fromStream()` 단계에서 파싱 오류로 앱이 안 뜰 가능성이 크다.  
  → 여기서 실수로 “잘못된 치환”을 하고 있을 가능성은 낮다.

### 2.3 ProjectId 미지정

- 현재 `FirebaseOptions`에는 **`setProjectId()`를 호출하지 않는다**.
- FCM HTTP v1 URL은 `https://fcm.googleapis.com/v1/projects/{projectId}/messages:send` 형태이므로, SDK가 **projectId를 어딘가에서** 가져와야 한다.
- 일반적으로는 **Credentials(서비스 계정 JSON) 안의 `project_id`** 를 사용한다.
- 다만, SDK/버전에 따라 “Options에 projectId가 없으면 Credentials에서만 채우는데, 그 경로가 실패하는 경우”가 있을 수 있다.  
→ **Options에 JSON에서 파싱한 projectId를 명시**해 보는 것은 의미 있다.

### 2.4 OAuth2 Scope

- `GoogleCredentials.fromStream(stream)` 은 **별도 scope를 지정하지 않으면 기본 scope**를 사용한다.
- FCM HTTP v1은 보통  
  `https://www.googleapis.com/auth/cloud-platform`  
  또는  
  `https://www.googleapis.com/auth/firebase.messaging`  
  scope가 필요하다.
- 기본값이 이 scope를 포함하지 않는 환경/버전이 있으면, **토큰은 발급되지만 FCM API 호출만 401**이 날 수 있다.  
→ **FCM용 scope를 명시적으로 요청**해 보는 것이 좋다.

### 2.5 env 값이 컨테이너에 어떻게 들어가는지

- `dorandoran-user.env` 에서 `FIREBASE_ADMIN_JSON_BASE64=<한 줄 base64>` 형태로 넣었다.
- **env 한 줄 안에 개행이 들어가면**, 그 뒤는 “다음 변수”로 인식되어 **base64가 중간에 잘릴 수 있다**.
- 잘리면 JSON이 깨지고, 그걸로 초기화하면 보통 **앱 기동 단계에서 실패**한다.  
  다만 “일부만 잘려서 private_key 끝이나 client_email이 깨진 상태로 파싱되는” 극단적인 경우라면, 이론상 **잘못된 자격 증명으로 요청이 나가 401**이 될 수는 있다.
- **대응**: 서버에서 실제 로드된 값을 검증할 수 있도록, `FirebaseConfig` 에서 초기화 시점에 `project_id` / `client_email` 정도만 로그에 남기고, 그 로그로 “의도한 프로젝트/계정이 맞는지” 확인하는 것이 좋다. (이미 `logProjectInfo` 로 구현해 둔 상태면, 배포 후 해당 로그 확인.)

### 2.6 FirebaseApp 인스턴스

- `PushNotificationService` 는 `ObjectProvider<FirebaseApp>` 로 **우리가 만든 FirebaseApp** 만 받아서 `FirebaseMessaging.getInstance(firebaseApp).send(message)` 에 넘긴다.
- 즉, **기본(디폴트) FirebaseApp 이나 다른 설정**을 쓰는 경로는 없다.  
  `GOOGLE_APPLICATION_CREDENTIALS` 등이 있어도, 우리가 초기화한 `FirebaseApp` 기준으로만 FCM을 쓰므로, “다른 자격 증명이 섞였다”는 가능성은 낮다.

---

## 3. 서비스 로직 관점에서 시도해 볼 수 있는 수정

아래는 “우리 코드에서 할 수 있는” 변경이다.  
(키 만료/삭제, IAM 권한, 프로젝트 불일치 등은 GCP 쪽 설정이므로 여기서는 다루지 않는다.)

1. **FirebaseOptions에 projectId 명시**  
   - `resolveJson()` 으로 얻은 JSON에서 `project_id` 를 파싱해,  
     `FirebaseOptions.builder().setCredentials(...).setProjectId(projectId).build()` 로 설정.
   - FCM v1 URL에 쓰이는 projectId가 우리가 의도한 프로젝트로 고정되도록 한다.

2. **GoogleCredentials에 FCM용 scope 명시**  
   - 예:  
     `GoogleCredentials.fromStream(stream).createScoped("https://www.googleapis.com/auth/cloud-platform");`  
   - 또는  
     `Collections.singleton("https://www.googleapis.com/auth/firebase.messaging")`  
   - 이 Credentials를 `FirebaseOptions` 에 넣어서 사용.
   - “토큰은 받지만 FCM만 401” 인 경우, scope 부족 가능성을 줄일 수 있다.

3. **초기화 시점 검증**  
   - 이미 `logProjectInfo` 로 `project_id`, `client_email` 을 로그에 남기고 있다면,  
     배포 후 **해당 로그가 한 번만 나오고, 값이 예상한 프로젝트/서비스 계정인지** 확인.
   - 값이 비어 있거나 이상하면, env/base64가 잘렸거나 JSON이 깨진 것이다.

---

## 4. 정리

| 구분 | 내용 |
|------|------|
| 401이 나는 곳 | FCM HTTP v1 API (`messages:send`) 호출 시, 서버가 돌려주는 401 |
| 우리 코드에서 가능한 원인 | (1) ProjectId 미지정으로 URL/토큰 대상 프로젝트 꼬임, (2) OAuth2 scope 미지정, (3) env/base64 잘림으로 잘못된 자격 증명 로드 |
| 코드로 할 수 있는 대응 | ProjectId 명시, FCM용 scope 명시, 초기화 로그로 project_id/client_email 검증 |

그래도 401이 계속되면, **키 만료/삭제, 다른 프로젝트용 키 사용, IAM 역할 부족** 등은 GCP 콘솔에서 확인해야 한다.
