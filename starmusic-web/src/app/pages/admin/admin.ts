import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/api.service';
import { Article, Magazine, Member, Post, Product, VideoUpload } from '../../models';

@Component({
  selector: 'app-admin',
  imports: [FormsModule, DatePipe, DecimalPipe],
  templateUrl: './admin.html',
  styleUrl: './admin.scss'
})
export class AdminPage {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  protected readonly message = signal('');

  protected readonly pendingUploads = signal<VideoUpload[]>([]);
  protected readonly pendingPosts = signal<Post[]>([]);
  protected readonly approvedUploads = signal<VideoUpload[]>([]);
  protected readonly approvedPosts = signal<Post[]>([]);
  protected readonly managedNews = signal<Article[]>([]);
  protected readonly managedMagazines = signal<Magazine[]>([]);
  protected readonly managedProducts = signal<Product[]>([]);
  protected readonly allMembers = signal<Member[]>([]);

  protected reviewNote: Record<number, string> = {};
  protected adjustAmount: Record<number, number> = {};
  protected adjustNote: Record<number, string> = {};

  protected readonly newsForm = {
    title: '',
    category: '',
    summary: '',
    author: '',
    source: '',
    imageUrl: '',
    content: '',
    breaking: false
  };
  protected readonly magForm = {
    title: '',
    issueNo: '',
    category: '',
    publishDate: '',
    price: null as number | null,
    cover: '',
    coverStory: '',
    highlights: '',
    latest: false
  };
  protected readonly prodForm = {
    name: '',
    category: '',
    price: null as number | null,
    originalPrice: null as number | null,
    stock: null as number | null,
    image: '',
    description: ''
  };

  constructor() {
    effect(() => {
      if (this.auth.member()?.role === 'ADMIN') {
        this.loadAdmin();
      }
    });
  }

  review(id: number, approve: boolean): void {
    const note = this.reviewNote[id];
    const call = approve
      ? this.api.approveUpload(id, note)
      : this.api.rejectUpload(id, note);
    call.subscribe(() => {
      this.message.set(approve ? '已通過審核並上架' : '已退回該影片');
      this.loadAdmin();
    });
  }

  reviewPost(id: number, approve: boolean): void {
    const note = this.reviewNote[-id];
    this.api.reviewPost(id, approve, note).subscribe(() => {
      this.message.set(approve ? '投稿已通過上架' : '投稿已退回');
      this.loadAdmin();
    });
  }

  postTypeLabel(type: string): string {
    return { VIDEO: '影片', AUDIO: '音訊', IMAGE: '圖片', ARTICLE: '文章' }[type] ?? type;
  }

  takedownUpload(id: number): void {
    this.api.takedownUpload(id).subscribe(() => {
      this.message.set('影片已下架');
      this.loadAdmin();
    });
  }

  takedownPost(id: number): void {
    this.api.takedownPost(id).subscribe(() => {
      this.message.set('投稿已下架');
      this.loadAdmin();
    });
  }

  submitNews(): void {
    const f = this.newsForm;
    if (!f.title.trim()) {
      this.message.set('請填寫新聞標題');
      return;
    }
    this.api
      .createNews({
        title: f.title.trim(),
        category: f.category || undefined,
        summary: f.summary || undefined,
        content: f.content || undefined,
        source: f.source || undefined,
        author: f.author || undefined,
        imageUrl: f.imageUrl || undefined,
        breaking: f.breaking
      })
      .subscribe(() => {
        this.message.set('新聞已新增');
        Object.assign(f, {
          title: '', category: '', summary: '', author: '', source: '',
          imageUrl: '', content: '', breaking: false
        });
        this.loadAdmin();
      });
  }

  deleteNews(id: number): void {
    this.api.deleteNewsItem(id).subscribe(() => {
      this.message.set('新聞已刪除');
      this.loadAdmin();
    });
  }

  submitMagazine(): void {
    const f = this.magForm;
    if (!f.title.trim()) {
      this.message.set('請填寫雜誌名稱');
      return;
    }
    const highlights = f.highlights
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean);
    this.api
      .createMagazine({
        title: f.title.trim(),
        issueNo: f.issueNo || undefined,
        category: f.category || undefined,
        publishDate: f.publishDate || undefined,
        price: f.price ?? undefined,
        cover: f.cover || undefined,
        coverStory: f.coverStory || undefined,
        highlights: highlights.length ? highlights : undefined,
        latest: f.latest
      })
      .subscribe(() => {
        this.message.set('雜誌已新增');
        Object.assign(f, {
          title: '', issueNo: '', category: '', publishDate: '', price: null,
          cover: '', coverStory: '', highlights: '', latest: false
        });
        this.loadAdmin();
      });
  }

  deleteMagazine(id: number): void {
    this.api.deleteMagazineItem(id).subscribe(() => {
      this.message.set('雜誌已刪除');
      this.loadAdmin();
    });
  }

  submitProduct(): void {
    const f = this.prodForm;
    if (!f.name.trim()) {
      this.message.set('請填寫商品名稱');
      return;
    }
    this.api
      .createProduct({
        name: f.name.trim(),
        category: f.category || undefined,
        price: f.price ?? undefined,
        originalPrice: f.originalPrice ?? undefined,
        stock: f.stock ?? undefined,
        image: f.image || undefined,
        description: f.description || undefined
      })
      .subscribe(() => {
        this.message.set('商品已新增');
        Object.assign(f, {
          name: '', category: '', price: null, originalPrice: null,
          stock: null, image: '', description: ''
        });
        this.loadAdmin();
      });
  }

  deleteProduct(id: number): void {
    this.api.deleteProductItem(id).subscribe(() => {
      this.message.set('商品已刪除');
      this.loadAdmin();
    });
  }

  adjust(memberId: number, type: string): void {
    const amount = this.adjustAmount[memberId];
    if (!amount) {
      return;
    }
    this.api
      .addTransaction(memberId, {
        type,
        amount: type === 'CONSUME' ? -Math.abs(amount) : Math.abs(amount),
        note: this.adjustNote[memberId]
      })
      .subscribe(() => {
        this.message.set('帳務已更新');
        this.loadAdmin();
      });
  }

  private loadAdmin(): void {
    this.api.adminUploads('PENDING').subscribe({
      next: (u) => this.pendingUploads.set(u),
      error: () => this.pendingUploads.set([])
    });
    this.api.adminUploads('APPROVED').subscribe({
      next: (u) => this.approvedUploads.set(u),
      error: () => this.approvedUploads.set([])
    });
    this.api.pendingPosts().subscribe({
      next: (p) => this.pendingPosts.set(p.content),
      error: () => this.pendingPosts.set([])
    });
    this.api.managePosts('APPROVED').subscribe({
      next: (p) => this.approvedPosts.set(p),
      error: () => this.approvedPosts.set([])
    });
    this.api.managedNews().subscribe({
      next: (n) => this.managedNews.set(n),
      error: () => this.managedNews.set([])
    });
    this.api.managedMagazines().subscribe({
      next: (m) => this.managedMagazines.set(m),
      error: () => this.managedMagazines.set([])
    });
    this.api.managedProducts().subscribe({
      next: (p) => this.managedProducts.set(p),
      error: () => this.managedProducts.set([])
    });
    this.api.members().subscribe({
      next: (m) => this.allMembers.set(m),
      error: () => this.allMembers.set([])
    });
  }
}
