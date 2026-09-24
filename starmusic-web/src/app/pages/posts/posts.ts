import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Post } from '../../models';

@Component({
  selector: 'app-posts',
  imports: [FormsModule, DatePipe, RouterLink],
  templateUrl: './posts.html',
  styleUrl: './posts.scss'
})
export class PostsPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly posts = signal<Post[]>([]);
  protected readonly myPosts = signal<Post[]>([]);
  protected readonly activeType = signal('');
  protected readonly hasMore = signal(false);
  private page = 0;
  private static readonly PAGE_SIZE = 12;

  protected postType: Post['type'] = 'VIDEO';
  protected postTitle = '';
  protected postCategory = '';
  protected postBody = '';
  protected postFile: File | null = null;
  protected readonly uploading = signal(false);
  protected readonly msg = signal('');

  readonly types: { value: Post['type']; label: string; accept: string }[] = [
    { value: 'VIDEO', label: '影片', accept: 'video/*' },
    { value: 'AUDIO', label: '音訊', accept: 'audio/*' },
    { value: 'IMAGE', label: '圖片', accept: 'image/*' },
    { value: 'ARTICLE', label: '文章', accept: 'image/*' }
  ];

  ngOnInit(): void {
    this.load();
    this.loadMine();
  }

  select(type: string): void {
    this.activeType.set(type);
    this.page = 0;
    this.load();
  }

  loadMore(): void {
    this.page++;
    this.load(true);
  }

  acceptFor(type: string): string {
    return this.types.find((t) => t.value === type)?.accept ?? '*/*';
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.postFile = input.files?.[0] ?? null;
  }

  submit(): void {
    const title = this.postTitle.trim();
    if (!title) {
      this.msg.set('請填寫標題');
      return;
    }
    if (this.postType === 'ARTICLE' && !this.postBody.trim()) {
      this.msg.set('文章類型請填寫內文');
      return;
    }
    if (this.postType !== 'ARTICLE' && !this.postFile) {
      this.msg.set('請選擇要上傳的檔案');
      return;
    }
    this.uploading.set(true);
    this.msg.set('');
    this.api
      .createPost(
        {
          type: this.postType,
          title,
          category: this.postCategory || undefined,
          body: this.postBody || undefined
        },
        this.postFile
      )
      .subscribe({
        next: () => {
          this.uploading.set(false);
          this.msg.set('已送出，等待管理員審核');
          this.postTitle = '';
          this.postCategory = '';
          this.postBody = '';
          this.postFile = null;
          this.loadMine();
        },
        error: (e) => {
          this.uploading.set(false);
          this.msg.set(e.error?.message ?? '上傳失敗，請稍後再試');
        }
      });
  }

  typeLabel(type: string): string {
    return this.types.find((t) => t.value === type)?.label ?? type;
  }

  statusLabel(status: string): string {
    return { PENDING: '待審核', APPROVED: '已上架', REJECTED: '已退回', TAKEN_DOWN: '已下架' }[
      status
    ] ?? status;
  }

  takedown(id: number): void {
    this.api.takedownPost(id).subscribe({
      next: () => {
        this.msg.set('已下架');
        this.loadMine();
        this.load();
      },
      error: (e) => this.msg.set(e.error?.message ?? '下架失敗')
    });
  }

  resubmit(id: number): void {
    this.api.resubmitPost(id).subscribe({
      next: () => {
        this.msg.set('已重新送出，等待管理員審核');
        this.loadMine();
      },
      error: (e) => this.msg.set(e.error?.message ?? '重新送審失敗')
    });
  }

  remove(id: number): void {
    if (!confirm('確定要刪除此投稿？此操作無法復原。')) {
      return;
    }
    this.api.deletePost(id).subscribe({
      next: () => {
        this.msg.set('已刪除');
        this.loadMine();
        this.load();
      },
      error: (e) => this.msg.set(e.error?.message ?? '刪除失敗')
    });
  }

  private load(append = false): void {
    this.api
      .posts(this.activeType() || undefined, this.page, PostsPage.PAGE_SIZE)
      .subscribe((p) => {
        this.posts.update((list) => (append ? [...list, ...p.content] : p.content));
        this.hasMore.set(p.number + 1 < p.totalPages);
      });
  }

  private loadMine(): void {
    if (this.auth.member()) {
      this.api.myPosts().subscribe({
        next: (p) => this.myPosts.set(p.content),
        error: () => this.myPosts.set([])
      });
    }
  }
}
