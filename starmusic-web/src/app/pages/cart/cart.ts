import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../../core/cart.service';
import { Order } from '../../models';

@Component({
  selector: 'app-cart',
  imports: [RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.scss'
})
export class CartPage implements OnInit {
  protected readonly cartService = inject(CartService);

  protected readonly order = signal<Order | null>(null);

  ngOnInit(): void {
    this.cartService.refresh();
  }

  remove(productId: number): void {
    this.cartService.remove(productId).subscribe();
  }

  checkout(): void {
    this.cartService.checkout().subscribe((o) => this.order.set(o));
  }
}
