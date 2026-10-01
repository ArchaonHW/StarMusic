package com.starmusic.video;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerRepository bannerRepository;

    public BannerController(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @GetMapping
    public List<BannerDto> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(BannerDto::of)
                .toList();
    }

    public record BannerDto(Long id, String title, String imageUrl, String linkUrl, String description) {
        static BannerDto of(Banner banner) {
            return new BannerDto(
                    banner.getId(),
                    banner.getTitle(),
                    banner.getImageUrl(),
                    banner.getLinkUrl(),
                    banner.getDescription()
            );
        }
    }
}
