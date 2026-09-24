import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Post, PostComment } from '../../models';

@Component({
  selector: 'app-post-detail',
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './post-detail.html',
  styleUrl: './post-detail.scss'
})
export class PostDetail {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  readonly id = input.required<string>();

  protected readonly post = signal<Post | null>(null);
  protected readonly comments = signal<PostComment[]>([]);
  protected readonly liked = signal(false);
  protected readonly likeCount = signal(0);
  protected readonly notFound = signal(false);
  protected commentBody = '';
  protected readonly msg = signal('');

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.post.set(null);
      this.comments.set([]);
      this.notFound.set(false);
      this.msg.set('');
      this.api.post(id).subscribe({
        next: (p) => {
          this.post.set(p);
          this.likeCount.set(p.likeCount);
          this.loadExtras(p.id);
        },
        error: () => this.notFound.set(true)
      });
    });
  }

  private loadExtras(postId: number): void {
    this.api.postComments(postId).subscribe({
      next: (c) => this.comments.set(c),
      error: () => this.comments.set([])
    });
    if (this.auth.member()) {
      this.api.postLikes(postId).subscribe({
        next: (r) => {
          this.liked.set(r.liked);
          this.likeCount.set(r.likes);
        },
        error: () => {}
      });
    }
  }

  toggleLike(): void {
    const p = this.post();
    if (!p || !this.auth.member()) {
      this.msg.set('請先登入會員');
      return;
    }
    this.api.likePost(p.id).subscribe({
      next: (r) => {
        this.liked.set(r.liked);
        this.likeCount.set(r.likes);
      },
      error: (e) => this.msg.set(e.error?.message ?? '操作失敗')
    });
  }

  submitComment(): void {
    const p = this.post();
    const body = this.commentBody.trim();
    if (!p || !body) {
      return;
    }
    this.api.addPostComment(p.id, body).subscribe({
      next: (c) => {
        this.comments.update((list) => [c, ...list]);
        this.commentBody = '';
        this.msg.set('');
      },
      error: (e) => this.msg.set(e.error?.message ?? '留言失敗，請稍後再試')
    });
  }

  removeComment(id: number): void {
    this.api.deletePostComment(id).subscribe({
      next: () => this.comments.update((list) => list.filter((c) => c.id !== id)),
      error: () => this.msg.set('刪除失敗')
    });
  }

  canDelete(c: PostComment): boolean {
    const m = this.auth.member();
    return !!m && (m.id === c.memberId || m.role === 'ADMIN');
  }

  canManage(): boolean {
    const m = this.auth.member();
    const p = this.post();
    return !!m && !!p && (m.username === p.author || m.role === 'ADMIN');
  }

  takedown(): void {
    const p = this.post();
    if (!p) {
      return;
    }
    this.api.takedownPost(p.id).subscribe({
      next: (r) => this.post.set(r),
      error: (e) => this.msg.set(e.error?.message ?? '下架失敗')
    });
  }

  resubmit(): void {
    const p = this.post();
    if (!p) {
      return;
    }
    this.api.resubmitPost(p.id).subscribe({
      next: (r) => {
        this.post.set(r);
        this.msg.set('已重新送出，等待管理員審核');
      },
      error: (e) => this.msg.set(e.error?.message ?? '重新送審失敗')
    });
  }

  remove(): void {
    const p = this.post();
    if (!p || !confirm('確定要刪除此投稿？此操作無法復原。')) {
      return;
    }
    this.api.deletePost(p.id).subscribe({
      next: () => history.back(),
      error: (e) => this.msg.set(e.error?.message ?? '刪除失敗')
    });
  }

  typeLabel(type: string): string {
    return { VIDEO: '影片', AUDIO: '音訊', IMAGE: '圖片', ARTICLE: '文章' }[type] ?? type;
  }

  statusLabel(status: string): string {
    return { PENDING: '待審核', APPROVED: '已上架', REJECTED: '已退回', TAKEN_DOWN: '已下架' }[
      status
    ] ?? status;
  }
}
