# StarMusic 部署

單台 EC2(或任何 RHEL 系主機)部署:nginx 提供前端靜態檔並反代 `/api` 到
star-gateway,10 個 Spring Boot 服務以 systemd 執行。

## 需求

- Amazon Linux 2023(或相容 dnf 的系統),**建議 t3.large(8 GB)以上**
- Security Group:22、80、443 對外;8080/8761/8081-8088 保持內網
- root 或 sudo 權限

## 步驟

```bash
git clone https://github.com/ArchaonHW/StarMusic.git
cd StarMusic
sudo bash deploy/deploy.sh
```

腳本會:安裝 JDK 17 / Maven / Node / nginx → 建置 jars 與前端
(自動把 `staticData` 關閉改走真實後端)→ 建立 `starmusic` 使用者與
`/opt/starmusic/{jars,web,run}` → 安裝 systemd unit 與 nginx 設定 →
依序啟動(Eureka 先就緒再啟其餘)。

## 部署後必做

1. 編輯 `/opt/starmusic/starmusic.env`:補 `STARMUSIC_ADMIN_PASSWORD`、
   `STARMUSIC_DEMO_PASSWORD`、`STARMUSIC_ALLOWED_ORIGINS`(你的網域)。
   JWT secret 已由腳本隨機產生。
   ```bash
   sudo systemctl restart 'starmusic@*'
   ```
2. 網域 DNS 指到主機,nginx `server_name` 改成實際網域。
3. HTTPS:`sudo dnf install -y certbot python3-certbot-nginx` 然後
   `sudo certbot --nginx -d your-domain.example`。

## 檔案

| 檔案 | 用途 |
|---|---|
| `deploy.sh` | 一鍵部署(建置 + 安裝 + 啟動) |
| `starmusic@.service` | systemd template unit,`starmusic@<模組名>` 即一個服務 |
| `starmusic.env.example` | 環境變數範本 → `/opt/starmusic/starmusic.env` |
| `nginx-starmusic.conf` | 前端靜態檔 + `/api` 反代 |

## 常用維運指令

```bash
systemctl status 'starmusic@*'          # 全部服務狀態
journalctl -u starmusic@star-member-service -f   # 看某服務日誌
systemctl restart 'starmusic@*'         # 全部重啟
# 重新部署: 在 repo 目錄 git pull 後重跑 deploy.sh
```

## 改用 MySQL

member / video / post 三服務提供 `mysql` profile。在 `starmusic.env`
取消註解 `SPRING_PROFILES_ACTIVE=mysql` 與 `MYSQL_*` 變數(建議搭配
RDS),重啟對應服務即可;其餘服務維持 H2。
