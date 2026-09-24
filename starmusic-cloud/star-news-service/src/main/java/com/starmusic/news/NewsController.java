package com.starmusic.news;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    public record Article(long id, String title, String category, String summary,
                          String content, String source, String author,
                          String imageUrl, List<String> imageUrls,
                          String publishedAt, boolean breaking) {

        public Article(long id, String title, String category, String summary,
                       String content, String source, String author,
                       String imageUrl, String publishedAt, boolean breaking) {
            this(id, title, category, summary, content, source, author,
                    imageUrl, List.of(), publishedAt, breaking);
        }
    }

    private static final List<Article> ARTICLES = List.of(
            new Article(1, "星光全球娛樂台年度演唱會官宣 12/31 台北小巨蛋開唱", "娛樂",
                    "星光全球娛樂台今（24）日宣布，年度壓軸演唱會《星光之夜》將於跨年夜登場，卡司囊括華語樂壇天王天后。",
                    "星光全球娛樂台今日正式宣布，年度壓軸演唱會《星光之夜》將於 12 月 31 日晚間在台北小巨蛋開唱。主辦單位透露，本次卡司囊括華語樂壇天王天后，並將透過星光全球娛樂台全平台同步直播，預計吸引全球超過千萬觀眾收看。",
                    "星光全球娛樂台", "記者林小星", "/assets/news/concert.jpg",
                    "2026-09-24T09:00:00", true),
            new Article(2, "《乘風2026》總決賽收視創新高 成團名單出爐", "影視",
                    "選秀節目《乘風2026》總決賽收視率突破 5.2，七人女團名單正式公布。",
                    "選秀節目《乘風2026》昨晚迎來總決賽，收視率突破 5.2 創下本季新高。經過三個月的激烈角逐，最終七人成團名單正式公布，成團夜微博話題閱讀量突破 30 億。",
                    "星光全球娛樂台", "記者陳星光", "/assets/news/show.jpg",
                    "2026-09-23T22:30:00", true),
            new Article(3, "亞洲電台大賞揭曉 星光流行網蟬聯年度最佳電台", "音樂",
                    "亞洲電台大賞昨日頒獎，星光流行網連續第三年獲得年度最佳音樂電台殊榮。",
                    "亞洲電台大賞頒獎典禮昨日於新加坡舉行，星光流行網連續第三年獲得「年度最佳音樂電台」殊榮，DJ Stella 同時拿下最受歡迎主持人獎。",
                    "星光全球娛樂台", "記者王都會", "/assets/news/radio-award.jpg",
                    "2026-09-23T18:00:00", false),
            new Article(4, "串流平台大戰開打 獨家內容成決勝關鍵", "科技",
                    "各大串流平台紛紛加碼原創內容投資，分析師預估明年市場規模將突破千億。",
                    "隨著串流平台競爭白熱化，各平台紛紛加碼原創內容投資。市場分析師預估，亞太區串流市場規模明年將突破千億美元，獨家戲劇與綜藝內容成為決勝關鍵。",
                    "星光全球娛樂台", "記者李科技", "/assets/news/streaming.jpg",
                    "2026-09-22T14:20:00", false),
            new Article(5, "《長街長》完結篇網友淚崩 續集確認明年開拍", "影視",
                    "古裝大戲《長街長》播出完結篇，製作方同步宣布續集將於明年開拍。",
                    "古裝權謀大戲《長街長》昨晚播出完結篇，結局反轉讓網友淚崩。製作方同步宣布續集《長街長·歸途》將於明年春季開拍，原班人馬回歸。",
                    "星光全球娛樂台", "記者張戲劇", "/assets/news/drama.jpg",
                    "2026-09-22T23:00:00", false),
            new Article(6, "秋季美妝趨勢：星光妝容席捲時尚圈", "生活",
                    "今年秋季美妝趨勢以閃耀星光元素為主軸，各大品牌推出限定眼影盤。",
                    "今年秋季美妝趨勢以閃耀星光元素為主軸，各大品牌紛紛推出限定眼影盤與高光產品。時尚雜誌指出，帶有細緻珠光的香檳金色系將成為本季主流。",
                    "星光全球娛樂台", "記者蘇時尚", "/assets/news/beauty.jpg",
                    "2026-09-21T11:00:00", false),
            new Article(7, "虛擬偶像演唱會票房破億 AI 歌手成新藍海", "科技",
                    "虛擬偶像團體「星塵少女」首場全息演唱會票房突破一億元，AI 歌手市場備受關注。",
                    "虛擬偶像團體「星塵少女」首場全息投影演唱會票房突破一億元，創下虛擬演出新紀錄。業界認為 AI 歌手與虛擬偶像將成為娛樂產業下一波藍海市場。",
                    "星光全球娛樂台", "記者李科技", "/assets/news/virtual-idol.jpg",
                    "2026-09-20T16:45:00", false),
            new Article(8, "星光電子雜誌十週年 推出數位典藏版", "娛樂",
                    "星光電子雜誌歡慶十週年，推出收錄歷年經典封面的數位典藏版供訂戶免費下載。",
                    "星光電子雜誌歡慶創刊十週年，宣布推出收錄歷年 120 期經典封面的數位典藏版，訂戶可免費下載收藏，並同步推出十週年紀念專刊。",
                    "星光全球娛樂台", "記者林小星", "/assets/news/magazine.jpg",
                    "2026-09-19T10:00:00", false),
            new Article(9, "海饌宴會館盛大開幕 打造板橋餐飲宴會新地標", "活動",
                    "「海饌宴會館」9月21日於新北市板橋區盛大開幕，占地550坪、設10間獨立包廂及大型宴會空間，提供桌菜、港式點心、下午茶與歡唱服務。",
                    """
                    【新北市訊】位於新北市板橋區重慶路247號2樓的「海饌宴會館」，於民國115年9月21日中午11時舉行開幕典禮。剪綵貴賓包括國際獅子會300複合區國際理事邱銘乾博士、300B 2區前總監徐淑珍、新北市議員曾煥嘉、板橋廣德里里長周建和及板橋振興里里長柯仁傑，與各界嘉賓共同見證海饌正式啟航。

                    海饌宴會館占地約550坪，設有10間獨立包廂及大型宴會空間，提供桌菜、單點料理、港式點心、下午茶及歡唱服務，適合婚宴、壽宴、公司聚餐、社團會議與各類活動。

                    運營總監林均霖教授受訪表示：「海饌不只是一間餐廳，更是一個讓親友相聚、社團交流及公益活動都能自在舉辦的平台。我們將以美食連結人情、以歌聲傳遞歡樂，用真誠服務每一位顧客，讓海饌成為板橋充滿溫度的新地標。」

                    未來，海饌宴會館將持續結合餐飲、宴會、歌唱、藝文及公益活動，為民眾打造多元、歡樂且溫馨的聚會空間。

                    地址：新北市板橋區重慶路247號2樓
                    訂位專線：02-8952-6168
                    LINE ID：@616mrucc
                    """,
                    "星光全球娛樂台", "記者綜合報導", "/media/haizan-opening-1.jpg",
                    List.of("/media/haizan-opening-1.jpg", "/media/haizan-opening-2.jpg",
                            "/media/haizan-opening-3.jpg", "/media/haizan-opening-4.jpg"),
                    "2026-09-24T10:30:00", false)
    );

    @GetMapping
    public List<Article> list(@RequestParam(required = false) String category) {
        return ARTICLES.stream()
                .filter(a -> category == null || a.category().equals(category))
                .sorted(Comparator.comparing(Article::publishedAt).reversed())
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> get(@PathVariable long id) {
        return ARTICLES.stream()
                .filter(a -> a.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/breaking")
    public List<Article> breaking() {
        return ARTICLES.stream().filter(Article::breaking).toList();
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return ARTICLES.stream().map(Article::category).distinct().toList();
    }
}
