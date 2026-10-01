import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Post, PostQuery } from '../../models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-posts',
  imports: [FormsModule, DatePipe, RouterLink],
  templateUrl: './posts.html',
  styleUrl: './posts.scss'
})
export class PostsPage implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly posts = signal<Post[]>([]);
  protected readonly myPosts = signal<Post[]>([]);
  protected readonly categories = signal<string[]>([]);
  protected readonly activeType = signal('');
  protected readonly hasMore = signal(false);
  protected readonly total = signal(0);
  private page = 0;
  private static readonly PAGE_SIZE = 12;

  protected keyword = '';
  protected filterCategory = '';
  protected sort: 'latest' | 'likes' = 'latest';

  protected postType: Post['type'] = 'VIDEO';
  protected postTitle = '';
  protected postCategory = '';
  protected postBody = '';
  protected postFile: File | null = null;
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly previewKind = signal<'video' | 'audio' | 'image' | null>(null);
  protected readonly uploading = signal(false);
  protected readonly msg = signal('');
  protected readonly staticMode = environment.staticData;
  protected editingId: number | null = null;
  protected editTitle = '';
  protected editCategory = '';
  protected editBody = '';

  readonly types: { value: Post['type']; label: string; accept: string }[] = [
    { value: 'VIDEO', label: '影片', accept: 'video/*' },
    { value: 'AUDIO', label: '音訊', accept: 'audio/*' },
    { value: 'IMAGE', label: '圖片', accept: 'image/*' },
    { value: 'ARTICLE', label: '文章', accept: 'image/*' }
  ];

  ngOnInit(): void {
    this.load();
    this.loadMine();
    this.loadCategories();
  }

  ngOnDestroy(): void {
    this.clearPreview();
  }

  select(type: string): void {
    this.activeType.set(type);
    this.applyFilters();
  }

  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  resetFilters(): void {
    this.keyword = '';
    this.filterCategory = '';
    this.sort = 'latest';
    this.activeType.set('');
    this.applyFilters();
  }

  loadMore(): void {
    this.page++;
    this.load(true);
  }

  acceptFor(type: string): string {
    return this.types.find((t) => t.value === type)?.accept ?? '*/*';
  }

  onTypeChange(): void {
    this.postFile = null;
    this.clearPreview();
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.postFile = input.files?.[0] ?? null;
    this.clearPreview();
    if (this.postFile) {
      const t = this.postFile.type;
      this.previewKind.set(
        t.startsWith('video/') ? 'video' : t.startsWith('audio/') ? 'audio' : t.startsWith('image/') ? 'image' : null
      );
      this.previewUrl.set(URL.createObjectURL(this.postFile));
    }
  }

  // 靜態展示版只能保存圖片（壓縮後存在瀏覽器），影音檔不會保存
  mediaNotSaved(): boolean {
    return this.staticMode && (this.postType === 'VIDEO' || this.postType === 'AUDIO');
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
          category: this.postCategory.trim() || undefined,
          body: this.postBody || undefined
        },
        this.postFile
      )
      .subscribe({
        next: () => {
          this.uploading.set(false);
          this.msg.set(this.staticMode ? '投稿成功，已發佈到列表' : '已送出，等待管理員審核');
          this.postTitle = '';
          this.postCategory = '';
          this.postBody = '';
          this.postFile = null;
          this.clearPreview();
          this.loadMine();
          this.loadCategories();
          this.applyFilters();
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

  typeIcon(type: string): string {
    return { VIDEO: '🎬', AUDIO: '🎵', IMAGE: '🖼', ARTICLE: '📝' }[type] ?? '📄';
  }

  statusLabel(status: string): string {
    return { PENDING: '待審核', APPROVED: '已上架', REJECTED: '已退回', TAKEN_DOWN: '已下架' }[
      status
    ] ?? status;
  }

  startEdit(p: Post): void {
    this.editingId = p.id;
    this.editTitle = p.title;
    this.editCategory = p.category;
    this.editBody = p.body;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(p: Post): void {
    if (!this.editTitle.trim()) {
      this.msg.set('標題不能空白');
      return;
    }
    this.api
      .updatePost(p.id, {
        title: this.editTitle.trim(),
        category: this.editCategory.trim() || undefined,
        body: this.editBody || undefined
      })
      .subscribe({
        next: (r) => {
          this.editingId = null;
          this.msg.set(
            r.status === 'PENDING' && p.status === 'APPROVED' ? '已更新，修改後需重新審核' : '已更新'
          );
          this.refreshAll();
        },
        error: (e) => this.msg.set(e.error?.message ?? '更新失敗')
      });
  }

  takedown(id: number): void {
    this.api.takedownPost(id).subscribe({
      next: () => {
        this.msg.set('已下架');
        this.refreshAll();
      },
      error: (e) => this.msg.set(e.error?.message ?? '下架失敗')
    });
  }

  resubmit(id: number): void {
    this.api.resubmitPost(id).subscribe({
      next: () => {
        this.msg.set(this.staticMode ? '已重新上架' : '已重新送出，等待管理員審核');
        this.refreshAll();
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
        this.refreshAll();
      },
      error: (e) => this.msg.set(e.error?.message ?? '刪除失敗')
    });
  }

  private refreshAll(): void {
    this.loadMine();
    this.loadCategories();
    this.applyFilters();
  }

  private clearPreview(): void {
    const url = this.previewUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.previewUrl.set(null);
    this.previewKind.set(null);
  }

  private query(): PostQuery {
    return {
      type: this.activeType() || undefined,
      q: this.keyword.trim() || undefined,
      category: this.filterCategory || undefined,
      sort: this.sort
    };
  }

  private load(append = false): void {
    this.api.posts(this.query(), this.page, PostsPage.PAGE_SIZE).subscribe((p) => {
      this.posts.update((list) => (append ? [...list, ...p.content] : p.content));
      this.hasMore.set(p.number + 1 < p.totalPages);
      this.total.set(p.totalElements);
    });
  }

  private loadCategories(): void {
    this.api.postCategories().subscribe({
      next: (c) => this.categories.set(c),
      error: () => this.categories.set([])
    });
  }

  private loadMine(): void {
    if (this.auth.member()) {
      this.api.myPosts(0, 50).subscribe({
        next: (p) => this.myPosts.set(p.content),
        error: () => this.myPosts.set([])
      });
    }
  }
}
