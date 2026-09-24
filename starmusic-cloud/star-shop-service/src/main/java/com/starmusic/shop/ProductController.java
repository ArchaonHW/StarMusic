package com.starmusic.shop;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category) {
        return PRODUCTS.stream()
                .filter(p -> category == null || p.category().equals(category))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable long id) {
        return PRODUCTS.stream()
                .filter(p -> p.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return PRODUCTS.stream().map(Product::category).distinct().toList();
    }
}
