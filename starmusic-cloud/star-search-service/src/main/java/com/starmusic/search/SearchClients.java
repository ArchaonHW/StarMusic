package com.starmusic.search;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "star-video-service", fallback = SearchClients.VideoFallback.class)
interface VideoClient {
    @GetMapping("/api/videos")
    List<Map<String, Object>> videos();
}

@FeignClient(name = "star-news-service", fallback = SearchClients.NewsFallback.class)
interface NewsClient {
    @GetMapping("/api/news")
    List<Map<String, Object>> news();
}

@FeignClient(name = "star-magazine-service", fallback = SearchClients.MagazineFallback.class)
interface MagazineClient {
    @GetMapping("/api/magazines")
    List<Map<String, Object>> magazines();
}

@FeignClient(name = "star-shop-service", fallback = SearchClients.ShopFallback.class)
interface ShopClient {
    @GetMapping("/api/products")
    List<Map<String, Object>> products();
}

@FeignClient(name = "star-radio-service", fallback = SearchClients.RadioFallback.class)
interface RadioClient {
    @GetMapping("/api/radio/channels")
    List<Map<String, Object>> channels();

    @GetMapping("/api/radio/programs")
    List<Map<String, Object>> programs();
}

@FeignClient(name = "star-post-service", fallback = SearchClients.PostFallback.class)
interface PostClient {
    @GetMapping("/api/posts")
    Map<String, Object> posts(@RequestParam("size") int size);
}

final class SearchClients {

    private SearchClients() {
    }

    static List<Map<String, Object>> empty() {
        return List.of();
    }

    @org.springframework.stereotype.Component
    static class VideoFallback implements VideoClient {
        @Override
        public List<Map<String, Object>> videos() {
            return empty();
        }
    }

    @org.springframework.stereotype.Component
    static class NewsFallback implements NewsClient {
        @Override
        public List<Map<String, Object>> news() {
            return empty();
        }
    }

    @org.springframework.stereotype.Component
    static class MagazineFallback implements MagazineClient {
        @Override
        public List<Map<String, Object>> magazines() {
            return empty();
        }
    }

    @org.springframework.stereotype.Component
    static class ShopFallback implements ShopClient {
        @Override
        public List<Map<String, Object>> products() {
            return empty();
        }
    }

    @org.springframework.stereotype.Component
    static class RadioFallback implements RadioClient {
        @Override
        public List<Map<String, Object>> channels() {
            return empty();
        }

        @Override
        public List<Map<String, Object>> programs() {
            return empty();
        }
    }

    @org.springframework.stereotype.Component
    static class PostFallback implements PostClient {
        @Override
        public Map<String, Object> posts(int size) {
            return Map.of("content", List.of());
        }
    }
}
