package com.starmusic.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    public record Video(long id, String title, String category, String cover,
                        String description, String duration, long views,
                        List<String> tags, boolean hot, String videoUrl,
                        boolean vip, boolean featured, String updateNote) {
    }

    public record Section(String category, List<Video> videos) {
    }

    public record HomeResponse(List<Video> featured, List<Section> sections,
                               List<Video> ranking) {
    }

    private static final List<String> CHANNEL_ORDER = List.of(
            "綜藝", "電視劇", "電影", "少兒", "紀錄片", "動漫", "音樂", "直播", "活動", "會員上傳");

    private static final List<Video> VIDEOS = List.of(
            new Video(11, "海饌宴會館盛大開幕", "活動", "/assets/covers/event-haizan.jpg",
                    "海饌宴會館盛大開幕現場直擊，賓客雲集、星光熠熠，直擊開幕剪綵與宴會廳實景。",
                    "03:43", 26800, List.of("開幕", "直擊", "宴會館"), true,
                    "/media/haizan-banquet-opening.mp4", false, true, null),
            new Video(1, "星光燦爛的舞台", "綜藝", "/assets/covers/variety-1.jpg",
                    "明星嘉賓同台競演，歌舞、魔術、脫口秀一次滿足。", "95:20", 1520000,
                    List.of("真人秀", "音樂", "競演"), true, null, false, true, "更新至第8期"),
            new Video(2, "乘風2026", "綜藝", "/assets/covers/variety-2.jpg",
                    "姐姐們乘風破浪，挑戰唱跳舞台的極限。", "110:05", 2380000,
                    List.of("女團", "選秀"), true, null, true, true, "更新至第10期"),
            new Video(3, "快樂星光營", "綜藝", "/assets/covers/variety-3.jpg",
                    "週末黃金檔王牌綜藝，遊戲與訪談笑料不斷。", "88:40", 980000,
                    List.of("訪談", "遊戲"), false, null, false, false, "更新至第22期"),
            new Video(12, "明星大偵探·星光季", "綜藝", "/assets/covers/variety-4.jpg",
                    "沉浸式推理真人秀，明星玩家鬥智鬥勇找出真兇。", "125:00", 1980000,
                    List.of("推理", "真人秀"), true, null, true, true, "更新至第6期"),
            new Video(13, "嚮往的生活·星光篇", "綜藝", "/assets/covers/variety-5.jpg",
                    "遠離城市喧囂，明星好友的田園慢生活實錄。", "96:30", 1120000,
                    List.of("慢生活", "治癒"), false, null, false, false, "更新至第4期"),
            new Video(4, "長街長", "電視劇", "/assets/covers/drama-1.jpg",
                    "古裝權謀大戲，亂世兒女的家國情仇。", "45:00", 3210000,
                    List.of("古裝", "權謀"), true, null, true, true, "更新至第28集"),
            new Video(5, "城市星光", "電視劇", "/assets/covers/drama-2.jpg",
                    "都會男女在繁華都市中追尋夢想與愛情。", "42:30", 1750000,
                    List.of("都市", "愛情"), false, null, false, false, "全40集"),
            new Video(6, "深夜食堂·星光篇", "電視劇", "/assets/covers/drama-3.jpg",
                    "深夜食堂裡的人生百態，一菜一故事。", "38:00", 860000,
                    List.of("治癒", "美食"), false, null, false, false, "全12集"),
            new Video(14, "長安星光錄", "電視劇", "/assets/covers/drama-4.jpg",
                    "盛唐背景的古裝探案劇，抽絲剝繭揭開宮廷謎案。", "44:00", 2450000,
                    List.of("古裝", "懸疑"), true, null, true, false, "更新至第16集"),
            new Video(7, "星際遠航", "電影", "/assets/covers/movie-1.jpg",
                    "人類首次曲速航行，探索銀河系邊緣的未知文明。", "128:00", 4100000,
                    List.of("科幻", "冒險"), true, null, true, true, "4K修復版"),
            new Video(8, "星光下的約定", "電影", "/assets/covers/movie-2.jpg",
                    "青梅竹馬十年之約，笑中帶淚的浪漫喜劇。", "105:00", 1340000,
                    List.of("愛情", "喜劇"), false, null, false, false, null),
            new Video(15, "逆光飛行", "電影", "/assets/covers/movie-3.jpg",
                    "視障棒球少年逆風追夢，改編自真實故事的勵志電影。", "118:00", 1560000,
                    List.of("勵志", "運動"), false, null, true, false, "院線熱映"),
            new Video(16, "小星星的冒險", "少兒", "/assets/covers/kids-1.jpg",
                    "跟小星星一起展開宇宙冒險，邊玩邊學科學知識。", "22:00", 680000,
                    List.of("益智", "動畫"), false, null, false, false, "更新至第30集"),
            new Video(17, "寶貝音樂屋", "少兒", "/assets/covers/kids-2.jpg",
                    "唱跳律動兒歌精選，陪孩子快樂學音樂。", "15:00", 520000,
                    List.of("兒歌", "律動"), false, null, false, false, null),
            new Video(18, "星球紀錄·深海", "紀錄片", "/assets/covers/doc-1.jpg",
                    "潛入萬米深海，記錄地球上最後的未知疆域。", "52:00", 890000,
                    List.of("自然", "深海"), true, null, true, false, "全6集"),
            new Video(19, "舌尖上的星光", "紀錄片", "/assets/covers/doc-2.jpg",
                    "走訪巷弄老店，紀錄台灣最動人的庶民美味。", "48:00", 720000,
                    List.of("美食", "人文"), false, null, false, false, "更新至第5集"),
            new Video(9, "熱血少年團", "動漫", "/assets/covers/anime-1.jpg",
                    "少年們組成樂團，朝著全國大賽的舞台前進。", "24:00", 990000,
                    List.of("熱血", "音樂"), false, null, false, false, "更新至第18話"),
            new Video(20, "星海物語", "動漫", "/assets/covers/anime-2.jpg",
                    "少女與星海精靈的奇幻物語，治癒系人氣新番。", "23:30", 1150000,
                    List.of("奇幻", "治癒"), true, null, true, true, "更新至第9話"),
            new Video(10, "星光演唱會 Live", "音樂", "/assets/covers/live-1.jpg",
                    "星光全球娛樂台年度演唱會實況，眾星雲集。", "150:00", 5600000,
                    List.of("LIVE", "演唱會"), true, null, true, true, "完整版"),
            new Video(21, "星光歌王 第三季", "音樂", "/assets/covers/music-1.jpg",
                    "蒙面歌手實力對決，猜評團燒腦猜謎。", "102:00", 2890000,
                    List.of("音樂競演", "蒙面"), true, null, true, false, "更新至第7期"),
            new Video(22, "星光跨年直播間", "直播", "/assets/covers/live-2.jpg",
                    "跨年晚會全程直播，與全球觀眾一起倒數迎新年。", "240:00", 620000,
                    List.of("直播", "跨年"), false, null, false, false, "直播中")
    );

    private final VideoUploadRepository uploads;
    private final String mediaBase;

    public VideoController(VideoUploadRepository uploads,
                           @Value("${starmusic.media-base:http://localhost:8080}") String mediaBase) {
        this.uploads = uploads;
        this.mediaBase = mediaBase;
    }

    @GetMapping
    public List<Video> list(@RequestParam(required = false) String category,
                            @RequestParam(required = false) Boolean hot,
                            @RequestParam(required = false) Boolean vip) {
        return all()
                .filter(v -> category == null || v.category().equals(category))
                .filter(v -> hot == null || v.hot() == hot)
                .filter(v -> vip == null || v.vip() == vip)
                .toList();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Video> get(@PathVariable long id) {
        return all()
                .filter(v -> v.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/home")
    public HomeResponse home() {
        List<Video> all = all().toList();
        List<Video> featured = all.stream()
                .filter(Video::featured)
                .sorted(Comparator.comparingLong(Video::views).reversed())
                .toList();

        Map<String, List<Video>> grouped = new LinkedHashMap<>();
        for (String c : CHANNEL_ORDER) {
            grouped.put(c, all.stream()
                    .filter(v -> v.category().equals(c))
                    .sorted(Comparator.comparingLong(Video::views).reversed())
                    .limit(6)
                    .toList());
        }

        List<Section> sections = grouped.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> new Section(e.getKey(), e.getValue()))
                .toList();

        return new HomeResponse(featured, sections, ranking());
    }

    @GetMapping("/categories")
    public List<String> categories() {
        List<String> present = all().map(Video::category).distinct().toList();
        return Stream.concat(
                        CHANNEL_ORDER.stream().filter(present::contains),
                        present.stream().filter(c -> !CHANNEL_ORDER.contains(c)))
                .toList();
    }

    @GetMapping("/ranking")
    public List<Video> ranking() {
        return all()
                .sorted(Comparator.comparingLong(Video::views).reversed())
                .limit(10)
                .toList();
    }

    private Stream<Video> all() {
        return Stream.concat(
                VIDEOS.stream(),
                uploads.findByStatus(VideoUpload.APPROVED).stream().map(this::fromUpload));
    }

    List<Video> catalog() {
        return all().toList();
    }

    boolean exists(long videoId) {
        return all().anyMatch(v -> v.id() == videoId);
    }

    private Video fromUpload(VideoUpload u) {
        return new Video(10000 + u.getId(), u.getTitle(),
                u.getCategory() == null ? "會員上傳" : u.getCategory(), null,
                u.getDescription() == null ? "" : u.getDescription(),
                "—", 0, List.of("會員上傳", "by " + u.getUploader()), false,
                mediaBase + "/api/videos/files/" + u.getFilename(),
                false, false, "會員上傳");
    }
}
