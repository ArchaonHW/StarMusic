package com.starmusic.magazine;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
                           String coverStory, List<String> highlights, boolean latest) {
    }

    private static final List<Magazine> MAGAZINES = List.of(
            new Magazine(1, "星光娛樂週刊", "NO.521", "/assets/magazines/weekly-521.jpg",
                    "2026-09-22", 99, "娛樂",
                    "封面故事：乘風2026 成團之夜全紀錄",
                    List.of("獨家專訪成團七人", "演唱會幕後直擊", "秋季穿搭特輯"), true),
            new Magazine(2, "星光娛樂週刊", "NO.520", "/assets/magazines/weekly-520.jpg",
                    "2026-09-15", 99, "娛樂",
                    "封面故事：長街長完結篇深度解析",
                    List.of("導演獨家訪談", "權謀劇十大盘點", "古裝造型大賞"), false),
            new Magazine(3, "時尚星光", "2026 秋季號", "/assets/magazines/fashion-autumn.jpg",
                    "2026-09-01", 149, "時尚",
                    "封面故事：星光妝容的誕生",
                    List.of("秋冬時裝週前線", "香檳金妝容教學", "名模衣櫥公開"), true),
            new Magazine(4, "音樂先鋒", "VOL.88", "/assets/magazines/music-88.jpg",
                    "2026-09-10", 79, "音樂",
                    "封面故事：AI 歌手會取代真人嗎？",
                    List.of("虛擬偶像產業報告", "華語樂壇新聲代", "錄音室探秘"), true),
            new Magazine(5, "音樂先鋒", "VOL.87", "/assets/magazines/music-87.jpg",
                    "2026-08-10", 79, "音樂",
                    "封面故事：亞洲電台大賞完全特輯",
                    List.of("年度最佳電台專訪", "廣播的黃金年代", "DJ 的一天"), false),
            new Magazine(6, "星光電影誌", "NO.45", "/assets/magazines/movie-45.jpg",
                    "2026-09-05", 129, "電影",
                    "封面故事：星際遠航特效製作全揭秘",
                    List.of("視效總監訪談", "科幻片百年回顧", "金獎片單預測"), true)
    );

    public record MagazineRequest(String title, String issueNo, String cover,
                                  String publishDate, Double price, String category,
                                  String coverStory, List<String> highlights,
                                  Boolean latest) {
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

        MagazineEntity e = new MagazineEntity();
        e.setTitle(req.title().trim());
        e.setIssueNo(req.issueNo());
        e.setCover(req.cover());
        e.setPublishDate(req.publishDate());
        e.setPrice(req.price() == null ? 0 : req.price());
        e.setCategory(req.category());
        e.setCoverStory(req.coverStory());
        e.setHighlights(req.highlights() == null ? null : String.join("\n", req.highlights()));
        e.setLatest(Boolean.TRUE.equals(req.latest()));
        return fromEntity(magazines.save(e));
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
                e.getCoverStory(), highlights, e.isLatest());
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
