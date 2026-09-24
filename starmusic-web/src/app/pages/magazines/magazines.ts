import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { Magazine } from '../../models';

@Component({
  selector: 'app-magazines',
  templateUrl: './magazines.html',
  styleUrl: './magazines.scss'
})
export class Magazines implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly magazines = signal<Magazine[]>([]);
  protected readonly categories = signal<string[]>([]);
  protected readonly activeCategory = signal('');
  protected readonly selected = signal<Magazine | null>(null);

  ngOnInit(): void {
    this.api.magazineCategories().subscribe((c) => this.categories.set(c));
    this.load();
  }

  select(category: string): void {
    this.activeCategory.set(category);
    this.load();
  }

  open(m: Magazine): void {
    this.selected.set(m);
  }

  close(): void {
    this.selected.set(null);
  }

  private load(): void {
    this.api.magazines(this.activeCategory() || undefined).subscribe((m) => this.magazines.set(m));
  }
}
