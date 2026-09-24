import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Article, Channel, Magazine, Product, Video } from '../../models';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly breaking = signal<Article[]>([]);
  protected readonly hotVideos = signal<Video[]>([]);
  protected readonly channels = signal<Channel[]>([]);
  protected readonly latestMagazines = signal<Magazine[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly heroIndex = signal(0);

  ngOnInit(): void {
    this.api.breakingNews().subscribe((a) => this.breaking.set(a));
    this.api.videos(undefined).subscribe((v) =>
      this.hotVideos.set(v.filter((x) => x.hot).slice(0, 4))
    );
    this.api.channels().subscribe((c) => this.channels.set(c.slice(0, 4)));
    this.api.latestMagazines().subscribe((m) => this.latestMagazines.set(m.slice(0, 3)));
    this.api.products().subscribe((p) => this.products.set(p.slice(0, 4)));
  }

  formatViews(views: number): string {
    return views >= 10000 ? (views / 10000).toFixed(1) + ' 萬' : String(views);
  }
}
