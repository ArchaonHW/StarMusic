import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Post } from '../../models';

@Component({
  selector: 'app-posts',
  imports: [FormsModule, DatePipe],
  templateUrl: './posts.html',
  styleUrl: './posts.scss'
})
export class PostsPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly posts = signal<Post[]>([]);
  protected readonly myPosts = signal<Post[]>([]);
  protected readonly activeType = signal('');

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
    this.load();
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
    return { PENDING: '待審核', APPROVED: '已上架', REJECTED: '已退回' }[status] ?? status;
  }

  private load(): void {
    this.api.posts(this.activeType() || undefined).subscribe((p) => this.posts.set(p));
  }

  private loadMine(): void {
    if (this.auth.member()) {
      this.api.myPosts().subscribe({
        next: (p) => this.myPosts.set(p),
        error: () => this.myPosts.set([])
      });
    }
  }
}
