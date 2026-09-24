import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Video, VideoComment } from '../../models';

@Component({
  selector: 'app-watch',
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './watch.html',
  styleUrl: './watch.scss'
})
export class Watch {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  readonly id = input.required<string>();

  protected readonly video = signal<Video | null>(null);
  protected readonly related = signal<Video[]>([]);
  protected readonly notFound = signal(false);

  protected readonly favorited = signal(false);
  protected readonly comments = signal<VideoComment[]>([]);
  protected commentBody = '';
  protected readonly commentMsg = signal('');

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.video.set(null);
      this.related.set([]);
      this.notFound.set(false);
      this.favorited.set(false);
      this.comments.set([]);
      this.commentMsg.set('');
      this.api.video(id).subscribe({
        next: (v) => {
          this.video.set(v);
          this.loadRelated(v);
          this.loadInteractions(v.id);
        },
        error: () => this.notFound.set(true)
      });
    });
  }

  private loadRelated(v: Video): void {
    this.api.videos(v.category).subscribe((list) =>
      this.related.set(list.filter((x) => x.id !== v.id).slice(0, 6))
    );
  }

  private loadInteractions(videoId: number): void {
    this.api.comments(videoId).subscribe({
      next: (c) => this.comments.set(c),
      error: () => this.comments.set([])
    });
    if (this.auth.member()) {
      this.api.favorited(videoId).subscribe({
        next: (r) => this.favorited.set(r.favorited),
        error: () => {}
      });
      this.api.recordHistory(videoId).subscribe({ error: () => {} });
    }
  }

  toggleFavorite(): void {
    const v = this.video();
    if (!v || !this.auth.member()) {
      return;
    }
    this.api.toggleFavorite(v.id).subscribe({
      next: (r) => this.favorited.set(r.favorited)
    });
  }

  submitComment(): void {
    const v = this.video();
    const body = this.commentBody.trim();
    if (!v || !body) {
      return;
    }
    this.api.addComment(v.id, body).subscribe({
      next: (c) => {
        this.comments.update((list) => [c, ...list]);
        this.commentBody = '';
        this.commentMsg.set('');
      },
      error: (e) => this.commentMsg.set(e.error?.message ?? '留言失敗，請稍後再試')
    });
  }

  removeComment(id: number): void {
    this.api.deleteComment(id).subscribe({
      next: () => this.comments.update((list) => list.filter((c) => c.id !== id)),
      error: () => this.commentMsg.set('刪除失敗')
    });
  }

  canDelete(c: VideoComment): boolean {
    const m = this.auth.member();
    return !!m && (m.id === c.memberId || m.role === 'ADMIN');
  }

  formatViews(views: number): string {
    return views >= 10000 ? (views / 10000).toFixed(1) + ' 萬' : String(views);
  }
}
