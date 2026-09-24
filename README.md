# StarMusic 星光全球娛樂台

廣播、新聞、TV、雜誌、購物、會員 — 娛樂入口網站雛形。

- 後端：Spring Boot 3.5 + Spring Cloud 2025（Eureka + Gateway + 九個微服務，search-service 以 OpenFeign 聚合）
- 資料庫：H2（檔案型持久化，預設）／MySQL（`--spring.profiles.active=mysql` 切換，見下文）
- 認證：JWT（HS256，`STARMUSIC_JWT_SECRET`）＋ 舊版 `star-*` token 相容
- 前端：Angular 20（Standalone Components + Signals + SCSS）

## 專案結構

```
StarMusic/
├── starmusic-cloud/              # Maven 多模組後端
│   ├── star-eureka-server        # 服務註冊中心          :8761
│   ├── star-gateway              # API 閘道（路由/CORS） :8080
│   ├── star-video-service        # 影視（參考芒果TV）    :8081 /api/videos
│   ├── star-radio-service        # 電台（參考亞洲電台）  :8082 /api/radio
│   ├── star-news-service         # 新聞（參考今日新聞）  :8083 /api/news
│   ├── star-magazine-service     # 電子雜誌             :8084 /api/magazines
│   ├── star-shop-service         # 購物/購物車/訂單     :8085 /api/products|cart|orders
│   ├── star-member-service       # 會員註冊/登入        :8086 /api/members
│   ├── star-search-service       # 全站搜尋聚合         :8087 /api/search
│   └── star-post-service         # 會員投稿（審核制）   :8088 /api/posts
└── starmusic-web/                # Angular 前端          :4200
```

## 啟動方式

### 1. 後端（需 JDK 17+ 與 Maven）

```bash
cd starmusic-cloud
mvn -DskipTests package

# 依序啟動（每個指令一個終端視窗）
mvn -pl star-eureka-server spring-boot:run        # 先啟註冊中心
mvn -pl star-gateway spring-boot:run              # API 閘道
mvn -pl star-video-service spring-boot:run
mvn -pl star-radio-service spring-boot:run
mvn -pl star-news-service spring-boot:run
mvn -pl star-magazine-service spring-boot:run
mvn -pl star-shop-service spring-boot:run
mvn -pl star-member-service spring-boot:run
mvn -pl star-search-service spring-boot:run
mvn -pl star-post-service spring-boot:run
```

- Eureka 面板：http://localhost:8761
- API 入口（閘道）：http://localhost:8080/api/...

### 2. 前端（需 Node 20+）

```bash
cd starmusic-web
npm install
npm start          # http://localhost:4200
```

## 頁面對應

| 路由        | 內容                                  |
| ----------- | ------------------------------------- |
| `/`         | 首頁：精選輪播、快訊跑馬燈、各頻道精選 |
| `/videos`   | 影視（芒果TV式）：精選輪播 + 頻道樓層 + VIP專區 + 分類排行 |
| `/videos/:id` | 影片播放頁（串流播放 + 相關推薦）   |
| `/radio`    | 電台：頻道列表、節目表、播放列（合成音訊串流、音量、現正播出標示） |
| `/news`     | 新聞：分類篩選、快訊標籤              |
| `/news/:id` | 新聞詳情                              |
| `/magazines`| 雜誌：封面牆 + 單期預覽彈窗           |
| `/shop`     | 購物：商品列表、加入購物車            |
| `/cart`     | 購物車：刪除品項、結帳產生訂單        |
| `/member`   | 會員：註冊 / 登入 / 個人資料 / 帳務明細 / 我的收藏 / 觀看紀錄 |
| `/posts`    | 會員投稿：影片/音訊/圖片/文章牆 + 投稿表單 + 我的投稿管理（下架/重送/刪除） |
| `/posts/:id`| 投稿詳情：按讚、留言、管理操作      |
| `/admin`    | 管理後台（僅管理員）：待審佇列、上下架、內容管理（新聞/雜誌/商品）、會員帳務 |
| `/search`   | 搜尋：跨服務關鍵字搜尋結果（?q=）     |

## 測試帳號

| 帳號 | 密碼 | 角色 | 說明 |
| --- | --- | --- | --- |
| `starfan` | `STARMUSIC_DEMO_PASSWORD` 環境變數 | MEMBER | demo 會員（內建餘額與帳務紀錄） |
| `admin` | `STARMUSIC_ADMIN_PASSWORD` 環境變數 | ADMIN | 管理員：審核影片與投稿、調整會員帳務、查看全部會員 |

> 未設定環境變數時，種子密碼為啟動日誌中自動產生的隨機值（搜尋 `[DEV]` 字樣）。

## 會員上傳與審核流程

### 影片上傳（video-service）

1. 會員登入後到「影視」頁 → 展開「上傳影片」→ 填標題/分類/簡介並選擇影片檔 → 送出（狀態：待審核）
2. 管理員（admin）登入後到「會員中心」→「待審核影片」→ 通過上架或退回（可填備註）
3. 審核通過的影片自動出現在影視列表，可線上播放（檔案由 video-service 串流，支援 Range）

### 會員投稿（post-service，審核制）

1. 會員登入後到「投稿」頁 → 展開「發表新內容」→ 選擇類型（影片 / 音訊 / 圖片 / 文章）填表送出（狀態：待審核）
   - 影片：mp4 / m4v / mov / webm / mkv；音訊：mp3 / wav / ogg / m4a / flac；圖片：jpg / png / gif / webp / avif
   - 文章類型以內文為必填，檔案可選（作為封面圖）；其餘類型檔案為必填
2. 管理員在「會員中心」→「待審核投稿」通過或退回（可填備註）
3. 通過的投稿出現在「投稿」牆與全站搜尋結果；會員可在投稿頁看到自己的投稿狀態與退回原因

帳務：會員中心顯示餘額與交易明細；管理員可對任一會員儲值/扣款（寫入 `account_transactions` 並更新餘額）。

- H2 資料檔：`star-member-service/data/`（members、account_transactions、auth_tokens）、`star-video-service/data/`（video_uploads、收藏/觀看紀錄/評論）、`star-post-service/data/`（posts、post_likes、post_comments）、`star-magazine-service`、`star-news-service`、`star-shop-service`（JPA 持久化）
- 上傳檔案存放：`star-video-service/uploads/`、`star-post-service/uploads/`，分別經 `/api/videos/files/{檔名}`、`/api/posts/files/{檔名}` 存取；未上架檔案僅上傳者本人與管理員可讀取

### 互動功能

- 影片：播放頁收藏（❤）、留言（本人/管理員可刪）、觀看紀錄自動寫入；會員中心顯示「我的收藏」「觀看紀錄」
- 投稿：詳情頁按讚、留言；上架後作者可自行下架/重送/刪除
- 電台：每個頻道提供 `/api/radio/streams/{id}` 合成 WAV 串流，前端播放列可循環收聽

## 資料庫

- 預設為 H2 檔案型資料庫，免安裝即可啟動
- member / video / post 三服務提供 `mysql` profile：

```bash
mvn -pl star-member-service spring-boot:run -Dspring-boot.run.profiles=mysql
# 以環境變數設定連線：MYSQL_HOST、MYSQL_PORT、MYSQL_USER、MYSQL_PASSWORD
```

## 認證與權限

- 登入後 member-service 簽發 JWT（HS256，7 天效期，簽章密鑰 `STARMUSIC_JWT_SECRET`）；前端所有需身分的請求只帶 `Authorization: Bearer` 標頭
- Gateway 的 `AuthRelayFilter` 會剔除客戶端偽造的 `X-User-*` 標頭：JWT 在閘道本地驗簽後直接注入 `X-User-Id` / `X-User-Name` / `X-User-Role`；舊版 `star-*` token 則 fallback 向 member-service `/api/members/me` 換取身分（`X-User-Name` 為 URL-encoded UTF-8，支援中文帳號，下游使用時需 `URLDecoder.decode`）
- 密碼以 BCrypt 雜湊儲存；登入失敗 5 次/5 分鐘會被限流
- 種子帳號密碼由環境變數決定：`STARMUSIC_ADMIN_PASSWORD`、`STARMUSIC_DEMO_PASSWORD`（未設定時自動產生隨機密碼並印在啟動日誌）
- 資料庫密碼：`STARMUSIC_DB_PASSWORD`（預設空）

## 備註

- 目前為雛形：影視目錄、電台節目表為記憶體種子資料；會員、帳務、影片上傳、會員投稿、新聞、雜誌、商品已接 H2 資料庫持久化。
- search-service 透過 OpenFeign 呼叫各微服務聚合搜尋結果；單一服務離線時該類結果自動降級為空。
- 後續可加入：Spring Cloud Config、Resilience4j 熔斷、Redis 快取。
