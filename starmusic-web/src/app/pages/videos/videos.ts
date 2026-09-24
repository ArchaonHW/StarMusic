import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Video, VideoHome, VideoUpload } from '../../models';

@Component({
  selector: 'app-videos',
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './videos.html',
  styleUrl: './videos.scss'
})
export class Videos implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly home = signal<VideoHome | null>(null);
  protected readonly videos = signal<Video[]>([]);
  protected readonly categories = signal<string[]>([]);
  protected readonly ranking = signal<Video[]>([]);
  protected readonly activeCategory = signal('');
  protected readonly heroIdx = signal(0);
  private heroTimer?: ReturnType<typeof setInterval>;

  protected readonly myUploads = signal<VideoUpload[]>([]);
  protected readonly uploading = signal(false);
  protected readonly uploadMsg = signal('');
  protected uploadTitle = '';
  protected uploadCategory = '';
  protected uploadDesc = '';
  protected uploadFile: File | null = null;

  ngOnInit(): void {
    this.api.videoCategories().subscribe((c) => this.categories.set(c));
    this.api.videoRanking().subscribe((r) => this.ranking.set(r.slice(0, 5)));
    this.api.videoHome().subscribe((h) => {
      this.home.set(h);
      this.startHero();
    });
    this.loadMyUploads();
  }

  ngOnDestroy(): void {
    clearInterval(this.heroTimer);
  }

  select(category: string): void {
    this.activeCategory.set(category);
    if (!category) {
      this.videos.set([]);
      return;
    }
    if (category === 'VIP') {
      this.api.videos(undefined, true).subscribe((v) => this.videos.set(v));
      return;
    }
    this.api.videos(category).subscribe((v) => this.videos.set(v));
  }

  nextHero(dir: number): void {
    const n = this.home()?.featured.length ?? 0;
    if (n > 1) {
      this.heroIdx.update((i) => (i + dir + n) % n);
    }
  }

  goHero(i: number): void {
    this.heroIdx.set(i);
  }

  private startHero(): void {
    clearInterval(this.heroTimer);
    this.heroTimer = setInterval(() => this.nextHero(1), 6000);
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.uploadFile = input.files?.[0] ?? null;
  }

  submitUpload(): void {
    if (!this.uploadFile || !this.uploadTitle.trim()) {
      this.uploadMsg.set('請填寫標題並選擇影片檔案');
      return;
    }
    this.uploading.set(true);
    this.uploadMsg.set('');
    this.api
      .uploadVideo(this.uploadFile, {
        title: this.uploadTitle.trim(),
        category: this.uploadCategory || undefined,
        description: this.uploadDesc || undefined
      })
      .subscribe({
        next: () => {
          this.uploading.set(false);
          this.uploadMsg.set('上傳成功，等待管理員審核');
          this.uploadTitle = '';
          this.uploadDesc = '';
          this.uploadFile = null;
          this.loadMyUploads();
        },
        error: () => {
          this.uploading.set(false);
          this.uploadMsg.set('上傳失敗，請稍後再試');
        }
      });
  }

  statusLabel(status: string): string {
    return { PENDING: '待審核', APPROVED: '已上架', REJECTED: '已退回' }[status] ?? status;
  }

  private loadMyUploads(): void {
    if (this.auth.member()) {
      this.api.myUploads().subscribe({
        next: (u) => this.myUploads.set(u),
        error: () => this.myUploads.set([])
      });
    }
  }

  formatViews(views: number): string {
    return views >= 10000 ? (views / 10000).toFixed(1) + ' 萬' : String(views);
  }
}
