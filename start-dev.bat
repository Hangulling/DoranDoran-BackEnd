@echo off
echo ========================================
echo   DoranDoran MSA 개발 환경 시작
echo ========================================
echo.

echo [1/4] 기존 컨테이너 정리...
docker compose -f docker/docker-compose.yml down

echo [2/4] Gradle 빌드...
call gradlew build -x test

echo [3/4] Docker 이미지 빌드...
docker compose -f docker/docker-compose.yml build

echo [4/4] MSA 환경 시작...
docker compose -f docker/docker-compose.yml up -d

echo.
echo ========================================
echo   서비스 시작 완료!
echo ========================================
echo.
echo 📊 접속 URL:
echo   - API Gateway: http://localhost:8080
echo   - Auth Service: http://localhost:8081
echo   - User Service: http://localhost:8082
echo   - Chat Service: http://localhost:8083
echo   - Store Service: http://localhost:8084
echo   - Prometheus: http://localhost:9090
echo   - Grafana: http://localhost:3000 (admin/admin123)
echo.
echo 🔧 유용한 명령어:
echo   - 상태 확인: docker compose -f docker/docker-compose.yml ps
echo   - 로그 확인: docker compose -f docker/docker-compose.yml logs -f
echo   - 서비스 중지: stop-dev.bat
echo   - 빌드: gradlew build
echo   - 테스트: gradlew test
echo.
pause
