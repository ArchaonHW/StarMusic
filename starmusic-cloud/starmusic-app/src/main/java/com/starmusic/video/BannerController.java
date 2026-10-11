package com.starmusic.video;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    public record BannerDto(Long id, String title, String imageUrl, String linkUrl,
                            String description, Integer sortOrder, Boolean active) {
        static BannerDto of(Banner banner) {
            return new BannerDto(
                    banner.getId(),
                    banner.getTitle(),
                    banner.getImageUrl(),
                    banner.getLinkUrl(),
                    banner.getDescription(),
                    banner.getSortOrder(),
                    banner.getActive()
            );
        }
    }

    public record BannerRequest(String title, String imageUrl, String linkUrl,
                                String description, Integer sortOrder, Boolean active) {
    }

    private static final int MAX_TITLE = 200;
    private static final int MAX_LINK = 500;
    private static final int MAX_DESCRIPTION = 500;
    // data URL 圖片上限約 3MB
    private static final int MAX_IMAGE = 4_000_000;

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

    @GetMapping("/manage")
    public List<BannerDto> managed(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return bannerRepository.findAllByOrderBySortOrderAsc().stream()
                .map(BannerDto::of)
                .toList();
    }

    @PostMapping("/manage")
    public BannerDto create(@RequestBody(required = false) BannerRequest req,
                            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        Banner b = new Banner();
        apply(b, req);
        return BannerDto.of(bannerRepository.save(b));
    }

    @PutMapping("/manage/{id}")
    public BannerDto update(@PathVariable long id,
                            @RequestBody(required = false) BannerRequest req,
                            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        Banner b = bannerRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "輪播圖不存在"));
        apply(b, req);
        return BannerDto.of(bannerRepository.save(b));
    }

    @DeleteMapping("/manage/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        if (!bannerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "輪播圖不存在");
        }
        bannerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void apply(Banner b, BannerRequest req) {
        if (req == null || blank(req.title()) || req.title().length() > MAX_TITLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題必填且不超過 200 字");
        }
        if (blank(req.imageUrl()) || req.imageUrl().length() > MAX_IMAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "請提供圖片（上限約 3MB）");
        }
        String image = req.imageUrl().trim();
        if (!image.startsWith("/") && !image.startsWith("https://")
                && !image.startsWith("http://") && !image.startsWith("data:image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "圖片網址格式不正確");
        }
        String link = blank(req.linkUrl()) ? "/" : req.linkUrl().trim();
        if (link.length() > MAX_LINK
                || !(link.startsWith("/") || link.startsWith("https://") || link.startsWith("http://"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "連結需為站內路徑或 http(s) 網址");
        }
        if (req.description() != null && req.description().length() > MAX_DESCRIPTION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "描述過長");
        }
        b.setTitle(req.title().trim());
        b.setImageUrl(image);
        b.setLinkUrl(link);
        b.setDescription(req.description());
        b.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        b.setActive(req.active() == null || req.active());
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
    }
}
