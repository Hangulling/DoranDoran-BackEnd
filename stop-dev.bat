@echo off
echo ========================================
echo   DoranDoran MSA 개발 환경 중지
echo ========================================
echo.

echo [1/3] MSA 서비스 중지...
docker compose -f docker/docker-compose.yml down

echo [2/3] 사용하지 않는 리소스 정리...
docker system prune -f

echo [3/3] Gradle 캐시 정리...
call gradlew clean

echo.
echo ========================================
echo   모든 서비스가 중지되었습니다!
echo ========================================
echo.
echo 🧹 정리된 리소스:
echo   - 컨테이너: 중지됨
echo   - 네트워크: 정리됨
echo   - 사용하지 않는 이미지: 삭제됨
echo   - Gradle 캐시: 정리됨
echo.
echo 🔧 다음에 개발할 때:
echo   - 서비스 시작: start-dev.bat
echo   - 빌드: gradlew build
echo   - 테스트: gradlew test
echo.
pause
