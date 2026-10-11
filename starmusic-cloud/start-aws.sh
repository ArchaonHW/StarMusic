#!/usr/bin/env bash
# StarMusic 單體版啟動腳本（AWS/單機部署）
# 前置：JDK 17+，同目錄下有 starmusic-app/target/starmusic-app-1.0.0.jar 與 web/（前端 dist）
# 使用：./start-aws.sh
cd "$(dirname "$0")"

export STARMUSIC_JWT_SECRET="${STARMUSIC_JWT_SECRET:-$(head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')}"
export STARMUSIC_ADMIN_PASSWORD="${STARMUSIC_ADMIN_PASSWORD:-$(head -c 12 /dev/urandom | base64 | tr -d '/+=')}"
export STARMUSIC_DEMO_PASSWORD="${STARMUSIC_DEMO_PASSWORD:-$(head -c 12 /dev/urandom | base64 | tr -d '/+=')}"

# 第一次啟動時把產生的密碼印出來（種子帳號只建一次，之後改密碼要用資料庫或重建）
if [ ! -f data/starmusicdb.mv.db ]; then
  echo "FIRST RUN — seed passwords:"
  echo "  admin   / $STARMUSIC_ADMIN_PASSWORD"
  echo "  starfan / $STARMUSIC_DEMO_PASSWORD"
  echo "（請記下；之後啟動可 export STARMUSIC_ADMIN_PASSWORD=... 固定）"
fi

exec java -Xmx512m -jar starmusic-app/target/starmusic-app-1.0.0.jar \
  --spring.web.resources.static-locations="file:./web/"
