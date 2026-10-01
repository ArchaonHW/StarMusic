package com.starmusic.shop;

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
                          int stock, String description, String buyUrl) {
    }

    // 娛樂周邊嚴選：價格為市場參考價，「前往購買」連到蝦皮購物搜尋
    static final List<Product> PRODUCTS = List.of(
            new Product(1, "演唱會應援手燈", "演唱會周邊", 399, 599,
                    "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=600&q=80",
                    4.8, 0, "演唱會必備應援手燈，多款官方授權與通用款可選（圖為示意，價格為市場參考）。",
                    "https://shopee.tw/search?keyword=%E6%BC%94%E5%94%B1%E6%9C%83%E6%89%8B%E7%87%88"),
            new Product(2, "K-POP 偶像專輯", "唱片專輯", 650, 850,
                    "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&q=80",
                    4.9, 0, "韓團最新專輯、寫真卡版本齊全，蝦皮多家代購比價（圖為示意，價格為市場參考）。",
                    "https://shopee.tw/search?keyword=KPOP%E5%B0%88%E8%BC%AF"),
            new Product(3, "黑膠唱片與唱機", "唱片專輯", 1290, 1590,
                    "https://images.unsplash.com/photo-1483412033650-1015ddeb83d1?w=600&q=80",
                    4.7, 0, "經典黑膠再版與入門唱機，復古聆聽體驗（圖為示意，價格為市場參考）。",
                    "https://shopee.tw/search?keyword=%E9%BB%91%E8%86%A0%E5%94%B1%E7%89%87"),
            new Product(4, "藍牙耳機", "3C影音", 999, 1499,
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&q=80",
                    4.6, 0, "通勤追星好夥伴，主動降噪與長續航款式（圖為示意，價格為市場參考）。",
                    "https://shopee.tw/search?keyword=%E8%97%8D%E7%89%99%E8%80%B3%E6%A9%9F"),
            new Product(5, "卡拉OK麥克風", "3C影音", 890, 1290,
                    "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=600&q=80",
                    4.5, 0, "在家開唱必備，藍牙連接手機即可歡唱（圖為示意，價格為市場參考）。",
                    "https://shopee.tw/search?keyword=%E5%8D%A1%E6%8B%89OK%E9%BA%A5%E5%85%8B%E9%A2%A8"),
            new Product(6, "藍牙音響", "3C影音", 1490, 1990,
                    "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=600&q=80",
                    4.4, 0, "派對與居家聆聽適用，重低音款式齊全（圖為示意，價格為市場參考）。",
                    "https://shopee.tw/search?keyword=%E8%97%8D%E7%89%99%E5%96%87%E5%8F%AD"),
            new Product(7, "演唱會應援帽T", "演唱會周邊", 780, 980,
                    "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=600&q=80",
                    4.6, 0, "偶像巡演周邊帽T與應援服飾，各團官方與同人款（圖為示意，價格為市場參考）。",
                    "https://shopee.tw/search?keyword=%E6%87%89%E6%8F%B4%E5%B8%BDT"),
            new Product(8, "偶像公仔與收藏模型", "收藏周邊", 580, 780,
                    "https://images.unsplash.com/photo-1608889175123-8ee362201f81?w=600&q=80",
                    4.7, 0, "Q版公仔、景品與官方收藏模型（圖為示意，價格為市場參考）。",
                    "https://shopee.tw/search?keyword=%E5%81%B6%E5%83%8F%E5%85%AC%E4%BB%94")
    );

    public record ProductRequest(String name, String category, Double price,
                                 Double originalPrice, String image, Double rating,
                                 Integer stock, String description, String buyUrl) {
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
        ProductEntity e = new ProductEntity();
        apply(e, req);
        return fromEntity(products.save(e));
    }

    @PutMapping("/manage/{id}")
    public Product update(@PathVariable long id,
                          @RequestBody(required = false) ProductRequest req,
                          @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        ProductEntity e = products.findById(id - MANAGED_ID_BASE).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在或為內建資料"));
        apply(e, req);
        return fromEntity(products.save(e));
    }

    private void apply(ProductEntity e, ProductRequest req) {
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

        e.setName(req.name().trim());
        e.setCategory(req.category());
        e.setPrice(req.price() == null ? 0 : req.price());
        e.setOriginalPrice(req.originalPrice() == null ? 0 : req.originalPrice());
        e.setImage(req.image());
        e.setRating(req.rating() == null ? 0 : req.rating());
        e.setStock(req.stock() == null ? 0 : req.stock());
        e.setDescription(req.description());
        e.setBuyUrl(req.buyUrl());
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
                e.getStock(), e.getDescription(), e.getBuyUrl());
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
