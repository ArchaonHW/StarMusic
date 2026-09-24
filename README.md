# StarMusic 星光全球娛樂台

廣播、新聞、TV、雜誌、購物、會員 — 娛樂入口網站雛形。

- 後端：Spring Boot 3.3 + Spring Cloud 2023（Eureka + Gateway + 七個微服務）
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
│   └── star-search-service       # 全站搜尋聚合         :8087 /api/search
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
| `/`         | 首頁：Hero、快訊跑馬燈、各頻道精選    |
| `/videos`   | 影視：分類篩選 + 觀看排行             |
| `/radio`    | 電台：頻道列表、節目表、播放按鈕 stub |
| `/news`     | 新聞：分類篩選、快訊標籤              |
| `/news/:id` | 新聞詳情                              |
| `/magazines`| 雜誌：封面牆 + 單期預覽彈窗           |
| `/shop`     | 購物：商品列表、加入購物車            |
| `/cart`     | 購物車：刪除品項、結帳產生訂單        |
| `/member`   | 會員：註冊 / 登入 / 個人資料          |
| `/search`   | 搜尋：跨服務關鍵字搜尋結果（?q=）     |

## 測試帳號

- 帳號：`starfan` / 密碼：`123456`（會員服務內建 demo 帳號）

## 備註

- 目前為雛形：資料皆為各服務記憶體中的種子資料，重啟即重置；尚未接資料庫、串流與金流。
- 後續可加入：Spring Cloud Config、Resilience4j 熔斷、Spring Security + JWT、MySQL/Redis、OpenFeign 服務間呼叫。
