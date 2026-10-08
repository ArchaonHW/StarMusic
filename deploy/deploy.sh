#!/usr/bin/env bash
# StarMusic 一鍵部署 — Amazon Linux 2023 / RHEL 系
# 用法: 在伺服器上 clone 本 repo,然後以 root 執行:
#   sudo bash deploy/deploy.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR=/opt/starmusic
SERVICES=(star-eureka-server star-gateway star-video-service star-radio-service
          star-news-service star-magazine-service star-shop-service
          star-member-service star-search-service star-post-service)

echo "==> [1/6] 安裝系統套件"
dnf install -y java-17-amazon-corretto-devel maven nginx git
dnf install -y nodejs20 2>/dev/null || dnf install -y nodejs

echo "==> [2/6] 建置後端 jars"
cd "$REPO_ROOT/starmusic-cloud"
mvn -DskipTests package
mkdir -p "$APP_DIR/jars"
for s in "${SERVICES[@]}"; do
    cp "$s/target/$s-1.0.0.jar" "$APP_DIR/jars/"
done

echo "==> [3/6] 建置前端(關閉 staticData,走真實後端)"
cd "$REPO_ROOT/starmusic-web"
sed -i 's/staticData: true/staticData: false/' src/environments/environment.prod.ts
npm ci
npm run build
mkdir -p "$APP_DIR/web"
cp -r dist/starmusic-web/browser/* "$APP_DIR/web/" 2>/dev/null \
    || cp -r dist/starmusic-web/* "$APP_DIR/web/"

echo "==> [4/6] 建立帳號與目錄"
id starmusic &>/dev/null || useradd -r -s /sbin/nologin starmusic
for s in "${SERVICES[@]}"; do mkdir -p "$APP_DIR/run/$s"; done
chown -R starmusic:starmusic "$APP_DIR"

echo "==> [5/6] 安裝環境檔 / systemd units / nginx 設定"
if [[ ! -f "$APP_DIR/starmusic.env" ]]; then
    cp "$REPO_ROOT/deploy/starmusic.env.example" "$APP_DIR/starmusic.env"
    sed -i "s/^STARMUSIC_JWT_SECRET=$/STARMUSIC_JWT_SECRET=$(openssl rand -hex 32)/" "$APP_DIR/starmusic.env"
    echo "    已產生隨機 JWT secret;請編輯 $APP_DIR/starmusic.env 補齊密碼與網域"
fi
chmod 600 "$APP_DIR/starmusic.env"; chown starmusic:starmusic "$APP_DIR/starmusic.env"
cp "$REPO_ROOT/deploy/starmusic@.service" /etc/systemd/system/
cp "$REPO_ROOT/deploy/nginx-starmusic.conf" /etc/nginx/conf.d/starmusic.conf
systemctl daemon-reload

echo "==> [6/6] 啟動服務(Eureka 先行)"
systemctl enable --now starmusic@star-eureka-server
for i in $(seq 1 60); do
    curl -sf http://127.0.0.1:8761 >/dev/null && break || sleep 2
done
for s in "${SERVICES[@]:1}"; do systemctl enable --now "starmusic@$s"; done
nginx -t && systemctl enable --now nginx && systemctl reload nginx

echo ""
echo "完成。檢查: curl http://127.0.0.1:8761 (Eureka) / curl http://127.0.0.1/api/news (Gateway)"
echo "日誌:  journalctl -u starmusic@star-gateway -f"
