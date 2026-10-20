import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../../core/cart.service';
import { Order } from '../../models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-cart',
  imports: [RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.scss'
})
export class CartPage implements OnInit {
  protected readonly staticMode = environment.staticData;
  protected readonly cartService = inject(CartService);

  protected readonly order = signal<Order | null>(null);

  ngOnInit(): void {
    if (!this.staticMode) this.cartService.refresh();
  }

  remove(productId: number): void {
    if (this.staticMode) return;
    this.cartService.remove(productId).subscribe();
  }

  checkout(): void {
    if (this.staticMode) return;
    this.cartService.checkout().subscribe((o) => this.order.set(o));
  }
}
