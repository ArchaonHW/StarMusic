import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { Video } from '../../models';

@Component({
  selector: 'app-videos',
  templateUrl: './videos.html',
  styleUrl: './videos.scss'
})
export class Videos implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly videos = signal<Video[]>([]);
  protected readonly categories = signal<string[]>([]);
  protected readonly ranking = signal<Video[]>([]);
  protected readonly activeCategory = signal('');

  ngOnInit(): void {
    this.api.videoCategories().subscribe((c) => this.categories.set(c));
    this.api.videoRanking().subscribe((r) => this.ranking.set(r.slice(0, 5)));
    this.load();
  }

  select(category: string): void {
    this.activeCategory.set(category);
    this.load();
  }

  private load(): void {
    this.api.videos(this.activeCategory() || undefined).subscribe((v) => this.videos.set(v));
  }

  formatViews(views: number): string {
    return views >= 10000 ? (views / 10000).toFixed(1) + ' 萬' : String(views);
  }
}
