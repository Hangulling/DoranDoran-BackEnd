-- Baseline: 스키마 존재 가정 (docker/init-shared-db.sql과 동일 구조 유지)
-- 운영 DB에서 이미 생성된 객체가 있다고 가정하고, Flyway는 기준선만 잡는다.

-- 주: 실제 테이블 생성은 공용 init 스크립트에서 관리. 여기서는 no-op 처리.
-- Flyway가 baseline-on-migrate=true 상태에서 V1을 통과시키기 위한 파일.

-- no-op

