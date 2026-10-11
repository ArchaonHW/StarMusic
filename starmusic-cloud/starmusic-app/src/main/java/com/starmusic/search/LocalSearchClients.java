package com.starmusic.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starmusic.magazine.MagazineController;
import com.starmusic.news.NewsController;
import com.starmusic.post.PostController;
import com.starmusic.radio.RadioController;
import com.starmusic.shop.ProductController;
import com.starmusic.video.VideoController;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// 單體版：原本跨服務的 Feign 介面，改為直接呼叫同 JVM 的 controller，
// 回傳型別用 ObjectMapper 轉成 Map，SearchController 不需改動。

interface VideoClient {
    List<Map<String, Object>> videos();
}

interface NewsClient {
    List<Map<String, Object>> news();
}

interface MagazineClient {
    List<Map<String, Object>> magazines();
}

interface ShopClient {
    List<Map<String, Object>> products();
}

interface RadioClient {
    List<Map<String, Object>> channels();

    List<Map<String, Object>> programs();
}

interface PostClient {
    Map<String, Object> posts(int size);
}

@Component
class LocalClients implements VideoClient, NewsClient, MagazineClient,
        ShopClient, RadioClient, PostClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VideoController videos;
    private final NewsController news;
    private final MagazineController magazines;
    private final ProductController products;
    private final RadioController radio;
    private final PostController posts;

    LocalClients(VideoController videos, NewsController news,
                 MagazineController magazines, ProductController products,
                 RadioController radio, PostController posts) {
        this.videos = videos;
        this.news = news;
        this.magazines = magazines;
        this.products = products;
        this.radio = radio;
        this.posts = posts;
    }

    @Override
    public List<Map<String, Object>> videos() {
        return toMaps(videos.list(null, null, null));
    }

    @Override
    public List<Map<String, Object>> news() {
        return toMaps(news.list(null));
    }

    @Override
    public List<Map<String, Object>> magazines() {
        return toMaps(magazines.list(null));
    }

    @Override
    public List<Map<String, Object>> products() {
        return toMaps(products.list(null));
    }

    @Override
    public List<Map<String, Object>> channels() {
        return toMaps(radio.channels());
    }

    @Override
    public List<Map<String, Object>> programs() {
        return toMaps(radio.programs(null));
    }

    @Override
    public Map<String, Object> posts(int size) {
        Page<?> page = posts.list(null, null, null, null, 0, size);
        return Map.of("content", toMaps(page.getContent()));
    }

    private static List<Map<String, Object>> toMaps(List<?> list) {
        return list.stream()
                .map(o -> MAPPER.convertValue(o, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                }))
                .toList();
    }
}
