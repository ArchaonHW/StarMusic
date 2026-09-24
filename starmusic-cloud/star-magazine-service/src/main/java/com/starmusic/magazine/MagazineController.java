package com.starmusic.magazine;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    public List<Magazine> list(@RequestParam(required = false) String category) {
        return MAGAZINES.stream()
                .filter(m -> category == null || m.category().equals(category))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Magazine> get(@PathVariable long id) {
        return MAGAZINES.stream()
                .filter(m -> m.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public List<Magazine> latest() {
        return MAGAZINES.stream().filter(Magazine::latest).toList();
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return MAGAZINES.stream().map(Magazine::category).distinct().toList();
    }
}
