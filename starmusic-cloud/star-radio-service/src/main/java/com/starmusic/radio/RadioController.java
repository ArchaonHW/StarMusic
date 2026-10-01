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
            new Channel(1, "飛碟電台 UFO", "FM 92.1", "就是愛音樂，飛碟聯播網", "流行音樂",
                    "https://n10.rcs.revma.com/em90w4aeewzuv", true),
            new Channel(2, "亞洲電台 AsiaFM", "FM 92.3", "亞洲最前線的流行音樂電台", "華語/日韓",
                    "https://n13.rcs.revma.com/xpgtqc74hv8uv", true),
            new Channel(3, "中廣流行網 i like radio", "FM 103.3", "只給你想聽的好音樂", "華語流行",
                    "https://n03.rcs.revma.com/aw9uqyxy2tzuv", true),
            new Channel(4, "古典音樂台", "FM 97.7", "古典與輕音樂，靜心聆聽", "古典/輕音樂",
                    "http://onair.family977.com.tw:8000/live.mp3", true),
            new Channel(5, "中廣新聞網", "AM 648", "整點新聞，隨時掌握", "新聞/談話",
                    "https://n03.rcs.revma.com/78fm9wyy2tzuv", true)
    );

    private final Map<Long, byte[]> streamCache = new ConcurrentHashMap<>();

    private static final List<Program> PROGRAMS = List.of(
            new Program(1, 1, "飛碟早餐", "主播群", "07:00-09:00", "談話",
                    "飛碟聯播網晨間招牌節目，時事與生活話題。"),
            new Program(2, 1, "陶子晚報", "陶晶瑩", "17:00-18:00", "娛樂",
                    "陶子陪你聊娛樂圈大小事。"),
            new Program(3, 1, "夜光家族", "光禹", "22:00-24:00", "音樂",
                    "深夜音樂陪伴節目，溫暖每個夜晚。"),
            new Program(4, 2, "Asia Morning Call", "Asia DJ 群", "07:00-10:00", "音樂",
                    "用亞洲最新流行曲開啟一天。"),
            new Program(5, 2, "亞洲音樂榜", "Asia DJ 群", "19:00-21:00", "榜單",
                    "華語、日韓排行榜一次聽完。"),
            new Program(6, 3, "娛樂e世代", "吳建恆", "20:00-22:00", "娛樂",
                    "流行音樂與娛樂話題，歌手專訪首播新歌。"),
            new Program(7, 4, "古典音樂廳", "主持人群", "09:00-12:00", "古典",
                    "交響樂與室內樂精選導聆。"),
            new Program(8, 5, "整點新聞", "主播群", "整點", "新聞",
                    "每小時整點最新新聞快報。"),
            new Program(9, 5, "新聞評論", "主播群", "18:00-19:00", "評論",
                    "深度剖析當日重要時事。")
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
