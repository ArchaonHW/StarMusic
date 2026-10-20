import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { CartService } from '../../core/cart.service';
import { Product } from '../../models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-shop',
  templateUrl: './shop.html',
  styleUrl: './shop.scss'
})
export class Shop implements OnInit {
  protected readonly staticMode = environment.staticData;
  private readonly api = inject(ApiService);
  protected readonly cartService = inject(CartService);

  protected readonly products = signal<Product[]>([]);
  protected readonly categories = signal<string[]>([]);
  protected readonly activeCategory = signal('');
  protected readonly addedId = signal<number | null>(null);

  ngOnInit(): void {
    this.api.productCategories().subscribe((c) => this.categories.set(c));
    this.load();
  }

  select(category: string): void {
    this.activeCategory.set(category);
    this.load();
  }

  addToCart(p: Product): void {
    if (this.staticMode) return;
    this.cartService.add(p.id).subscribe(() => {
      this.addedId.set(p.id);
      setTimeout(() => this.addedId.set(null), 1500);
    });
  }

  private load(): void {
    this.api.products(this.activeCategory() || undefined).subscribe((p) => this.products.set(p));
  }
}
