#!/usr/bin/env bash
# Usage: [SSH_BIND_INTERFACE=en0] [HOST=user@host] deploy/deploy.sh <ssh-key> [apk]
# 站点地址和 SSH 主机写在 deploy/private/site.env（不进 git）：SITE_URL、SITE_FALLBACK_URL、SSH_HOST
# Ships the sync server, landing page and (optionally) a new APK to the Ubuntu host.
set -euo pipefail
KEY=${1:?ssh key}
APK=${2:-}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SITE_ENV="$ROOT/deploy/private/site.env"
[[ -f "$SITE_ENV" ]] || { echo "缺少 $SITE_ENV" >&2; exit 1; }
env_get() { grep -E "^$1=" "$SITE_ENV" | head -1 | cut -d= -f2- | tr -d '[:space:]'; }
SITE_URL=$(env_get SITE_URL); SITE_URL=${SITE_URL%/}
SITE_HOST=${SITE_URL#*://}
ORIGIN_HOST=$(env_get SITE_FALLBACK_URL); ORIGIN_HOST=${ORIGIN_HOST#*://}; ORIGIN_HOST=${ORIGIN_HOST%%/*}
HOST=${HOST:-$(env_get SSH_HOST)}
[[ -n "$SITE_HOST" && -n "$HOST" ]] || { echo "$SITE_ENV 里缺 SITE_URL 或 SSH_HOST" >&2; exit 1; }
SSH_ARGS=(-i "$KEY" -o StrictHostKeyChecking=accept-new)
if [[ -n "${SSH_BIND_INTERFACE:-}" ]]; then
  SSH_ARGS+=(-o "BindInterface=$SSH_BIND_INTERFACE")
fi

STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT
mkdir -p "$STAGE/app" "$STAGE/web/download"
cp "$ROOT/server/app.py" "$ROOT/server/requirements.txt" "$STAGE/app/"
cp -R "$ROOT/server/web/." "$STAGE/web/"
cp "$ROOT/deploy/nuanyue.service" "$ROOT/deploy/nuanyue-backup.cron" "$STAGE/"
# nginx 配置是模板，部署时填入域名和源站地址
sed -e "s/__SITE_HOST__/$SITE_HOST/g" -e "s/__ORIGIN_HOST__/$ORIGIN_HOST/g" "$ROOT/deploy/nginx-nuanyue.conf" > "$STAGE/nginx-nuanyue.conf"
if [[ -n "$APK" ]]; then
  cp "$APK" "$STAGE/web/download/nuanyue.apk"
  VERSION_CODE=$(grep -E 'versionCode *=' "$ROOT/android/app/build.gradle.kts" | grep -oE '[0-9]+')
  VERSION_NAME=$(grep -E 'versionName *=' "$ROOT/android/app/build.gradle.kts" | grep -oE '"[^"]+"' | tr -d '"')
  SIZE=$(du -m "$APK" | cut -f1)
  SHA=$(sha256sum "$APK" | awk '{print $1}')
  cat > "$STAGE/web/version.json" <<JSON
{"versionCode": $VERSION_CODE, "versionName": "$VERSION_NAME", "url": "$SITE_URL/", "apkUrl": "$SITE_URL/download/nuanyue.apk", "sha256": "$SHA", "size": "${SIZE}MB", "notes": ""}
JSON
fi

COPYFILE_DISABLE=1 tar --no-xattrs -C "$STAGE" -czf "$STAGE.tgz" .
scp "${SSH_ARGS[@]}" "$STAGE.tgz" "$HOST:/tmp/nuanyue.tgz"
rm -f "$STAGE.tgz"

ssh "${SSH_ARGS[@]}" "$HOST" 'bash -s' <<'REMOTE'
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
# 整份覆盖会冲掉吃了吗加的路由和源站防护，装了吃了吗就补回来
if [[ -x /opt/todayeat/patch_nginx.py ]]; then
  sudo python3 /opt/todayeat/patch_nginx.py --backup-dir "$HOME/backup" --no-reload
fi
sudo nginx -t -q
sudo systemctl enable -q nginx
sudo systemctl reload nginx || sudo systemctl restart nginx
sleep 2
curl -fsS http://127.0.0.1/api/health && echo
systemctl is-active nuanyue nginx
REMOTE
echo "deployed → $SITE_URL/"
