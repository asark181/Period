#!/usr/bin/env bash
# Usage: deploy/deploy.sh <ssh-key> [apk]
# Ships the sync server, landing page and (optionally) a new APK to the Ubuntu host.
set -euo pipefail
KEY=${1:?ssh key}
APK=${2:-}
HOST=ubuntu@129.151.21.101
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SSH="ssh -i $KEY -o StrictHostKeyChecking=accept-new"

STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT
mkdir -p "$STAGE/app" "$STAGE/web/download"
cp "$ROOT/server/app.py" "$ROOT/server/requirements.txt" "$STAGE/app/"
cp -R "$ROOT/server/web/." "$STAGE/web/"
cp "$ROOT/deploy/nginx-nuanyue.conf" "$ROOT/deploy/nuanyue.service" "$ROOT/deploy/nuanyue-backup.cron" "$STAGE/"
if [[ -n "$APK" ]]; then
  cp "$APK" "$STAGE/web/download/nuanyue.apk"
  VERSION_CODE=$(grep -E 'versionCode *=' "$ROOT/android/app/build.gradle.kts" | grep -oE '[0-9]+')
  VERSION_NAME=$(grep -E 'versionName *=' "$ROOT/android/app/build.gradle.kts" | grep -oE '"[^"]+"' | tr -d '"')
  SIZE=$(du -m "$APK" | cut -f1)
  SHA=$(sha256sum "$APK" | awk '{print $1}')
  cat > "$STAGE/web/version.json" <<JSON
{"versionCode": $VERSION_CODE, "versionName": "$VERSION_NAME", "url": "https://leafy.fxxkopenai.com/", "apkUrl": "https://leafy.fxxkopenai.com/download/nuanyue.apk", "sha256": "$SHA", "size": "${SIZE}MB", "notes": ""}
JSON
fi

COPYFILE_DISABLE=1 tar --no-xattrs -C "$STAGE" -czf "$STAGE.tgz" .
scp -i "$KEY" "$STAGE.tgz" "$HOST:/tmp/nuanyue.tgz"
rm -f "$STAGE.tgz"

$SSH "$HOST" 'bash -s' <<'REMOTE'
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
if ! command -v nginx >/dev/null || ! dpkg -s python3-venv >/dev/null 2>&1 || ! command -v sqlite3 >/dev/null; then
  sudo apt-get update -qq
  sudo apt-get install -y -qq nginx python3-venv sqlite3 >/dev/null
fi
id nuanyue >/dev/null 2>&1 || sudo useradd --system --home /var/lib/nuanyue --shell /usr/sbin/nologin nuanyue
sudo mkdir -p /opt/nuanyue/app /opt/nuanyue/web/download /var/lib/nuanyue/backups
sudo chown -R nuanyue:nuanyue /var/lib/nuanyue

T=$(mktemp -d); tar -C "$T" -xzf /tmp/nuanyue.tgz; rm /tmp/nuanyue.tgz
sudo cp "$T"/app/* /opt/nuanyue/app/
sudo cp -R "$T"/web/. /opt/nuanyue/web/
sudo cp "$T"/nginx-nuanyue.conf /etc/nginx/sites-available/nuanyue
sudo ln -sf /etc/nginx/sites-available/nuanyue /etc/nginx/sites-enabled/nuanyue
sudo rm -f /etc/nginx/sites-enabled/default
sudo cp "$T"/nuanyue.service /etc/systemd/system/nuanyue.service
sudo cp "$T"/nuanyue-backup.cron /etc/cron.d/nuanyue-backup
sudo chmod 644 /etc/cron.d/nuanyue-backup
rm -rf "$T"

[[ -x /opt/nuanyue/venv/bin/python ]] || sudo python3 -m venv /opt/nuanyue/venv
sudo /opt/nuanyue/venv/bin/pip install -q --upgrade pip
sudo /opt/nuanyue/venv/bin/pip install -q -r /opt/nuanyue/app/requirements.txt

sudo systemctl daemon-reload
sudo systemctl enable -q nuanyue
sudo systemctl restart nuanyue
sudo nginx -t -q
sudo systemctl enable -q nginx
sudo systemctl reload nginx || sudo systemctl restart nginx
sleep 2
curl -fsS http://127.0.0.1/api/health && echo
systemctl is-active nuanyue nginx x-ui
REMOTE
echo "deployed → https://leafy.fxxkopenai.com/ (origin http://129.151.21.101/)"
