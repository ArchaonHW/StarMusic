package com.starmusic.video;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 首次啟動（資料表為空）時建立預設輪播圖，之後由管理後台維護
@Component
public class BannerSeeder implements ApplicationRunner {

    private final BannerRepository banners;

    public BannerSeeder(BannerRepository banners) {
        this.banners = banners;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (banners.count() > 0) {
            return;
        }
        banners.saveAll(List.of(
                banner("告五人 Here @ World Tour 2026",
                        "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=1200&h=400&fit=crop",
                        "/videos", "台北小巨蛋 11/6-11/8 全場完售", 1),
                banner("René 飛行日 巡迴演唱會",
                        "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&h=400&fit=crop",
                        "/videos", "12/5 台北小巨蛋 FINAL CALL", 2),
                banner("鼓鼓呂思緯 我現在又在想你了",
                        "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&h=400&fit=crop",
                        "/videos", "12/26 台北流行音樂中心", 3)));
    }

    private static Banner banner(String title, String image, String link, String desc, int order) {
        Banner b = new Banner();
        b.setTitle(title);
        b.setImageUrl(image);
        b.setLinkUrl(link);
        b.setDescription(desc);
        b.setSortOrder(order);
        b.setActive(true);
        return b;
    }
}
