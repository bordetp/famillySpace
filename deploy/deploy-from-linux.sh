#!/usr/bin/env bash
# Deploy Family Space backend to Oracle Cloud VM (Linux)
set -euo pipefail

VM_IP="141.253.105.251"
VM_USER="opc"
REMOTE_DIR="/home/opc/familyspace"
GOOGLE_CLIENT_ID="151953099656-rh76ijdiirkka8rqag42k658e1sohcgc.apps.googleusercontent.com"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEY_PATH="${ORACLE_SSH_KEY:-$HOME/.ssh/oracle.key}"

if [[ ! -f "$KEY_PATH" && -f "$PROJECT_ROOT/secrets/oracle.key" ]]; then
  KEY_PATH="$PROJECT_ROOT/secrets/oracle.key"
  chmod 600 "$KEY_PATH" 2>/dev/null || true
fi

if [[ ! -f "$KEY_PATH" ]]; then
  echo "Clé SSH introuvable : $KEY_PATH (ou secrets/oracle.key)"
  exit 1
fi

if [[ ! -f "$PROJECT_ROOT/deploy/firebase-admin.json" ]]; then
  echo "deploy/firebase-admin.json manquant — copiez le JSON Firebase Admin avant le déploiement."
  exit 1
fi

SSH=(ssh -i "$KEY_PATH" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20)
SCP=(scp -i "$KEY_PATH" -o StrictHostKeyChecking=accept-new)

echo "==> Test SSH..."
"${SSH[@]}" "${VM_USER}@${VM_IP}" "echo SSH_OK"

echo "==> Packaging project..."
ARCHIVE="$(mktemp /tmp/familyspace-deploy.XXXXXX.tar.gz)"
tar -czf "$ARCHIVE" -C "$PROJECT_ROOT" \
  --exclude=.git \
  --exclude=androidApp/build \
  --exclude=backend/build \
  --exclude=shared/build \
  --exclude=.gradle \
  --exclude=uploads \
  --exclude=keystore_familly \
  --exclude=.env \
  docker-compose.yml Dockerfile settings.docker.gradle.kts settings.gradle.kts build.gradle.kts gradle.properties \
  gradlew gradlew.bat gradle backend shared deploy

echo "==> Upload..."
"${SSH[@]}" "${VM_USER}@${VM_IP}" "mkdir -p $REMOTE_DIR"
"${SCP[@]}" "$ARCHIVE" "${VM_USER}@${VM_IP}:/tmp/familyspace-deploy.tar.gz"
rm -f "$ARCHIVE"

REMOTE_SCRIPT="$(mktemp /tmp/familyspace-remote-install.XXXXXX.sh)"
cat > "$REMOTE_SCRIPT" <<EOF
#!/bin/bash
set -euo pipefail
cd /home/opc/familyspace
tar -xzf /tmp/familyspace-deploy.tar.gz
rm -f /tmp/familyspace-deploy.tar.gz
mkdir -p deploy/certs

if [ -f /etc/letsencrypt/live/famillyspace.duckdns.org/fullchain.pem ]; then
  cp deploy/nginx-ssl.conf deploy/nginx.conf
fi

GOOGLE_ID='${GOOGLE_CLIENT_ID}'

if [ ! -f .env ]; then
  JWT_SECRET=\$(openssl rand -hex 32)
  POSTGRES_PASSWORD=\$(openssl rand -hex 16)
  cat > .env <<ENVEOF
POSTGRES_USER=familyspace
POSTGRES_PASSWORD=\${POSTGRES_PASSWORD}
POSTGRES_DB=familyspace
JWT_SECRET=\${JWT_SECRET}
JWT_ISSUER=familyspace
JWT_AUDIENCE=familyspace-users
GOOGLE_CLIENT_ID=\${GOOGLE_ID}
PUBLIC_BASE_URL=https://famillyspace.duckdns.org
MAX_UPLOAD_BYTES=52428800
FIREBASE_ADMIN_JSON=/app/firebase-admin.json
ADMIN_EMAIL=deceirem@gmail.com
ENVEOF
else
  grep -q '^GOOGLE_CLIENT_ID=' .env || echo "GOOGLE_CLIENT_ID=\${GOOGLE_ID}" >> .env
  if grep -q '^GOOGLE_CLIENT_ID=' .env; then
    sed -i "s|^GOOGLE_CLIENT_ID=.*|GOOGLE_CLIENT_ID=\${GOOGLE_ID}|" .env
  fi
  if grep -q '^PUBLIC_BASE_URL=http://141.253.105.251' .env; then
    sed -i 's|^PUBLIC_BASE_URL=.*|PUBLIC_BASE_URL=https://famillyspace.duckdns.org|' .env
  fi
  grep -q '^MAX_UPLOAD_BYTES=' .env && sed -i 's|^MAX_UPLOAD_BYTES=.*|MAX_UPLOAD_BYTES=52428800|' .env || echo 'MAX_UPLOAD_BYTES=52428800' >> .env
  grep -q '^FIREBASE_ADMIN_JSON=' .env && sed -i 's|^FIREBASE_ADMIN_JSON=.*|FIREBASE_ADMIN_JSON=/app/firebase-admin.json|' .env || echo 'FIREBASE_ADMIN_JSON=/app/firebase-admin.json' >> .env
  grep -q '^ADMIN_EMAIL=' .env && sed -i 's|^ADMIN_EMAIL=.*|ADMIN_EMAIL=deceirem@gmail.com|' .env || echo 'ADMIN_EMAIL=deceirem@gmail.com' >> .env
fi

grep GOOGLE_CLIENT_ID .env
sudo docker compose up -d --build
sleep 25
curl -sf https://famillyspace.duckdns.org/health
echo ""
echo "DEPLOY_OK"
EOF

"${SCP[@]}" "$REMOTE_SCRIPT" "${VM_USER}@${VM_IP}:/tmp/familyspace-remote-install.sh"
rm -f "$REMOTE_SCRIPT"

echo "==> Installing on VM..."
"${SSH[@]}" "${VM_USER}@${VM_IP}" "bash /tmp/familyspace-remote-install.sh"
echo "==> Done."
