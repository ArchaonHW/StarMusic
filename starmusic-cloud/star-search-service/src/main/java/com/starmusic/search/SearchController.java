package com.starmusic.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    public record SearchResult(String keyword, int total,
                               List<Map<String, Object>> videos,
                               List<Map<String, Object>> news,
                               List<Map<String, Object>> magazines,
                               List<Map<String, Object>> products,
                               List<Map<String, Object>> channels,
                               List<Map<String, Object>> programs,
                               List<Map<String, Object>> posts) {
    }

    private final VideoClient videos;
    private final NewsClient news;
    private final MagazineClient magazines;
    private final ShopClient shop;
    private final RadioClient radio;
    private final PostClient posts;

    public SearchController(VideoClient videos, NewsClient news, MagazineClient magazines,
                            ShopClient shop, RadioClient radio, PostClient posts) {
        this.videos = videos;
        this.news = news;
        this.magazines = magazines;
        this.shop = shop;
        this.radio = radio;
        this.posts = posts;
    }

    @GetMapping
    public SearchResult search(@RequestParam(defaultValue = "") String q) {
        String kw = q.trim().toLowerCase(Locale.ROOT);

        List<Map<String, Object>> videos = filter(
                safe(this.videos::videos), kw,
                "title", "category", "description", "tags");
        List<Map<String, Object>> news = filter(
                safe(this.news::news), kw,
                "title", "category", "summary", "content", "author", "source");
        List<Map<String, Object>> magazines = filter(
                safe(this.magazines::magazines), kw,
                "title", "issueNo", "category", "coverStory", "highlights");
        List<Map<String, Object>> products = filter(
                safe(shop::products), kw,
                "name", "category", "description");
        List<Map<String, Object>> channels = filter(
                safe(radio::channels), kw,
                "name", "frequency", "slogan", "genre");
        List<Map<String, Object>> programs = filter(
                safe(radio::programs), kw,
                "title", "dj", "category", "description");
        List<Map<String, Object>> posts = filter(
                pageContent(() -> this.posts.posts(50)), kw,
                "title", "category", "body", "author");

        int total = videos.size() + news.size() + magazines.size()
                + products.size() + channels.size() + programs.size() + posts.size();
        return new SearchResult(q.trim(), total,
                videos, news, magazines, products, channels, programs, posts);
    }

    private List<Map<String, Object>> safe(Supplier<List<Map<String, Object>>> call) {
        try {
            List<Map<String, Object>> body = call.get();
            return body == null ? List.of() : body;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> pageContent(Supplier<Map<String, Object>> call) {
        try {
            Map<String, Object> body = call.get();
            if (body == null || !(body.get("content") instanceof List<?> content)) {
                return List.of();
            }
            return content.stream()
                    .filter(Map.class::isInstance)
                    .map(m -> (Map<String, Object>) m)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> filter(List<Map<String, Object>> items,
                                             String kw, String... fields) {
        if (kw.isEmpty()) {
            return List.of();
        }
        return items.stream().filter(m -> matches(m, kw, fields)).toList();
    }

    private boolean matches(Map<String, Object> item, String kw, String... fields) {
        for (String field : fields) {
            Object value = item.get(field);
            if (value == null) {
                continue;
            }
            String text = value instanceof Collection<?> c
                    ? c.stream().map(String::valueOf).collect(Collectors.joining(" "))
                    : String.valueOf(value);
            if (text.toLowerCase(Locale.ROOT).contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
