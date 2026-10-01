package com.starmusic.magazine;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/magazines")
public class MagazineController {

    public record Magazine(long id, String title, String issueNo, String cover,
                           String publishDate, double price, String category,
                           String coverStory, List<String> highlights, boolean latest,
                           String readUrl) {
    }

    // 免費線上雜誌：台北畫刊（北市府觀傳局）、台灣光華雜誌、PAR表演藝術、新活水
    private static final List<Magazine> MAGAZINES = List.of(
            new Magazine(1, "台北畫刊", "第687期", "https://cdn.readmoo.com/cover/li/f9neqml.jpg?v=1784537995",
                    "2026-07-17", 0, "城市",
                    "台北河・夜太美",
                    List.of("當河岸點亮城市的另一種風景", "大稻埕的人間煙火氣｜河岸遊憩",
                            "彩虹橋畔的浪漫夜色｜河岸食光", "來去環南市場：從消夜吃到早餐",
                            "亞洲戲偶權威 Robin Ruizendaal 的偶戲人生", "臺北親水節 High 翻城市水樂園"),
                    true, "https://www.travel.taipei/zh-tw/pictorial/period/376"),
            new Magazine(2, "台北畫刊", "第686期", "https://cdn.readmoo.com/cover/iq/cfgkpig.jpg?v=1777944733",
                    "2026-05-05", 0, "城市",
                    "翻讀漫畫：探索書頁裡的台北",
                    List.of("台灣漫畫發展脈絡與台北的漫畫沃土", "漫畫場景拾影：大稻埕、西門町的時空旅行",
                            "漫畫空間巡禮：通往想像的文化路徑", "白鹿洞書坊與懷舊租書店文化"),
                    true, "https://news.travel.taipei/ebook/686/"),
            new Magazine(3, "台灣光華雜誌 Taiwan Panorama", "2026年10月號",
                    "https://www.taiwan-panorama.com/s3/tw-panorama.com/Uploads/%E6%9C%9F%E5%88%8A%E5%B0%81%E9%9D%A2/a1675a11-7979-42cd-b095-d303f2ce704c/000-000-11510-%E5%B0%81%E9%9D%A2%20rt%20a.jpg",
                    "2026-10", 0, "綜合",
                    "台灣面向世界的雙語月刊",
                    List.of("百年文藝群星登陸東京：《共時的星叢》於東京都現代美術館開展",
                            "在北投遇見日本妖怪：一場台日的文化轉譯",
                            "台中綠美圖的空間實驗：普利茲克獎團隊 SANAA 打造",
                            "全台灣最瘋狂排球賽：彰化二林「ㄓ豆大叔盃水田排球賽」"),
                    true, "https://www.taiwan-panorama.com/zh-tw/Periodical/Details?Guid=a1675a11-7979-42cd-b095-d303f2ce704c"),
            new Magazine(4, "PAR表演藝術", "最新刊", "https://par.npac-ntch.org/rsrc/latestMag.png",
                    "持續更新", 0, "表演藝術",
                    "國家表演藝術中心官方雜誌",
                    List.of("音樂・舞蹈・戲劇・戲曲專題與評論", "藝術家與製作團隊深度專訪",
                            "兩廳院、衛武營、台中歌劇院演出情報"),
                    true, "https://par.npac-ntch.org/"),
            new Magazine(5, "高雄畫刊", "No.10", "https://takao-aws.hmgcdn.com/files/periodical/p_431_20261001075708_675.jpg",
                    "2026-10", 0, "城市",
                    "高雄．夜正精彩",
                    List.of("尋覓美味・探索高雄・藝術文化・高雄人物", "養蜂新世代：8年級女孩在梓官釀出海線蜜香",
                            "百貨頂樓的夜：換城市玩家上場", "大港連線與港都萬花筒"),
                    true, "https://takao.kcg.gov.tw/issue/article/431"),
            new Magazine(7, "桃園誌", "第110期", "https://cdn.readmoo.com/cover/jf/iejerhl.jpg?v=1777944376",
                    "2026-05-04", 0, "城市",
                    "晴時多雲偶陣雨",
                    List.of("以對話連結城市的桃園在地刊物", "人物專訪與桃園生活記事",
                            "桃園市政府新聞處發行"),
                    true, "https://readmoo.com/book/220470619000101"),
            new Magazine(8, "漾台中", "NO.43 2026年6月號", "https://cdn.readmoo.com/cover/fj/ecbdmla.jpg?v=1781583462",
                    "2026-06", 0, "城市",
                    "城市新動脈：串起山海屯都",
                    List.of("交通建設專題：捷運、橋梁與轉運系統", "好漾企劃：陪孩子一起長大的城市",
                            "城市食光：重新認識素食的可能", "人物印象：李權洋導演與霧農大叔"),
                    true, "https://readmoo.com/book/220480935000101"),
            new Magazine(6, "台北畫刊", "第675期", "https://cdn.readmoo.com/cover/a6/3cedi68.jpg?v=1720411539",
                    "2024-07-08", 0, "城市",
                    "我城焦點：台北百年摩登",
                    List.of("建城140週年：看向未來的文化首都", "尋找歷史座標，開啟時代的建築之最",
                            "看見摩登時尚：多元交織的服裝風華", "漫遊花火下的大稻埕",
                            "走訪天下第一攤", "百年流轉的城市光影"),
                    false, "https://news.travel.taipei/ebook/675/")
    );

    public record MagazineRequest(String title, String issueNo, String cover,
                                  String publishDate, Double price, String category,
                                  String coverStory, List<String> highlights,
                                  Boolean latest, String readUrl) {
    }

    private static final long MANAGED_ID_BASE = 10000;
    private static final int MAX_TITLE = 200;
    private static final int MAX_SHORT = 100;
    private static final int MAX_TEXT = 2000;

    private final MagazineRepository magazines;

    public MagazineController(MagazineRepository magazines) {
        this.magazines = magazines;
    }

    @GetMapping
    public List<Magazine> list(@RequestParam(required = false) String category) {
        return all()
                .filter(m -> category == null || m.category().equals(category))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Magazine> get(@PathVariable long id) {
        return all()
                .filter(m -> m.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public List<Magazine> latest() {
        return all().filter(Magazine::latest).toList();
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return all().map(Magazine::category).distinct().toList();
    }

    @GetMapping("/manage")
    public List<Magazine> managed(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return magazines.findAll().stream()
                .sorted(Comparator.comparing(MagazineEntity::getId).reversed())
                .map(this::fromEntity)
                .toList();
    }

    @PostMapping("/manage")
    public Magazine create(@RequestBody(required = false) MagazineRequest req,
                           @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        MagazineEntity e = new MagazineEntity();
        apply(e, req);
        return fromEntity(magazines.save(e));
    }

    @PutMapping("/manage/{id}")
    public Magazine update(@PathVariable long id,
                           @RequestBody(required = false) MagazineRequest req,
                           @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        MagazineEntity e = magazines.findById(id - MANAGED_ID_BASE).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "內容不存在或為內建資料"));
        apply(e, req);
        return fromEntity(magazines.save(e));
    }

    private void apply(MagazineEntity e, MagazineRequest req) {
        if (req == null || req.title() == null || req.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題必填");
        }
        if (req.title().length() > MAX_TITLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題過長");
        }
        checkLen(req.issueNo(), MAX_SHORT, "期號");
        checkLen(req.publishDate(), MAX_SHORT, "出版日期");
        checkLen(req.category(), MAX_SHORT, "分類");
        checkLen(req.coverStory(), MAX_TEXT, "封面故事");

        e.setTitle(req.title().trim());
        e.setIssueNo(req.issueNo());
        e.setCover(req.cover());
        e.setPublishDate(req.publishDate());
        e.setPrice(req.price() == null ? 0 : req.price());
        e.setCategory(req.category());
        e.setCoverStory(req.coverStory());
        e.setHighlights(req.highlights() == null ? null : String.join("\n", req.highlights()));
        e.setLatest(Boolean.TRUE.equals(req.latest()));
        e.setReadUrl(req.readUrl());
    }

    @DeleteMapping("/manage/{id}")
    public ResponseEntity<Void> deleteManaged(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        long dbId = id - MANAGED_ID_BASE;
        if (!magazines.existsById(dbId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "內容不存在或為內建資料");
        }
        magazines.deleteById(dbId);
        return ResponseEntity.noContent().build();
    }

    private Stream<Magazine> all() {
        return Stream.concat(
                MAGAZINES.stream(),
                magazines.findAll().stream().map(this::fromEntity));
    }

    private Magazine fromEntity(MagazineEntity e) {
        List<String> highlights = e.getHighlights() == null || e.getHighlights().isBlank()
                ? List.of()
                : List.of(e.getHighlights().split("\\R"));
        return new Magazine(MANAGED_ID_BASE + e.getId(), e.getTitle(), e.getIssueNo(),
                e.getCover(), e.getPublishDate(), e.getPrice(), e.getCategory(),
                e.getCoverStory(), highlights, e.isLatest(), e.getReadUrl());
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
    }

    private void checkLen(String v, int max, String label) {
        if (v != null && v.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "過長");
        }
    }
}
