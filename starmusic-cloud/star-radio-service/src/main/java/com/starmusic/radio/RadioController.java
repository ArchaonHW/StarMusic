package com.starmusic.radio;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/radio")
public class RadioController {

    public record Channel(long id, String name, String frequency, String slogan,
                          String genre, String streamUrl, boolean live) {
    }

    public record Program(long id, long channelId, String title, String dj,
                          String timeSlot, String category, String description) {
    }

    private static final List<Channel> CHANNELS = List.of(
            new Channel(1, "星光流行網", "FM 92.7", "全亞洲最亮的流行音樂台", "流行音樂",
                    "/api/radio/streams/1", true),
            new Channel(2, "亞洲音樂台", "FM 95.5", "華語金曲與日韓潮流直通車", "華語/日韓",
                    "/api/radio/streams/2", true),
            new Channel(3, "都會星光", "FM 99.1", "城市夜生活，陪你到最後", "都會/爵士",
                    "/api/radio/streams/3", true),
            new Channel(4, "古典星光", "FM 101.7", "古典與跨界，靜心聆聽", "古典/輕音樂",
                    "/api/radio/streams/4", false),
            new Channel(5, "星光新聞網", "AM 1296", "24 小時全球娛樂新聞快報", "新聞/談話",
                    "/api/radio/streams/5", true)
    );

    private final Map<Long, byte[]> streamCache = new ConcurrentHashMap<>();

    private static final List<Program> PROGRAMS = List.of(
            new Program(1, 1, "晨光音樂早餐", "小星", "06:00-09:00", "音樂",
                    "用最新流行金曲喚醒你的早晨。"),
            new Program(2, 1, "星光點播站", "阿光", "12:00-14:00", "點播",
                    "聽眾來電點歌，把祝福送給想念的人。"),
            new Program(3, 1, "超級星光大道", "DJ Stella", "20:00-22:00", "綜藝",
                    "專訪當紅歌手，獨家首播新歌。"),
            new Program(4, 2, "亞洲風雲榜", "DJ Ken", "17:00-19:00", "榜單",
                    "華語、日韓、西洋排行榜一次聽完。"),
            new Program(5, 2, "K-POP 直達車", "DJ Yuna", "19:00-21:00", "韓流",
                    "最速韓流新歌與偶像動態。"),
            new Program(6, 3, "都會夜未眠", "DJ Luna", "22:00-24:00", "談話",
                    "深夜心情故事，陪你走過城市夜色。"),
            new Program(7, 4, "星空音樂廳", "DJ Mira", "20:00-22:00", "古典",
                    "交響樂與室內樂精選導聆。"),
            new Program(8, 5, "星光快報", "主播群", "整點", "新聞",
                    "每小時整點全球娛樂快訊。")
    );

    @GetMapping("/channels")
    public List<Channel> channels() {
        return CHANNELS;
    }

    @GetMapping("/channels/{id}")
    public ResponseEntity<Channel> channel(@PathVariable long id) {
        return CHANNELS.stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/programs")
    public List<Program> programs(@RequestParam(required = false) Long channelId) {
        return PROGRAMS.stream()
                .filter(p -> channelId == null || p.channelId() == channelId)
                .toList();
    }

    @GetMapping("/streams/{id}")
    public ResponseEntity<ByteArrayResource> stream(@PathVariable long id) {
        boolean exists = CHANNELS.stream().anyMatch(c -> c.id() == id);
        if (!exists) {
            return ResponseEntity.notFound().build();
        }
        byte[] wav = streamCache.computeIfAbsent(id, WavSynth::render);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .contentLength(wav.length)
                .body(new ByteArrayResource(wav));
    }
}
