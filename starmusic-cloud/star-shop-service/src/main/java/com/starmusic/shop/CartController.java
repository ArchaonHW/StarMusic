package com.starmusic.shop;

import com.starmusic.shop.ProductController.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class CartController {

    public record CartItem(long productId, String name, double price, int quantity) {
    }

    public record Cart(List<CartItem> items, double total) {
    }

    public record AddItemRequest(long productId, int quantity) {
    }

    public record Order(long id, List<CartItem> items, double total, String createdAt) {
    }

    private final List<CartItem> cart = new CopyOnWriteArrayList<>();
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong orderSeq = new AtomicLong(1000);

    @GetMapping("/api/cart")
    public Cart getCart() {
        return new Cart(List.copyOf(cart), total());
    }

    @PostMapping("/api/cart/items")
    public ResponseEntity<Cart> addItem(@RequestBody AddItemRequest req) {
        Product product = ProductController.PRODUCTS.stream()
                .filter(p -> p.id() == req.productId())
                .findFirst()
                .orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        cart.stream()
                .filter(i -> i.productId() == req.productId())
                .findFirst()
                .ifPresentOrElse(
                        i -> cart.set(cart.indexOf(i),
                                new CartItem(i.productId(), i.name(), i.price(),
                                        i.quantity() + Math.max(1, req.quantity()))),
                        () -> cart.add(new CartItem(product.id(), product.name(),
                                product.price(), Math.max(1, req.quantity()))));
        return ResponseEntity.ok(getCart());
    }

    @DeleteMapping("/api/cart/items/{productId}")
    public Cart removeItem(@PathVariable long productId) {
        cart.removeIf(i -> i.productId() == productId);
        return getCart();
    }

    @PostMapping("/api/orders")
    public ResponseEntity<Order> checkout() {
        if (cart.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Order order = new Order(orderSeq.incrementAndGet(), List.copyOf(cart),
                total(), Instant.now().toString());
        orders.put(order.id(), order);
        cart.clear();
        return ResponseEntity.ok(order);
    }

    @GetMapping("/api/orders")
    public List<Order> orders() {
        return List.copyOf(orders.values());
    }

    private double total() {
        return cart.stream().mapToDouble(i -> i.price() * i.quantity()).sum();
    }
}
