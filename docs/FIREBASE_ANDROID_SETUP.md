# Firebase Android 설정 가이드

Capacitor로 포팅한 Android 앱에 Firebase를 적용하는 절차입니다.

## 📋 사전 준비사항

- Firebase Console 접근 권한
- Android 패키지명: `com.doranchat.app`
- Google Services 플러그인 이미 설정됨 (확인 완료)

## 🔥 1단계: Firebase 프로젝트 생성

### 1.1 Firebase Console 접속
1. [Firebase Console](https://console.firebase.google.com/) 접속
2. Google 계정으로 로그인

### 1.2 프로젝트 생성 또는 선택
1. 기존 프로젝트가 있으면 선택, 없으면 "프로젝트 추가" 클릭
2. 프로젝트 이름 입력 (예: "도란도란" 또는 "DoranDoran")
3. Google Analytics 설정 (선택사항, 권장: 활성화)
4. 프로젝트 생성 완료

## 📱 2단계: Android 앱 등록

### 2.1 Android 앱 추가
1. Firebase 프로젝트 대시보드에서 **Android 아이콘** 클릭
2. Android 앱 추가 화면에서:
   - **Android 패키지 이름**: `com.doranchat.app` 입력
   - **앱 닉네임**: `도란도란` (선택사항)
   - **디버그 서명 인증서 SHA-1**: (선택사항, 나중에 추가 가능)
3. **앱 등록** 클릭

### 2.2 google-services.json 다운로드
1. **google-services.json 다운로드** 버튼 클릭
2. 파일을 다운로드 받음

## 📁 3단계: google-services.json 파일 배치

### 3.1 파일 위치
다운로드한 `google-services.json` 파일을 다음 위치에 배치:

```
dorandoran-frontend/android/app/google-services.json
```

**중요**: 
- `public/google-services.json`이 아닌 `android/app/google-services.json`에 배치해야 합니다.
- 현재 `public` 폴더에 있는 파일은 OAuth 설정 파일이므로 Firebase용 파일과는 다릅니다.

### 3.2 파일 확인
파일이 올바른 위치에 있는지 확인:
```bash
# 프로젝트 루트에서
ls -la dorandoran-frontend/android/app/google-services.json
```

## 🔧 4단계: Gradle 설정 확인

### 4.1 프로젝트 레벨 build.gradle (이미 설정됨)
`dorandoran-frontend/android/build.gradle` 파일에 다음이 이미 포함되어 있는지 확인:

```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.2'  // ✅ 이미 있음
    }
}
```

### 4.2 앱 레벨 build.gradle (이미 설정됨)
`dorandoran-frontend/android/app/build.gradle` 파일에 다음이 이미 포함되어 있는지 확인:

```gradle
// 파일 하단에
try {
    def servicesJSON = file('google-services.json')
    if (servicesJSON.text) {
        apply plugin: 'com.google.gms.google-services'  // ✅ 이미 있음
    }
} catch(Exception e) {
    logger.info("google-services.json not found, google-services plugin not applied. Push Notifications won't work")
}
```

## 📦 5단계: Firebase SDK 의존성 추가

### 5.1 필요한 Firebase 서비스 선택
사용하려는 Firebase 서비스에 따라 의존성을 추가합니다:

#### Firebase Analytics (기본, 권장)
`dorandoran-frontend/android/app/build.gradle`의 `dependencies` 섹션에 추가:

```gradle
dependencies {
    // ... 기존 의존성들 ...
    
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:33.7.0')
    implementation 'com.google.firebase:firebase-analytics'
    implementation 'com.google.firebase:firebase-messaging'  // FCM (푸시 알림)
}
```

#### Firebase Authentication (Google 로그인 등)
```gradle
implementation 'com.google.firebase:firebase-auth'
```

#### Firebase Cloud Firestore (데이터베이스)
```gradle
implementation 'com.google.firebase:firebase-firestore'
```

#### Firebase Storage (파일 저장)
```gradle
implementation 'com.google.firebase:firebase-storage'
```

### 5.2 전체 예시 (Analytics + Messaging)
`dorandoran-frontend/android/app/build.gradle`:

```gradle
dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation "androidx.appcompat:appcompat:$androidxAppCompatVersion"
    implementation "androidx.coordinatorlayout:coordinatorlayout:$androidxCoordinatorLayoutVersion"
    implementation "androidx.core:core-splashscreen:$coreSplashScreenVersion"
    implementation project(':capacitor-android')
    testImplementation "junit:junit:$junitVersion"
    androidTestImplementation "androidx.test.ext:junit:$androidxJunitVersion"
    androidTestImplementation "androidx.test.espresso:espresso-core:$androidxEspressoCoreVersion"
    implementation project(':capacitor-cordova-android-plugins')
    
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:33.7.0')
    implementation 'com.google.firebase:firebase-analytics'
    implementation 'com.google.firebase:firebase-messaging'
}
```

## 🚀 6단계: Firebase 초기화 (선택사항)

### 6.1 Java/Kotlin에서 초기화
일반적으로 `google-services.json` 파일만 있으면 자동으로 초기화되지만, 
필요한 경우 `MainActivity.java`에서 초기화할 수 있습니다:

`dorandoran-frontend/android/app/src/main/java/com/doranchat/app/MainActivity.java`:

```java
package com.doranchat.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import com.google.firebase.FirebaseApp;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Firebase 초기화 (일반적으로 자동으로 초기화됨)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }
}
```

## ✅ 7단계: 빌드 및 테스트

### 7.1 Gradle 동기화
Android Studio에서:
1. **File > Sync Project with Gradle Files** 클릭
2. 또는 터미널에서:
```bash
cd dorandoran-frontend/android
./gradlew clean
./gradlew build
```

### 7.2 빌드 확인
빌드가 성공하면 Firebase가 정상적으로 연동된 것입니다.

### 7.3 로그 확인
앱 실행 후 Logcat에서 Firebase 초기화 로그 확인:
```
FirebaseApp initialization successful
```

## 🔍 8단계: Firebase 기능 테스트

### 8.1 Firebase Analytics 테스트
1. 앱 실행
2. Firebase Console > Analytics > 이벤트에서 앱 실행 이벤트 확인
3. 몇 분 후 데이터가 표시됨

### 8.2 Firebase Cloud Messaging (FCM) 테스트
1. Firebase Console > Cloud Messaging에서 테스트 메시지 전송
2. 앱에서 알림 수신 확인

## 📝 주의사항

1. **google-services.json 파일 위치**
   - ✅ 올바른 위치: `android/app/google-services.json`
   - ❌ 잘못된 위치: `public/google-services.json`

2. **패키지명 일치**
   - Firebase Console에 등록한 패키지명과 `AndroidManifest.xml`의 패키지명이 일치해야 함
   - 현재: `com.doranchat.app`

3. **SHA-1 인증서 (Google 로그인용)**
   - Google Sign-In을 사용하는 경우, SHA-1 인증서를 Firebase Console에 추가해야 함
   - 디버그 키스토어 SHA-1 확인:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

4. **프로덕션 빌드**
   - 릴리즈 빌드 시 별도의 키스토어를 사용하는 경우, 해당 키스토어의 SHA-1도 추가해야 함

## 🐛 문제 해결

### 문제: "google-services.json not found"
- **원인**: 파일이 잘못된 위치에 있음
- **해결**: `android/app/google-services.json` 위치 확인

### 문제: 빌드 에러 "Plugin with id 'com.google.gms.google-services' not found"
- **원인**: 프로젝트 레벨 build.gradle에 classpath가 없음
- **해결**: `android/build.gradle`에 `classpath 'com.google.gms:google-services:4.4.2'` 추가 확인

### 문제: Firebase 초기화 실패
- **원인**: google-services.json 파일 형식 오류 또는 패키지명 불일치
- **해결**: Firebase Console에서 새로 다운로드한 파일로 교체

## 📚 추가 리소스

- [Firebase Android 문서](https://firebase.google.com/docs/android/setup)
- [Capacitor Firebase 플러그인](https://capacitorjs.com/docs/guides/push-notifications-firebase)
- [Firebase Console](https://console.firebase.google.com/)











