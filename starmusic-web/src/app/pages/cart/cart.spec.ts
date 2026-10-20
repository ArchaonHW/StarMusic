import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { CartPage } from './cart';
import { CartService } from '../../core/cart.service';
import { environment } from '../../../environments/environment';

describe('CartPage on static hosting', () => {
  const originalMode = environment.staticData;
  afterEach(() => { environment.staticData = originalMode; });

  it('shows availability and never requests cart or checkout operations', () => {
    environment.staticData = true;
    const cart = jasmine.createSpyObj('CartService', ['refresh', 'remove', 'checkout'], {
      cart: signal({ items: [], total: 0 })
    });
    TestBed.configureTestingModule({
      imports: [CartPage],
      providers: [provideRouter([]), { provide: CartService, useValue: cart }]
    });
    const fixture = TestBed.createComponent(CartPage);
    fixture.detectChanges();
    fixture.componentInstance.remove(1);
    fixture.componentInstance.checkout();
    expect(cart.refresh).not.toHaveBeenCalled();
    expect(cart.remove).not.toHaveBeenCalled();
    expect(cart.checkout).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('目前未開放購物車與結帳');
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/shop');
  });
});
