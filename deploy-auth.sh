#!/bin/bash
# Auth 서비스 배포 (서버에서 실행 시)
# 단일 소스: /home/ec2-user/dorandoran-firebase.json
# 환경변수: /home/ec2-user/dorandoran-auth.env 에 동기화 후 로드

set -euo pipefail
ENV_FILE="${ENV_FILE:-/home/ec2-user/dorandoran-auth.env}"
JSON_FILE="${JSON_FILE:-/home/ec2-user/dorandoran-firebase.json}"

if [ ! -f "$JSON_FILE" ]; then
  echo "[ERROR] Firebase JSON 파일이 없습니다: $JSON_FILE" >&2
  exit 1
fi
if [ ! -f "$ENV_FILE" ]; then
  echo "[ERROR] Auth env 파일이 없습니다: $ENV_FILE" >&2
  exit 1
fi

python3 - <<'PY'
import base64
import json
import os
from pathlib import Path

env_path = Path(os.environ.get("ENV_FILE", "/home/ec2-user/dorandoran-auth.env"))
json_path = Path(os.environ.get("JSON_FILE", "/home/ec2-user/dorandoran-firebase.json"))

raw = json_path.read_text(encoding="utf-8")
data = json.loads(raw)

project_id = (data.get("project_id") or "").strip()
client_email = (data.get("client_email") or "").strip()
private_key = (data.get("private_key") or "").strip()

if not project_id or not client_email or not private_key:
    raise SystemExit("[ERROR] Firebase JSON에 project_id/client_email/private_key가 모두 필요합니다.")
if "-----BEGIN PRIVATE KEY-----" not in private_key or "-----END PRIVATE KEY-----" not in private_key:
    raise SystemExit("[ERROR] Firebase private_key PEM 형식이 올바르지 않습니다.")

private_key_env = private_key.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\n")
admin_json_base64 = base64.b64encode(raw.encode("utf-8")).decode("ascii")

lines = env_path.read_text(encoding="utf-8").splitlines()
drop_prefixes = (
    "FIREBASE_PROJECT_ID=",
    "FIREBASE_CLIENT_EMAIL=",
    "FIREBASE_PRIVATE_KEY=",
    "FIREBASE_ADMIN_JSON_BASE64=",
)
kept = [ln for ln in lines if not ln.startswith(drop_prefixes)]
kept.extend([
    f"FIREBASE_PROJECT_ID={project_id}",
    f"FIREBASE_CLIENT_EMAIL={client_email}",
    f"FIREBASE_PRIVATE_KEY={private_key_env}",
    f"FIREBASE_ADMIN_JSON_BASE64={admin_json_base64}",
])

tmp_path = env_path.with_suffix(".env.new")
tmp_path.write_text("\n".join(kept) + "\n", encoding="utf-8")
tmp_path.replace(env_path)
print("[OK] Firebase env 동기화 완료")
PY

chmod 600 "$ENV_FILE"

docker stop dorandoran-auth 2>/dev/null || true
docker rm dorandoran-auth 2>/dev/null || true

docker run -d \
  --name dorandoran-auth \
  --network dorandoran-network \
  -p 8081:8081 \
  --restart=unless-stopped \
  --env-file "$ENV_FILE" \
  -e SPRING_PROFILES_ACTIVE=docker \
  dorandoran-auth:latest

echo "Auth deployment done."
