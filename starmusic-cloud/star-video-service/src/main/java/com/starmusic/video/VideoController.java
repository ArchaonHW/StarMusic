package com.starmusic.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
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
            "活動", "音樂", "藝文", "公益", "新聞", "會員上傳");

    private static final List<Video> VIDEOS = List.of(
            new Video(1, "「愛民之歌」", "音樂",
                    "https://i.ytimg.com/vi/tpotOgKlH-s/hqdefault.jpg",
                    "星光流行音樂網原創歌曲「愛民之歌」MV。", "04:09", 63,
                    List.of("MV", "原創歌曲"), false,
                    "https://www.youtube.com/embed/tpotOgKlH-s", false, true, "2026-09-16"),
            new Video(2, "順帆點唱機：台語星曲點播", "音樂",
                    "https://i.ytimg.com/vi/bVFAF51VYCk/hqdefault.jpg",
                    "聽不完的音樂，明星陪你一整天。", "1:42:06", 389,
                    List.of("台語", "點播節目"), true,
                    "https://www.youtube.com/embed/bVFAF51VYCk", false, true, "2026-09-14"),
            new Video(3, "第一屆「靜心盃」硬筆書法比賽頒獎典禮", "藝文",
                    "https://i.ytimg.com/vi/vOF7U3PHDhw/hqdefault.jpg",
                    "「墨映月圓」兩岸書畫名家邀請展暨第一屆靜心盃頒獎典禮。", "11:04", 99,
                    List.of("書法", "頒獎典禮"), false,
                    "https://www.youtube.com/embed/vOF7U3PHDhw", false, true, "2026-09-10"),
            new Video(4, "愛民黨成立二週年慶歡樂唱登場", "活動",
                    "https://i.ytimg.com/vi/nqGZAQXM8mY/hqdefault.jpg",
                    "愛民黨二週年慶暨星光之星新歌發表，歌聲歡聚。", "05:18", 50,
                    List.of("週年慶", "新歌發表"), false,
                    "https://www.youtube.com/embed/nqGZAQXM8mY", false, true, "2026-08-26"),
            new Video(5, "台北古亭獅子會會長交接典禮", "活動",
                    "https://i.ytimg.com/vi/tgnk_zWp3rk/hqdefault.jpg",
                    "古亭獅子會會長交接暨授證57週年慶，群星演出。", "04:41", 163,
                    List.of("交接典禮", "獅子會"), true,
                    "https://www.youtube.com/embed/tgnk_zWp3rk", false, false, "2026-08-08"),
            new Video(6, "福泰飯店【台灣有愛】華煬娛樂合唱", "音樂",
                    "https://i.ytimg.com/vi/Utqm7s96Lbw/hqdefault.jpg",
                    "華煬娛樂於福泰飯店合唱「台灣有愛」。", "05:24", 32,
                    List.of("合唱", "台灣有愛"), false,
                    "https://www.youtube.com/embed/Utqm7s96Lbw", false, false, "2026-07-13"),
            new Video(7, "《彩墨之美》×《竹藝創作》聯合展", "藝文",
                    "https://i.ytimg.com/vi/c_zpYXliKQA/hqdefault.jpg",
                    "許玉惠《彩墨之美》攜手湯宗源《竹藝創作》聯展。", "05:20", 35,
                    List.of("展覽", "水墨", "竹藝"), false,
                    "https://www.youtube.com/embed/c_zpYXliKQA", false, false, "2026-07-08"),
            new Video(8, "大嬸婆國際同濟會第十屆會員大會", "活動",
                    "https://i.ytimg.com/vi/gZl_jOaMSp8/hqdefault.jpg",
                    "「榮耀同濟，共創新局」第十屆會員大會紀實。", "02:38", 100,
                    List.of("同濟會", "會員大會"), true,
                    "https://www.youtube.com/embed/gZl_jOaMSp8", false, false, "2026-06-08"),
            new Video(9, "天懿慈惠堂捐贈高頂救護車", "公益",
                    "https://i.ytimg.com/vi/8WbfPU8EQqs/hqdefault.jpg",
                    "石門天懿慈惠堂捐贈救護車，守護北海岸生命線。", "02:02", 44,
                    List.of("公益", "捐贈"), false,
                    "https://www.youtube.com/embed/8WbfPU8EQqs", false, false, "2026-06-03"),
            new Video(10, "日月聖天宮天上聖母聖誕千秋", "活動",
                    "https://i.ytimg.com/vi/d9b7kjXkUJA/hqdefault.jpg",
                    "桃園新屋日月聖天宮天上聖母聖誕祝壽大典。", "06:01", 110,
                    List.of("宮廟", "祝壽大典"), true,
                    "https://www.youtube.com/embed/d9b7kjXkUJA", false, false, "2026-05-30"),
            new Video(11, "海饌宴會館盛大開幕", "活動",
                    "https://i.ytimg.com/vi/HhNfgPhh8IM/hqdefault.jpg",
                    "海饌宴會館盛大開幕，賓客雲集直擊剪綵與宴會廳實景。", "03:43", 35,
                    List.of("開幕", "板橋", "宴會"), false,
                    "/media/haizan-banquet-opening.mp4", false, true, "2026-09-22"),
            new Video(12, "苗栗銅鑼天靈寺浴佛節殊勝法會", "活動",
                    "https://i.ytimg.com/vi/W3rG0dCMexk/hqdefault.jpg",
                    "苗栗銅鑼天靈寺浴佛節法會紀實。", "04:59", 187,
                    List.of("法會", "浴佛節"), true,
                    "https://www.youtube.com/embed/W3rG0dCMexk", false, false, "2026-05-26"),
            new Video(13, "星光流行音樂網×祺霖文化傳媒簽約合作", "新聞",
                    "https://i.ytimg.com/vi/NDgpicV9teU/hqdefault.jpg",
                    "星光流行音樂網與祺霖文化傳媒簽約合作圓滿成功。", "07:48", 191,
                    List.of("簽約", "合作"), true,
                    "https://www.youtube.com/embed/NDgpicV9teU", false, false, "2026-05-10"),
            new Video(14, "義消第一大隊聯誼餐敘慶新春", "活動",
                    "https://i.ytimg.com/vi/s61WzqMjd6w/hqdefault.jpg",
                    "義消第一大隊迎春納福聯誼餐敘，眾星雲集傳遞台灣有愛。", "05:11", 147,
                    List.of("義消", "聯誼"), true,
                    "https://www.youtube.com/embed/s61WzqMjd6w", false, false, "2026-03-16"),
            new Video(15, "王瓊麗教授《美哉・如是觀》作品展", "藝文",
                    "https://i.ytimg.com/vi/aSDWBUgkjPU/hqdefault.jpg",
                    "國立台灣師範大學王瓊麗教授作品展開幕紀實。", "07:59", 99,
                    List.of("展覽", "水墨"), false,
                    "https://www.youtube.com/embed/aSDWBUgkjPU", false, false, "2026-02-08")
    );

    private static final Map<String, MediaType> FILE_MEDIA_TYPE = Map.of(
            ".mp4", MediaType.parseMediaType("video/mp4"),
            ".m4v", MediaType.parseMediaType("video/mp4"),
            ".mov", MediaType.parseMediaType("video/quicktime"),
            ".webm", MediaType.parseMediaType("video/webm"),
            ".mkv", MediaType.parseMediaType("video/x-matroska"));

    private final VideoUploadRepository uploads;
    private final String mediaBase;
    private final Path uploadDir;

    public VideoController(VideoUploadRepository uploads,
                           @Value("${starmusic.media-base:http://localhost:8080}") String mediaBase,
                           @Value("${starmusic.upload-dir:uploads}") String uploadDir) {
        this.uploads = uploads;
        this.mediaBase = mediaBase;
        this.uploadDir = Path.of(uploadDir);
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

    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> file(
            @PathVariable String filename,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!filename.matches("[0-9a-f-]{36}\\.[a-z0-9]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效的檔名");
        }
        VideoUpload u = uploads.findByFilename(filename).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "檔案不存在"));
        String caller = userName == null ? null
                : java.net.URLDecoder.decode(userName, java.nio.charset.StandardCharsets.UTF_8);
        boolean allowed = VideoUpload.APPROVED.equals(u.getStatus())
                || (caller != null && caller.equals(u.getUploader()))
                || "ADMIN".equals(role);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此影片尚未公開或已下架");
        }
        Path path = uploadDir.resolve(filename).normalize();
        if (!path.startsWith(uploadDir) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "檔案不存在");
        }
        return ResponseEntity.ok()
                .contentType(FILE_MEDIA_TYPE.getOrDefault(
                        ext(filename), MediaType.APPLICATION_OCTET_STREAM))
                .body(new FileSystemResource(path));
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

    private String ext(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase(java.util.Locale.ROOT) : "";
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
