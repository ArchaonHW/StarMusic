import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Cart, Order } from '../models';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;

  readonly cart = signal<Cart>({ items: [], total: 0 });

  refresh(): void {
    this.http.get<Cart>(`${this.base}/cart`).subscribe((c) => this.cart.set(c));
  }

  add(productId: number, quantity = 1): Observable<Cart> {
    return this.http
      .post<Cart>(`${this.base}/cart/items`, { productId, quantity })
      .pipe(tap((c) => this.cart.set(c)));
  }

  remove(productId: number): Observable<Cart> {
    return this.http
      .delete<Cart>(`${this.base}/cart/items/${productId}`)
      .pipe(tap((c) => this.cart.set(c)));
  }

  checkout(): Observable<Order> {
    return this.http
      .post<Order>(`${this.base}/orders`, {})
      .pipe(tap(() => this.cart.set({ items: [], total: 0 })));
  }
}
