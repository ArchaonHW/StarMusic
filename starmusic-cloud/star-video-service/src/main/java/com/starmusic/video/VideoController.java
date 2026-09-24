package com.starmusic.video;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    public record Video(long id, String title, String category, String cover,
                        String description, String duration, long views,
                        List<String> tags, boolean hot, String videoUrl) {
    }

    private static final List<Video> VIDEOS = List.of(
            new Video(11, "海饌宴會館盛大開幕", "活動", "/assets/covers/event-haizan.jpg",
                    "海饌宴會館盛大開幕現場直擊，賓客雲集、星光熠熠，直擊開幕剪綵與宴會廳實景。",
                    "03:43", 26800, List.of("開幕", "直擊", "宴會館"), true,
                    "/media/haizan-banquet-opening.mp4"),
            new Video(1, "星光燦爛的舞台", "綜藝", "/assets/covers/variety-1.jpg",
                    "明星嘉賓同台競演，歌舞、魔術、脫口秀一次滿足。", "95:20", 1520000,
                    List.of("真人秀", "音樂", "競演"), true, null),
            new Video(2, "乘風2026", "綜藝", "/assets/covers/variety-2.jpg",
                    "姐姐們乘風破浪，挑戰唱跳舞台的極限。", "110:05", 2380000,
                    List.of("女團", "選秀"), true, null),
            new Video(3, "快樂星光營", "綜藝", "/assets/covers/variety-3.jpg",
                    "週末黃金檔王牌綜藝，遊戲與訪談笑料不斷。", "88:40", 980000,
                    List.of("訪談", "遊戲"), false, null),
            new Video(4, "長街長", "戲劇", "/assets/covers/drama-1.jpg",
                    "古裝權謀大戲，亂世兒女的家國情仇。", "45:00", 3210000,
                    List.of("古裝", "權謀"), true, null),
            new Video(5, "城市星光", "戲劇", "/assets/covers/drama-2.jpg",
                    "都會男女在繁華都市中追尋夢想與愛情。", "42:30", 1750000,
                    List.of("都市", "愛情"), false, null),
            new Video(6, "深夜食堂·星光篇", "戲劇", "/assets/covers/drama-3.jpg",
                    "深夜食堂裡的人生百態，一菜一故事。", "38:00", 860000,
                    List.of("治癒", "美食"), false, null),
            new Video(7, "星際遠航", "電影", "/assets/covers/movie-1.jpg",
                    "人類首次曲速航行，探索銀河系邊緣的未知文明。", "128:00", 4100000,
                    List.of("科幻", "冒險"), true, null),
            new Video(8, "星光下的約定", "電影", "/assets/covers/movie-2.jpg",
                    "青梅竹馬十年之約，笑中帶淚的浪漫喜劇。", "105:00", 1340000,
                    List.of("愛情", "喜劇"), false, null),
            new Video(9, "熱血少年團", "動漫", "/assets/covers/anime-1.jpg",
                    "少年們組成樂團，朝著全國大賽的舞台前進。", "24:00", 990000,
                    List.of("熱血", "音樂"), false, null),
            new Video(10, "星光演唱會 Live", "演唱會", "/assets/covers/live-1.jpg",
                    "星光全球娛樂台年度演唱會實況，眾星雲集。", "150:00", 5600000,
                    List.of("LIVE", "演唱會"), true, null)
    );

    @GetMapping
    public List<Video> list(@RequestParam(required = false) String category,
                            @RequestParam(required = false) Boolean hot) {
        return VIDEOS.stream()
                .filter(v -> category == null || v.category().equals(category))
                .filter(v -> hot == null || v.hot() == hot)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Video> get(@PathVariable long id) {
        return VIDEOS.stream()
                .filter(v -> v.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return VIDEOS.stream().map(Video::category).distinct().toList();
    }

    @GetMapping("/ranking")
    public List<Video> ranking() {
        return VIDEOS.stream()
                .sorted(Comparator.comparingLong(Video::views).reversed())
                .limit(10)
                .toList();
    }
}
