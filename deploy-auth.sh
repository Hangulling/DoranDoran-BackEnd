#!/bin/bash
docker stop dorandoran-auth 2>/dev/null || true
docker rm dorandoran-auth 2>/dev/null || true
docker run -d \
  --name dorandoran-auth \
  --network dorandoran-network \
  -p 8081:8081 \
  --restart=unless-stopped \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://dorandoran-shared-db:5432/dorandoran' \
  -e SPRING_DATASOURCE_USERNAME='doran' \
  -e SPRING_DATASOURCE_PASSWORD='doran' \
  -e SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=auth_schema \
  -e SPRING_REDIS_HOST='dorandoran-redis' \
  -e SPRING_REDIS_PORT='6379' \
  -e SPRING_MAIL_HOST='smtp.gmail.com' \
  -e SPRING_MAIL_PORT='587' \
  -e SPRING_MAIL_USERNAME='dorandoran202509@gmail.com' \
  -e SPRING_MAIL_PASSWORD='znhcmqhjrvwodrso' \
  -e FRONTEND_URL='https://www.doran-chat.com' \
  -e BACKEND_URL='https://api.doran-chat.com' \
  -e GOOGLE_OAUTH_CLIENT_ID='7033117549-66689tp0iqof4s6h6prflfoont2g9hnv.apps.googleusercontent.com' \
  -e GOOGLE_OAUTH_ANDROID_CLIENT_IDS='7033117549-m863ru45qu4l8ham1796j357hi1jc3sp.apps.googleusercontent.com,7033117549-d7k7vr58pk5vhfjsatc1d09bfl2i5i12.apps.googleusercontent.com,29324186506-c971u3g05h8meusc6133888052r642r0.apps.googleusercontent.com' \
  -e GOOGLE_OAUTH_CLIENT_SECRET='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' \
  -e FIREBASE_PROJECT_ID='dorandoran-firebase' \
  -e FIREBASE_CLIENT_EMAIL='firebase-adminsdk-fbsvc@dorandoran-firebase.iam.gserviceaccount.com' \
  -e FIREBASE_PRIVATE_KEY='-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCGW9fLhK6H/TMX
wkYeh++xTxi7wQ5tbH6rxvF4QmLzdSvcL54XLVgknY43P6iceMDAh7HLKhyLxSrA
L1ss6k2Q0m2DAgqumiI5I2hc4ao9kcn+mvweCnbY5bJgMOEIxWtsNnBwCV3aX79V
lTasLE4pPEXGKIJ6tWx44NqxkVBqOVFI7a1j3QHzD5ytBKC4by5sntrAagzhXaIN
Zalo+JRH/wgs62SS1htm0we3leC807UWpqg3PmDrHFhsYaKQGM42uruvCHznKd8/
rS/fQlQdTJsMFyAluBTNBMhOxeJhS+ckKKcbKhBDB8AONzM3+nVgcvLkz6aqzgS6
fJ6MV5V/AgMBAAECggEACRfsylJ1lPnEyeq5u1XVYwu/j4IfnbW9HrwGY4DZmMM7
iyLIw6Y93RtoxWZ+AafzIp3exfKh1oorPGgTiT34rtL/JHQL7C2Y+6eXsRLCCwxz
rBxzOdlGmjYHVU3ivSjOR3LOE1ce8ylpOrvCwDwcj9tUmHaj/z73U3jH4h4phG25
kmL1c5a459M0aYi16UEY+eab6vXcAJlyyCtcH1ijXuoeMaSVlOY4uJUpH3yIjHC1
7PVahDNFGHBzJfooQ+xpszTJ3ujUAbdZEdBjWNhMIoeUmvT/YggV2AAQSJj4/h7j
ViI+2gmdLIplhjKRMI8TITDQtZc68M/kIB2Gt+KcyQKBgQC84MG2bD4h8wq7YjKy
wbUPXlCgnFZyE7GjhPGeUEg6wU83puL/O3hofPeyZ5ryy3caw9YK/x2xEbLyvKT/
2zf2kUEPSusRVzHMAogKOMoU0WVtYx6Ng0J8YSxd75Gbv6zUSTzDHKpRHrRBMZbo
mpV50TnVeGtxLIodClbPpF9XjQKBgQC2Gy6WdeZUtbuzL0Qr9+wMVN2imwhC/fhQ
IaVjtuqmcfJx06GgBJsBkN/+AFx7j55PpIHOMhzm4HzJ4VRfE337PuU3l8d4dYxI
ofBenCjCEH9dmT7zAlSOdu6aXom+KZSiQGJbJsg3bZEbYD6bUXNhRXdarVMWGhkK
72pZvKoIOwKBgDXhyGFKL89vjjg2o0/rgsOHnGi1MNQKWjrTyjnnnTNlwjuWGFvH
k2n7fI4gkEuDT88K3xzz+nxxGCO3PTrgRu7IMU8ziCvCPCp+7mA8IyodMloqVKT6
NRVWg1mo5lkQLTXCTts6ajwKDlIq22ov70R5M456c+wtUJlIQcuqkKhhAoGBAIwp
e8Ae2rtUbTXLKYNV/P64yyX+gWteoqK6YtvOy+h0R5DIvNu2779SzLRpfsUzkzvl
neGtGMuyY2DNiFVhgXlTk4fPaBJN4lhLJYYLQWqaToBRggQz1PDOy4o86OHZ+9oGz
YICb5vqSgF87wrQ1CsNRfj3yrcKTL7c08vxCs/RFAoGAFjxDCjR4sinFkrP5zUgq
3wH7SiIHp2hcfr7l8wGmXTV7oX5Ym9WuIE4y297uyH/DUJ7VXLjc9SxeeTqvdVFJ
boiq1ou9TUTPUlzdACpzCakaw1WvsNexd1Ow6PeWtCH5GF9Be+20SqeXKOw6dMea
R4Iw4ZArEf1nczouigHTDt4=
-----END PRIVATE KEY-----' \
  dorandoran-auth:latest







