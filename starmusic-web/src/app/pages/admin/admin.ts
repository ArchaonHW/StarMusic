import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/api.service';
import { compressImage } from '../../core/media';
import { Article, Banner, Magazine, Member, Post, Product, VideoUpload } from '../../models';
import { environment } from '../../../environments/environment';

type Tab = 'dashboard' | 'posts' | 'videos' | 'banners' | 'news' | 'magazines' | 'products' | 'members';
type PostStatusFilter = '' | Post['status'];

@Component({
  selector: 'app-admin',
  imports: [FormsModule, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './admin.html',
  styleUrl: './admin.scss'
})
export class AdminPage {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  protected readonly staticMode = environment.staticData;
  protected readonly message = signal('');
  protected readonly error = signal('');
  protected readonly tab = signal<Tab>('dashboard');

  readonly tabs: { id: Tab; label: string }[] = [
    { id: 'dashboard', label: '儀表板' },
    { id: 'posts', label: '投稿管理' },
    { id: 'videos', label: '影片審核' },
    { id: 'banners', label: '輪播圖' },
    { id: 'news', label: '新聞' },
    { id: 'magazines', label: '雜誌' },
    { id: 'products', label: '商品' },
    { id: 'members', label: '會員帳務' }
  ];

  readonly postStatuses: { value: PostStatusFilter; label: string }[] = [
    { value: '', label: '全部' },
    { value: 'PENDING', label: '待審核' },
    { value: 'APPROVED', label: '已上架' },
    { value: 'REJECTED', label: '已退回' },
    { value: 'TAKEN_DOWN', label: '已下架' }
  ];

  protected readonly allPosts = signal<Post[]>([]);
  protected readonly allUploads = signal<VideoUpload[]>([]);
  protected readonly banners = signal<Banner[]>([]);
  protected readonly managedNews = signal<Article[]>([]);
  protected readonly managedMagazines = signal<Magazine[]>([]);
  protected readonly managedProducts = signal<Product[]>([]);
  protected readonly allMembers = signal<Member[]>([]);

  // ===== 投稿管理 =====
  protected readonly postStatus = signal<PostStatusFilter>('PENDING');
  protected readonly postKeyword = signal('');
  protected readonly expandedPostId = signal<number | null>(null);
  protected editingPostId: number | null = null;
  protected postEdit = { title: '', category: '', body: '' };

  protected readonly filteredPosts = computed(() => {
    const status = this.postStatus();
    const kw = this.postKeyword().trim().toLowerCase();
    return this.allPosts().filter(
      (p) =>
        (!status || p.status === status) &&
        (!kw || [p.title, p.author, p.category, p.body].some((v) => v?.toLowerCase().includes(kw)))
    );
  });

  protected readonly pendingUploads = computed(() =>
    this.allUploads().filter((u) => u.status === 'PENDING')
  );
  protected readonly approvedUploads = computed(() =>
    this.allUploads().filter((u) => u.status === 'APPROVED')
  );

  protected readonly stats = computed(() => {
    const posts = this.allPosts();
    const count = (s: Post['status']) => posts.filter((p) => p.status === s).length;
    return {
      posts: posts.length,
      pendingPosts: count('PENDING'),
      approvedPosts: count('APPROVED'),
      offlinePosts: count('REJECTED') + count('TAKEN_DOWN'),
      likes: posts.reduce((sum, p) => sum + (p.likeCount ?? 0), 0),
      byType: (['VIDEO', 'AUDIO', 'IMAGE', 'ARTICLE'] as const).map((t) => ({
        type: t,
        count: posts.filter((p) => p.type === t).length
      })),
      pendingUploads: this.pendingUploads().length,
      members: this.allMembers().length,
      activeBanners: this.banners().filter((b) => b.active !== false).length,
      banners: this.banners().length,
      news: this.managedNews().length,
      magazines: this.managedMagazines().length,
      products: this.managedProducts().length
    };
  });

  protected readonly recentPosts = computed(() =>
    [...this.allPosts()].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5)
  );

  protected reviewNote: Record<number, string> = {};
  protected adjustAmount: Record<number, number> = {};
  protected adjustNote: Record<number, string> = {};

  // ===== 輪播圖 =====
  protected editingBannerId: number | null = null;
  protected bannerForm = this.emptyBanner();
  protected readonly bannerUploading = signal(false);

  // ===== 新聞 / 雜誌 / 商品 =====
  protected editingNewsId: number | null = null;
  protected newsForm = this.emptyNews();
  protected editingMagId: number | null = null;
  protected magForm = this.emptyMag();
  protected editingProdId: number | null = null;
  protected prodForm = this.emptyProd();

  constructor() {
    effect(() => {
      if (this.auth.member()?.role === 'ADMIN') {
        this.loadAdmin();
      }
    });
  }

  selectTab(t: Tab): void {
    this.tab.set(t);
    this.message.set('');
    this.error.set('');
  }

  goPosts(status: PostStatusFilter): void {
    this.postStatus.set(status);
    this.selectTab('posts');
  }

  postTypeLabel(type: string): string {
    return { VIDEO: '影片', AUDIO: '音訊', IMAGE: '圖片', ARTICLE: '文章' }[type] ?? type;
  }

  statusLabel(status: string): string {
    return { PENDING: '待審核', APPROVED: '已上架', REJECTED: '已退回', TAKEN_DOWN: '已下架' }[
      status
    ] ?? status;
  }

  statusCount(status: PostStatusFilter): number {
    return status ? this.allPosts().filter((p) => p.status === status).length : this.allPosts().length;
  }

  // ===== 影片審核 =====

  review(id: number, approve: boolean): void {
    const note = this.reviewNote[id];
    this.run(
      approve ? this.api.approveUpload(id, note) : this.api.rejectUpload(id, note),
      approve ? '已通過審核並上架' : '已退回該影片'
    );
  }

  takedownUpload(id: number): void {
    this.run(this.api.takedownUpload(id), '影片已下架');
  }

  // ===== 投稿管理 =====

  togglePreview(id: number): void {
    this.expandedPostId.update((cur) => (cur === id ? null : id));
  }

  reviewPost(id: number, approve: boolean): void {
    this.run(
      this.api.reviewPost(id, approve, this.reviewNote[-id]),
      approve ? '投稿已上架' : '投稿已退回'
    );
  }

  takedownPost(id: number): void {
    this.run(this.api.takedownPost(id), '投稿已下架');
  }

  deletePost(p: Post): void {
    if (confirm(`確定要刪除「${p.title}」？此操作無法復原。`)) {
      this.run(this.api.deletePost(p.id), '投稿已刪除');
    }
  }

  startEditPost(p: Post): void {
    this.editingPostId = p.id;
    this.postEdit = { title: p.title, category: p.category ?? '', body: p.body ?? '' };
  }

  saveEditPost(id: number): void {
    if (!this.postEdit.title.trim()) {
      this.error.set('標題不能空白');
      return;
    }
    this.run(
      this.api.updatePost(id, {
        title: this.postEdit.title.trim(),
        category: this.postEdit.category.trim() || undefined,
        body: this.postEdit.body || undefined
      }),
      '投稿已更新',
      () => (this.editingPostId = null)
    );
  }

  // ===== 輪播圖 =====

  async onBannerFile(event: Event): Promise<void> {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) {
      return;
    }
    this.bannerUploading.set(true);
    try {
      this.bannerForm.imageUrl = await compressImage(file, 1600, 0.85);
    } catch {
      this.error.set('無法讀取圖片');
    } finally {
      this.bannerUploading.set(false);
    }
  }

  editBanner(b: Banner): void {
    this.editingBannerId = b.id;
    this.bannerForm = {
      title: b.title,
      imageUrl: b.imageUrl,
      linkUrl: b.linkUrl,
      description: b.description ?? '',
      sortOrder: b.sortOrder ?? 0,
      active: b.active !== false
    };
  }

  cancelBanner(): void {
    this.editingBannerId = null;
    this.bannerForm = this.emptyBanner();
  }

  saveBanner(): void {
    const f = this.bannerForm;
    if (!f.title.trim() || !f.imageUrl.trim()) {
      this.error.set('請填寫標題並提供圖片');
      return;
    }
    const req = { ...f, title: f.title.trim(), linkUrl: f.linkUrl.trim() || '/' };
    this.run(
      this.editingBannerId
        ? this.api.updateBanner(this.editingBannerId, req)
        : this.api.createBanner(req),
      this.editingBannerId ? '輪播圖已更新' : '輪播圖已新增',
      () => this.cancelBanner()
    );
  }

  toggleBanner(b: Banner): void {
    this.run(
      this.api.updateBanner(b.id, { ...b, active: b.active === false }),
      b.active === false ? '已啟用' : '已停用'
    );
  }

  // 與相鄰項目交換位置後重新編號，只更新排序有變動的項目
  moveBanner(index: number, dir: -1 | 1): void {
    const list = [...this.banners()];
    const j = index + dir;
    if (j < 0 || j >= list.length) {
      return;
    }
    [list[index], list[j]] = [list[j], list[index]];
    const changed = list
      .map((b, i) => ({ b, order: i + 1 }))
      .filter(({ b, order }) => b.sortOrder !== order);
    this.run(
      forkJoin(changed.map(({ b, order }) => this.api.updateBanner(b.id, { ...b, sortOrder: order }))),
      '排序已更新'
    );
  }

  deleteBanner(b: Banner): void {
    if (confirm(`確定要刪除輪播圖「${b.title}」？`)) {
      this.run(this.api.deleteBanner(b.id), '輪播圖已刪除');
    }
  }

  // ===== 新聞 =====

  editNews(n: Article): void {
    this.editingNewsId = n.id;
    this.newsForm = {
      title: n.title,
      category: n.category ?? '',
      summary: n.summary ?? '',
      author: n.author ?? '',
      source: n.source ?? '',
      imageUrl: n.imageUrl ?? '',
      content: n.content ?? '',
      breaking: n.breaking
    };
  }

  cancelNews(): void {
    this.editingNewsId = null;
    this.newsForm = this.emptyNews();
  }

  submitNews(): void {
    const f = this.newsForm;
    if (!f.title.trim()) {
      this.error.set('請填寫新聞標題');
      return;
    }
    const req = {
      title: f.title.trim(),
      category: f.category || undefined,
      summary: f.summary || undefined,
      content: f.content || undefined,
      source: f.source || undefined,
      author: f.author || undefined,
      imageUrl: f.imageUrl || undefined,
      breaking: f.breaking
    };
    this.run(
      this.editingNewsId ? this.api.updateNews(this.editingNewsId, req) : this.api.createNews(req),
      this.editingNewsId ? '新聞已更新' : '新聞已新增',
      () => this.cancelNews()
    );
  }

  deleteNews(n: Article): void {
    if (confirm(`確定要刪除新聞「${n.title}」？`)) {
      this.run(this.api.deleteNewsItem(n.id), '新聞已刪除');
    }
  }

  // ===== 雜誌 =====

  editMagazine(m: Magazine): void {
    this.editingMagId = m.id;
    this.magForm = {
      title: m.title,
      issueNo: m.issueNo ?? '',
      category: m.category ?? '',
      publishDate: m.publishDate ?? '',
      price: m.price,
      cover: m.cover ?? '',
      coverStory: m.coverStory ?? '',
      highlights: (m.highlights ?? []).join('\n'),
      latest: m.latest,
      readUrl: m.readUrl ?? ''
    };
  }

  cancelMagazine(): void {
    this.editingMagId = null;
    this.magForm = this.emptyMag();
  }

  submitMagazine(): void {
    const f = this.magForm;
    if (!f.title.trim()) {
      this.error.set('請填寫雜誌名稱');
      return;
    }
    const highlights = f.highlights
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean);
    const req = {
      title: f.title.trim(),
      issueNo: f.issueNo || undefined,
      category: f.category || undefined,
      publishDate: f.publishDate || undefined,
      price: f.price ?? undefined,
      cover: f.cover || undefined,
      coverStory: f.coverStory || undefined,
      highlights: highlights.length ? highlights : undefined,
      latest: f.latest,
      readUrl: f.readUrl || undefined
    };
    this.run(
      this.editingMagId
        ? this.api.updateMagazine(this.editingMagId, req)
        : this.api.createMagazine(req),
      this.editingMagId ? '雜誌已更新' : '雜誌已新增',
      () => this.cancelMagazine()
    );
  }

  deleteMagazine(m: Magazine): void {
    if (confirm(`確定要刪除雜誌「${m.title}」？`)) {
      this.run(this.api.deleteMagazineItem(m.id), '雜誌已刪除');
    }
  }

  // ===== 商品 =====

  editProduct(p: Product): void {
    this.editingProdId = p.id;
    this.prodForm = {
      name: p.name,
      category: p.category ?? '',
      price: p.price,
      originalPrice: p.originalPrice,
      stock: p.stock,
      image: p.image ?? '',
      description: p.description ?? '',
      buyUrl: p.buyUrl ?? ''
    };
  }

  cancelProduct(): void {
    this.editingProdId = null;
    this.prodForm = this.emptyProd();
  }

  submitProduct(): void {
    const f = this.prodForm;
    if (!f.name.trim()) {
      this.error.set('請填寫商品名稱');
      return;
    }
    const req = {
      name: f.name.trim(),
      category: f.category || undefined,
      price: f.price ?? undefined,
      originalPrice: f.originalPrice ?? undefined,
      stock: f.stock ?? undefined,
      image: f.image || undefined,
      description: f.description || undefined,
      buyUrl: f.buyUrl || undefined
    };
    this.run(
      this.editingProdId
        ? this.api.updateProduct(this.editingProdId, req)
        : this.api.createProduct(req),
      this.editingProdId ? '商品已更新' : '商品已新增',
      () => this.cancelProduct()
    );
  }

  deleteProduct(p: Product): void {
    if (confirm(`確定要刪除商品「${p.name}」？`)) {
      this.run(this.api.deleteProductItem(p.id), '商品已刪除');
    }
  }

  // ===== 會員帳務 =====

  adjust(memberId: number, type: string): void {
    const amount = this.adjustAmount[memberId];
    if (!amount) {
      return;
    }
    this.run(
      this.api.addTransaction(memberId, {
        type,
        amount: type === 'CONSUME' ? -Math.abs(amount) : Math.abs(amount),
        note: this.adjustNote[memberId]
      }),
      '帳務已更新'
    );
  }

  // 執行操作後顯示訊息並重新載入後台資料
  private run<T>(call: Observable<T>, okMessage: string, after?: () => void): void {
    this.error.set('');
    call.subscribe({
      next: () => {
        this.message.set(okMessage);
        after?.();
        this.loadAdmin();
      },
      error: (e) => this.fail(e)
    });
  }

  private fail(e: { status?: number; error?: { message?: string } }): void {
    this.message.set('');
    this.error.set(
      e.error?.message ?? (e.status === 501 ? '靜態展示版不提供此功能（需連接後端主機）' : '操作失敗，請稍後再試')
    );
  }

  private load<T>(call: Observable<T[]>, target: { set(v: T[]): void }): void {
    call.subscribe({ next: (v) => target.set(v), error: () => target.set([]) });
  }

  private loadAdmin(): void {
    this.load(this.api.managePosts(), this.allPosts);
    this.load(this.api.adminUploads(), this.allUploads);
    this.load(this.api.managedBanners(), this.banners);
    this.load(this.api.managedNews(), this.managedNews);
    this.load(this.api.managedMagazines(), this.managedMagazines);
    this.load(this.api.managedProducts(), this.managedProducts);
    this.load(this.api.members(), this.allMembers);
  }

  private emptyBanner() {
    return { title: '', imageUrl: '', linkUrl: '/', description: '', sortOrder: 0, active: true };
  }

  private emptyNews() {
    return {
      title: '', category: '', summary: '', author: '', source: '',
      imageUrl: '', content: '', breaking: false
    };
  }

  private emptyMag() {
    return {
      title: '', issueNo: '', category: '', publishDate: '', price: null as number | null,
      cover: '', coverStory: '', highlights: '', latest: false, readUrl: ''
    };
  }

  private emptyProd() {
    return {
      name: '', category: '', price: null as number | null, originalPrice: null as number | null,
      stock: null as number | null, image: '', description: '', buyUrl: ''
    };
  }
}
