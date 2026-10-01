import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Article, Banner, Channel, Magazine, Product, Video } from '../../models';
import { environment } from '../../../environments/environment';
import { demoLocalVideos } from '../../core/demo-store';

interface TvNewsStation {
  id: string;
  name: string;
  tag: string;
  slogan: string;
  url: string;
}

@Component({
  selector: 'app-home',
  imports: [RouterLink, DatePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);

  protected readonly breaking = signal<Article[]>([]);
  protected readonly hotVideos = signal<Video[]>([]);
  protected readonly channels = signal<Channel[]>([]);
  protected readonly latestMagazines = signal<Magazine[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly latestNews = signal<Article[]>([]);
  protected readonly banners = signal<Banner[]>([]);
  protected readonly bannerIndex = signal(0);
  private bannerTimer?: ReturnType<typeof setInterval>;

  // 外部電視新聞台：連到各台官方直播頁（非本站 API，純靜態版也可用）
  protected readonly tvNewsStations: TvNewsStation[] = [
    {
      id: 'tvbs',
      name: 'TVBS 新聞台',
      tag: '55 台 · 56 台',
      slogan: '真實、信賴，全台新聞流量第一品牌',
      url: 'https://news.tvbs.com.tw/live/news4live/69339'
    },
    {
      id: 'cti',
      name: '中天新聞',
      tag: 'Cti News',
      slogan: '監督的力量，24 小時新聞直播',
      url: 'https://ctinews.com/live'
    },
    {
      id: 'setn',
      name: '三立新聞台',
      tag: 'SETN',
      slogan: '即時掌握國內外大小事',
      url: 'https://live.setn.com/Channel/2'
    }
  ];

  ngOnInit(): void {
    this.api.breakingNews().subscribe((a) => this.breaking.set(a));
    this.api.banners().subscribe((b) => {
      this.banners.set(b);
      this.bannerIndex.set(0); // 確保從第一個開始
      if (b.length > 0) {
        this.bannerTimer = setInterval(
          () => this.bannerIndex.update((i) => (i + 1) % b.length),
          5000
        );
      }
    });
    this.api.videoHome().subscribe((h) => {
      this.hotVideos.set(
        environment.staticData ? demoLocalVideos().slice(0, 8) : h.ranking.slice(0, 8)
      );
    });
    this.api.channels().subscribe((c) => this.channels.set(c.slice(0, 8)));
    this.api.latestMagazines().subscribe((m) => this.latestMagazines.set(m.slice(0, 6)));
    this.api.news().subscribe((n) => this.latestNews.set(n.slice(0, 6)));
    this.api.products().subscribe((p) => this.products.set(p.slice(0, 8)));
  }

  ngOnDestroy(): void {
    clearInterval(this.bannerTimer);
  }

  goBanner(i: number): void {
    this.bannerIndex.set(i);
  }

  formatViews(views: number): string {
    return views >= 10000 ? (views / 10000).toFixed(1) + ' 萬' : String(views);
  }
}
