package com.starmusic.search;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAP =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;

    public SearchController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public record SearchResult(String keyword, int total,
                               List<Map<String, Object>> videos,
                               List<Map<String, Object>> news,
                               List<Map<String, Object>> magazines,
                               List<Map<String, Object>> products,
                               List<Map<String, Object>> channels,
                               List<Map<String, Object>> programs) {
    }

    @GetMapping
    public SearchResult search(@RequestParam(defaultValue = "") String q) {
        String kw = q.trim().toLowerCase(Locale.ROOT);

        List<Map<String, Object>> videos = filter(
                fetch("http://star-video-service/api/videos"), kw,
                "title", "category", "description", "tags");
        List<Map<String, Object>> news = filter(
                fetch("http://star-news-service/api/news"), kw,
                "title", "category", "summary", "content", "author", "source");
        List<Map<String, Object>> magazines = filter(
                fetch("http://star-magazine-service/api/magazines"), kw,
                "title", "issueNo", "category", "coverStory", "highlights");
        List<Map<String, Object>> products = filter(
                fetch("http://star-shop-service/api/products"), kw,
                "name", "category", "description");
        List<Map<String, Object>> channels = filter(
                fetch("http://star-radio-service/api/radio/channels"), kw,
                "name", "frequency", "slogan", "genre");
        List<Map<String, Object>> programs = filter(
                fetch("http://star-radio-service/api/radio/programs"), kw,
                "title", "dj", "category", "description");

        int total = videos.size() + news.size() + magazines.size()
                + products.size() + channels.size() + programs.size();
        return new SearchResult(q.trim(), total,
                videos, news, magazines, products, channels, programs);
    }

    private List<Map<String, Object>> fetch(String url) {
        try {
            List<Map<String, Object>> body = restTemplate
                    .exchange(url, HttpMethod.GET, null, LIST_OF_MAP)
                    .getBody();
            return body == null ? List.of() : body;
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
