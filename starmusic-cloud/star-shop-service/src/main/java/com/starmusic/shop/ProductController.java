package com.starmusic.shop;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    public record Product(long id, String name, String category, double price,
                          double originalPrice, String image, double rating,
                          int stock, String description) {
    }

    static final List<Product> PRODUCTS = List.of(
            new Product(1, "《星光之夜》跨年演唱會門票", "演唱會", 3280, 3880,
                    "/assets/products/concert-ticket.jpg", 4.9, 500,
                    "2026/12/31 台北小巨蛋，搖滾區站位票。"),
            new Product(2, "乘風2026 成團紀念專輯", "專輯", 599, 799,
                    "/assets/products/album-cf2026.jpg", 4.8, 1200,
                    "七人女團首張同名專輯，附贈寫真卡一套。"),
            new Product(3, "星光流行網 經典合輯 黑膠版", "專輯", 1299, 1599,
                    "/assets/products/vinyl.jpg", 4.7, 300,
                    "電台 20 週年紀念黑膠，收錄 12 首歷年金曲。"),
            new Product(4, "星光娛樂台 LOGO 帽T", "周邊", 980, 1280,
                    "/assets/products/hoodie.jpg", 4.6, 800,
                    "100% 純棉連帽T恤，胸前經典星標刺繡。"),
            new Product(5, "星光手燈 第三代", "周邊", 450, 550,
                    "/assets/products/lightstick.jpg", 4.9, 2000,
                    "演唱會必備應援手燈，支援藍牙連動場控。"),
            new Product(6, "星光電子雜誌 年度訂閱", "數位", 899, 1188,
                    "/assets/products/mag-sub.jpg", 4.5, 9999,
                    "全年 52 期電子雜誌，含十週年數位典藏版。"),
            new Product(7, "都會星光 DJ 聯名耳機", "周邊", 2490, 2990,
                    "/assets/products/headphone.jpg", 4.4, 150,
                    "與都會星光聯名限量耳機，DJ Luna 親自調音。"),
            new Product(8, "星塵少女 全息投影公仔", "周邊", 1680, 1980,
                    "/assets/products/figure.jpg", 4.7, 260,
                    "虛擬偶像星塵少女官方授權全息投影公仔。")
    );

    public record ProductRequest(String name, String category, Double price,
                                 Double originalPrice, String image, Double rating,
                                 Integer stock, String description) {
    }

    private static final long MANAGED_ID_BASE = 10000;
    private static final int MAX_NAME = 200;
    private static final int MAX_SHORT = 100;
    private static final int MAX_TEXT = 2000;

    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category) {
        return all()
                .filter(p -> category == null || p.category().equals(category))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable long id) {
        return findProduct(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return all().map(Product::category).distinct().toList();
    }

    @GetMapping("/manage")
    public List<Product> managed(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return products.findAll().stream()
                .sorted(Comparator.comparing(ProductEntity::getId).reversed())
                .map(this::fromEntity)
                .toList();
    }

    @PostMapping("/manage")
    public Product create(@RequestBody(required = false) ProductRequest req,
                          @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品名稱必填");
        }
        if (req.name().length() > MAX_NAME) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名稱過長");
        }
        checkLen(req.category(), MAX_SHORT, "分類");
        checkLen(req.description(), MAX_TEXT, "描述");
        if (req.price() != null && req.price() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "價格不可為負");
        }
        if (req.stock() != null && req.stock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "庫存不可為負");
        }

        ProductEntity e = new ProductEntity();
        e.setName(req.name().trim());
        e.setCategory(req.category());
        e.setPrice(req.price() == null ? 0 : req.price());
        e.setOriginalPrice(req.originalPrice() == null ? 0 : req.originalPrice());
        e.setImage(req.image());
        e.setRating(req.rating() == null ? 0 : req.rating());
        e.setStock(req.stock() == null ? 0 : req.stock());
        e.setDescription(req.description());
        return fromEntity(products.save(e));
    }

    @DeleteMapping("/manage/{id}")
    public ResponseEntity<Void> deleteManaged(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        long dbId = id - MANAGED_ID_BASE;
        if (!products.existsById(dbId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在或為內建資料");
        }
        products.deleteById(dbId);
        return ResponseEntity.noContent().build();
    }

    Optional<Product> findProduct(long id) {
        return all().filter(p -> p.id() == id).findFirst();
    }

    private Stream<Product> all() {
        return Stream.concat(
                PRODUCTS.stream(),
                products.findAll().stream().map(this::fromEntity));
    }

    private Product fromEntity(ProductEntity e) {
        return new Product(MANAGED_ID_BASE + e.getId(), e.getName(), e.getCategory(),
                e.getPrice(), e.getOriginalPrice(), e.getImage(), e.getRating(),
                e.getStock(), e.getDescription());
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
    }

    private void checkLen(String v, int max, String label) {
        if (v != null && v.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "過長");
        }
    }
}
