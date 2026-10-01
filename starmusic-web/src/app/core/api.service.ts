import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, defer, delay, from, map, of, switchMap, throwError, catchError } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  demoAddBanner,
  demoAddMagazine,
  demoAddNews,
  demoAddPost,
  demoAddPostComment,
  demoAddProduct,
  demoAddUpload,
  demoAllBanners,
  demoAllPosts,
  demoAllUploads,
  demoBanners,
  demoDeleteBanner,
  demoDeleteMagazine,
  demoDeleteNews,
  demoDeletePost,
  demoDeletePostComment,
  demoDeleteProduct,
  demoDeleteUpload,
  demoFindPost,
  demoFindVideo,
  demoLikeCount,
  demoLocalVideos,
  demoMagazines,
  demoMembers,
  demoMyPosts,
  demoMyUploads,
  demoNews,
  demoPostComments,
  demoPostLikes,
  demoProducts,
  demoToggleLike,
  demoUpdateBanner,
  demoUpdateMagazine,
  demoUpdateNews,
  demoUpdatePost,
  demoUpdateProduct,
  demoUpdateUpload
} from './demo-store';
import { compressImage, isImageFile } from './media';
import {
  AccountTransaction,
  Article,
  Banner,
  BannerRequest,
  Channel,
  Magazine,
  Member,
  Page,
  Post,
  PostComment,
  PostQuery,
  Product,
  Program,
  SearchResult,
  Video,
  VideoComment,
  VideoHome,
  VideoUpload,
  WatchHistoryItem
} from '../models';

export type NewsRequest = {
  title: string;
  category?: string;
  summary?: string;
  content?: string;
  source?: string;
  author?: string;
  imageUrl?: string;
  imageUrls?: string[];
  breaking?: boolean;
};

export type MagazineRequest = {
  title: string;
  issueNo?: string;
  cover?: string;
  publishDate?: string;
  price?: number;
  category?: string;
  coverStory?: string;
  highlights?: string[];
  latest?: boolean;
  readUrl?: string;
};

export type ProductRequest = {
  name: string;
  category?: string;
  price?: number;
  originalPrice?: number;
  image?: string;
  rating?: number;
  stock?: number;
  description?: string;
};

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;

  videos(category?: string, vip?: boolean): Observable<Video[]> {
    const req = this.http.get<Video[]>(`${this.base}/videos`, {
      params: this.p({ category, vip })
    });
    if (environment.staticData) {
      return req.pipe(
        map((list) => [
          ...demoLocalVideos().filter(
            (v) => (!category || v.category === category) && (vip !== true || v.vip)
          ),
          ...list
        ])
      );
    }
    return req;
  }

  video(id: number): Observable<Video> {
    if (environment.staticData) {
      const local = demoFindVideo(id);
      if (local) {
        return of(local);
      }
    }
    return this.http.get<Video>(`${this.base}/videos/${id}`);
  }

  videoCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/videos/categories`);
  }

  videoRanking(): Observable<Video[]> {
    const req = this.http.get<Video[]>(`${this.base}/videos/ranking`);
    if (environment.staticData) {
      return req.pipe(map((list) => [...demoLocalVideos(), ...list]));
    }
    return req;
  }

  videoHome(): Observable<VideoHome> {
    const req = this.http.get<VideoHome>(`${this.base}/videos/home`);
    if (environment.staticData) {
      return req.pipe(
        map((h) => {
          const locals = demoLocalVideos();
          return {
            ...h,
            featured: [...locals, ...h.featured],
            ranking: [...locals, ...h.ranking],
            sections: h.sections.map((s) => ({
              ...s,
              videos: [...locals.filter((v) => v.category === s.category), ...s.videos]
            }))
          };
        })
      );
    }
    return req;
  }

  banners(): Observable<Banner[]> {
    if (environment.staticData) {
      return of(demoBanners());
    }
    return this.http.get<Banner[]>(`${this.base}/banners`).pipe(
      catchError(() => of(demoBanners()))
    );
  }

  managedBanners(): Observable<Banner[]> {
    if (environment.staticData) {
      return this.demo(demoAllBanners);
    }
    return this.http.get<Banner[]>(`${this.base}/banners/manage`, {
      headers: this.authHeaders()
    });
  }

  createBanner(req: BannerRequest): Observable<Banner> {
    if (environment.staticData) {
      return this.demo(() => demoAddBanner(req));
    }
    return this.http.post<Banner>(`${this.base}/banners/manage`, req, {
      headers: this.authHeaders()
    });
  }

  updateBanner(id: number, req: BannerRequest): Observable<Banner> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateBanner(id, req)));
    }
    return this.http.put<Banner>(`${this.base}/banners/manage/${id}`, req, {
      headers: this.authHeaders()
    });
  }

  deleteBanner(id: number): Observable<void> {
    if (environment.staticData) {
      return this.demo(() => demoDeleteBanner(id));
    }
    return this.http.delete<void>(`${this.base}/banners/manage/${id}`, {
      headers: this.authHeaders()
    });
  }

  toggleFavorite(videoId: number): Observable<{ favorited: boolean }> {
    return this.http.post<{ favorited: boolean }>(
      `${this.base}/videos/${videoId}/favorite`,
      {},
      { headers: this.authHeaders() }
    );
  }

  favorited(videoId: number): Observable<{ favorited: boolean }> {
    return this.http.get<{ favorited: boolean }>(`${this.base}/videos/${videoId}/favorite`, {
      headers: this.authHeaders()
    });
  }

  myFavorites(): Observable<Video[]> {
    return this.http.get<Video[]>(`${this.base}/videos/favorites/mine`, {
      headers: this.authHeaders()
    });
  }

  recordHistory(videoId: number) {
    return this.http.post(
      `${this.base}/videos/${videoId}/history`,
      {},
      { headers: this.authHeaders() }
    );
  }

  myHistory(): Observable<WatchHistoryItem[]> {
    return this.http.get<WatchHistoryItem[]>(`${this.base}/videos/history/mine`, {
      headers: this.authHeaders()
    });
  }

  comments(videoId: number): Observable<VideoComment[]> {
    return this.http.get<VideoComment[]>(`${this.base}/videos/${videoId}/comments`);
  }

  addComment(videoId: number, body: string): Observable<VideoComment> {
    return this.http.post<VideoComment>(
      `${this.base}/videos/${videoId}/comments`,
      { body },
      { headers: this.authHeaders() }
    );
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/videos/comments/${commentId}`, {
      headers: this.authHeaders()
    });
  }

  channels(): Observable<Channel[]> {
    return this.http.get<Channel[]>(`${this.base}/radio/channels`);
  }

  programs(channelId?: number): Observable<Program[]> {
    return this.http.get<Program[]>(`${this.base}/radio/programs`, {
      params: this.p({ channelId })
    });
  }

  news(category?: string): Observable<Article[]> {
    return this.withLocal(
      this.http.get<Article[]>(`${this.base}/news`, { params: this.p({ category }) }),
      () => demoNews().filter((a) => !category || a.category === category)
    );
  }

  article(id: number): Observable<Article> {
    const local = environment.staticData ? demoNews().find((a) => a.id === id) : undefined;
    return local ? of(local) : this.http.get<Article>(`${this.base}/news/${id}`);
  }

  breakingNews(): Observable<Article[]> {
    return this.withLocal(this.http.get<Article[]>(`${this.base}/news/breaking`), () =>
      demoNews().filter((a) => a.breaking)
    );
  }

  newsCategories(): Observable<string[]> {
    return this.withLocalCategories(this.http.get<string[]>(`${this.base}/news/categories`), () =>
      demoNews().map((a) => a.category)
    );
  }

  managedNews(): Observable<Article[]> {
    if (environment.staticData) {
      return this.demo(demoNews);
    }
    return this.http.get<Article[]>(`${this.base}/news/manage`, {
      headers: this.authHeaders()
    });
  }

  createNews(req: NewsRequest): Observable<Article> {
    if (environment.staticData) {
      return this.demo(() => demoAddNews(this.toArticle(req)));
    }
    return this.http.post<Article>(`${this.base}/news/manage`, req, {
      headers: this.authHeaders()
    });
  }

  updateNews(id: number, req: NewsRequest): Observable<Article> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateNews(id, this.toArticle(req))));
    }
    return this.http.put<Article>(`${this.base}/news/manage/${id}`, req, {
      headers: this.authHeaders()
    });
  }

  deleteNewsItem(id: number): Observable<void> {
    if (environment.staticData) {
      return this.demo(() => demoDeleteNews(id));
    }
    return this.http.delete<void>(`${this.base}/news/manage/${id}`, {
      headers: this.authHeaders()
    });
  }

  magazines(category?: string): Observable<Magazine[]> {
    return this.withLocal(
      this.http.get<Magazine[]>(`${this.base}/magazines`, { params: this.p({ category }) }),
      () => demoMagazines().filter((m) => !category || m.category === category)
    );
  }

  latestMagazines(): Observable<Magazine[]> {
    return this.withLocal(this.http.get<Magazine[]>(`${this.base}/magazines/latest`), () =>
      demoMagazines().filter((m) => m.latest)
    );
  }

  magazineCategories(): Observable<string[]> {
    return this.withLocalCategories(
      this.http.get<string[]>(`${this.base}/magazines/categories`),
      () => demoMagazines().map((m) => m.category)
    );
  }

  managedMagazines(): Observable<Magazine[]> {
    if (environment.staticData) {
      return this.demo(demoMagazines);
    }
    return this.http.get<Magazine[]>(`${this.base}/magazines/manage`, {
      headers: this.authHeaders()
    });
  }

  createMagazine(req: MagazineRequest): Observable<Magazine> {
    if (environment.staticData) {
      return this.demo(() => demoAddMagazine(this.toMagazine(req)));
    }
    return this.http.post<Magazine>(`${this.base}/magazines/manage`, req, {
      headers: this.authHeaders()
    });
  }

  updateMagazine(id: number, req: MagazineRequest): Observable<Magazine> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateMagazine(id, this.toMagazine(req))));
    }
    return this.http.put<Magazine>(`${this.base}/magazines/manage/${id}`, req, {
      headers: this.authHeaders()
    });
  }

  deleteMagazineItem(id: number): Observable<void> {
    if (environment.staticData) {
      return this.demo(() => demoDeleteMagazine(id));
    }
    return this.http.delete<void>(`${this.base}/magazines/manage/${id}`, {
      headers: this.authHeaders()
    });
  }

  products(category?: string): Observable<Product[]> {
    return this.withLocal(
      this.http.get<Product[]>(`${this.base}/products`, { params: this.p({ category }) }),
      () => demoProducts().filter((p) => !category || p.category === category)
    );
  }

  productCategories(): Observable<string[]> {
    return this.withLocalCategories(
      this.http.get<string[]>(`${this.base}/products/categories`),
      () => demoProducts().map((p) => p.category)
    );
  }

  managedProducts(): Observable<Product[]> {
    if (environment.staticData) {
      return this.demo(demoProducts);
    }
    return this.http.get<Product[]>(`${this.base}/products/manage`, {
      headers: this.authHeaders()
    });
  }

  createProduct(req: ProductRequest): Observable<Product> {
    if (environment.staticData) {
      return this.demo(() => demoAddProduct(this.toProduct(req)));
    }
    return this.http.post<Product>(`${this.base}/products/manage`, req, {
      headers: this.authHeaders()
    });
  }

  updateProduct(id: number, req: ProductRequest): Observable<Product> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateProduct(id, this.toProduct(req))));
    }
    return this.http.put<Product>(`${this.base}/products/manage/${id}`, req, {
      headers: this.authHeaders()
    });
  }

  deleteProductItem(id: number): Observable<void> {
    if (environment.staticData) {
      return this.demo(() => demoDeleteProduct(id));
    }
    return this.http.delete<void>(`${this.base}/products/manage/${id}`, {
      headers: this.authHeaders()
    });
  }

  search(q: string): Observable<SearchResult> {
    return this.http.get<SearchResult>(`${this.base}/search`, { params: this.p({ q }) });
  }

  uploadVideo(file: File, meta: { title: string; category?: string; description?: string }) {
    if (environment.staticData) {
      return this.demo(() => demoAddUpload(file, meta));
    }
    const fd = new FormData();
    fd.append('file', file);
    fd.append('title', meta.title);
    if (meta.category) {
      fd.append('category', meta.category);
    }
    if (meta.description) {
      fd.append('description', meta.description);
    }
    return this.http.post<VideoUpload>(`${this.base}/videos/uploads`, fd, {
      headers: this.authHeaders()
    });
  }

  myUploads(): Observable<VideoUpload[]> {
    if (environment.staticData) {
      return this.demo(demoMyUploads);
    }
    return this.http.get<VideoUpload[]>(`${this.base}/videos/uploads/mine`, {
      headers: this.authHeaders()
    });
  }

  adminUploads(status?: string): Observable<VideoUpload[]> {
    if (environment.staticData) {
      return this.demo(() => demoAllUploads(status));
    }
    return this.http.get<VideoUpload[]>(`${this.base}/videos/uploads`, {
      params: this.p({ status }),
      headers: this.authHeaders()
    });
  }

  approveUpload(id: number, note?: string): Observable<VideoUpload> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateUpload(id, {
        status: 'APPROVED',
        reviewNote: note ?? '',
        reviewedAt: new Date().toISOString()
      })));
    }
    return this.http.post<VideoUpload>(
      `${this.base}/videos/uploads/${id}/approve`,
      { note },
      { headers: this.authHeaders() }
    );
  }

  rejectUpload(id: number, note?: string): Observable<VideoUpload> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateUpload(id, {
        status: 'REJECTED',
        reviewNote: note ?? '',
        reviewedAt: new Date().toISOString()
      })));
    }
    return this.http.post<VideoUpload>(
      `${this.base}/videos/uploads/${id}/reject`,
      { note },
      { headers: this.authHeaders() }
    );
  }

  updateUpload(
    id: number,
    meta: { title: string; category?: string; description?: string }
  ): Observable<VideoUpload> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateUpload(id, meta)));
    }
    return this.http.put<VideoUpload>(`${this.base}/videos/uploads/${id}`, meta, {
      headers: this.authHeaders()
    });
  }

  takedownUpload(id: number): Observable<VideoUpload> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateUpload(id, { status: 'TAKEN_DOWN' })));
    }
    return this.http.post<VideoUpload>(
      `${this.base}/videos/uploads/${id}/takedown`,
      {},
      { headers: this.authHeaders() }
    );
  }

  resubmitUpload(id: number): Observable<VideoUpload> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdateUpload(id, {
        status: 'PENDING',
        reviewNote: '',
        reviewedAt: null
      })));
    }
    return this.http.post<VideoUpload>(
      `${this.base}/videos/uploads/${id}/resubmit`,
      {},
      { headers: this.authHeaders() }
    );
  }

  deleteUpload(id: number): Observable<void> {
    if (environment.staticData) {
      return this.demo(() => demoDeleteUpload(id));
    }
    return this.http.delete<void>(`${this.base}/videos/uploads/${id}`, {
      headers: this.authHeaders()
    });
  }

  members(): Observable<Member[]> {
    if (environment.staticData) {
      return this.demo(demoMembers);
    }
    return this.http.get<Member[]>(`${this.base}/members`, { headers: this.authHeaders() });
  }

  transactions(memberId: number): Observable<AccountTransaction[]> {
    return this.http.get<AccountTransaction[]>(`${this.base}/members/${memberId}/transactions`, {
      headers: this.authHeaders()
    });
  }

  addTransaction(
    memberId: number,
    req: { type: string; amount: number; note?: string }
  ): Observable<AccountTransaction> {
    return this.http.post<AccountTransaction>(
      `${this.base}/members/${memberId}/transactions`,
      req,
      { headers: this.authHeaders() }
    );
  }

  posts(query: PostQuery = {}, page = 0, size = 12): Observable<Page<Post>> {
    if (environment.staticData) {
      return this.staticPosts().pipe(
        map((list) => this.toPage(this.filterPosts(list, query), page, size))
      );
    }
    return this.http.get<Page<Post>>(`${this.base}/posts`, {
      params: this.p({ ...query, page, size })
    });
  }

  postCategories(): Observable<string[]> {
    if (environment.staticData) {
      return this.staticPosts().pipe(
        map((list) => [...new Set(list.map((p) => p.category).filter(Boolean))].sort())
      );
    }
    return this.http.get<string[]>(`${this.base}/posts/categories`);
  }

  // 靜態模式：快照中的內建投稿 + 本機投稿，於前端完成篩選、排序與分頁
  private staticPosts(): Observable<Post[]> {
    return this.http
      .get<Page<Post>>(`${this.base}/posts`, { params: this.p({ page: 0, size: 12 }) })
      .pipe(
        map((p) => p.content),
        catchError(() => of([] as Post[])),
        map((remote) => [
          ...demoAllPosts('APPROVED'),
          ...remote
            .filter((r) => !demoFindPost(r.id))
            .map((r) => ({ ...r, likeCount: demoLikeCount(r.id) }))
        ])
      );
  }

  private filterPosts(list: Post[], q: PostQuery): Post[] {
    const kw = q.q?.trim().toLowerCase();
    const has = (v: string | null | undefined) => !!v && v.toLowerCase().includes(kw!);
    return list
      .filter((p) => !q.type || p.type === q.type)
      .filter((p) => !q.category || p.category === q.category)
      .filter((p) => !kw || has(p.title) || has(p.author) || has(p.body))
      .sort((a, b) =>
        q.sort === 'likes' && b.likeCount !== a.likeCount
          ? b.likeCount - a.likeCount
          : b.createdAt.localeCompare(a.createdAt)
      );
  }

  createPost(
    meta: { type: Post['type']; title: string; category?: string; body?: string },
    file?: File | null
  ) {
    if (environment.staticData) {
      // 靜態空間無法存檔：圖片壓縮後以 data URL 存在瀏覽器，影音檔僅記錄檔名
      const media = isImageFile(file) ? compressImage(file) : Promise.resolve(null);
      return from(media).pipe(switchMap((url) => this.demo(() => demoAddPost(meta, file, url))));
    }
    const fd = new FormData();
    fd.append('type', meta.type);
    fd.append('title', meta.title);
    if (meta.category) {
      fd.append('category', meta.category);
    }
    if (meta.body) {
      fd.append('body', meta.body);
    }
    if (file) {
      fd.append('file', file);
    }
    return this.http.post<Post>(`${this.base}/posts`, fd, { headers: this.authHeaders() });
  }

  myPosts(page = 0, size = 12): Observable<Page<Post>> {
    if (environment.staticData) {
      return this.demo(() => this.toPage(demoMyPosts(), page, size));
    }
    return this.http.get<Page<Post>>(`${this.base}/posts/mine`, {
      headers: this.authHeaders(),
      params: this.p({ page, size })
    });
  }

  pendingPosts(page = 0, size = 20): Observable<Page<Post>> {
    if (environment.staticData) {
      return this.demo(() => this.toPage(demoAllPosts('PENDING'), page, size));
    }
    return this.http.get<Page<Post>>(`${this.base}/posts/pending`, {
      headers: this.authHeaders(),
      params: this.p({ page, size })
    });
  }

  reviewPost(id: number, approve: boolean, note?: string): Observable<Post> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdatePost(id, {
        status: approve ? 'APPROVED' : 'REJECTED',
        reviewNote: note ?? '',
        reviewedAt: new Date().toISOString()
      })));
    }
    return this.http.post<Post>(
      `${this.base}/posts/${id}/${approve ? 'approve' : 'reject'}`,
      { note },
      { headers: this.authHeaders() }
    );
  }

  post(id: number): Observable<Post> {
    const req = this.http.get<Post>(`${this.base}/posts/${id}`, {
      headers: this.authHeaders()
    });
    if (environment.staticData) {
      const local = demoFindPost(id);
      return local ? of(local) : req.pipe(map((p) => ({ ...p, likeCount: demoLikeCount(p.id) })));
    }
    return req;
  }

  postLikes(id: number): Observable<{ likes: number; liked: boolean }> {
    if (environment.staticData) {
      return of(demoPostLikes(id));
    }
    return this.http.get<{ likes: number; liked: boolean }>(
      `${this.base}/posts/${id}/likes`,
      { headers: this.authHeaders() }
    );
  }

  likePost(id: number): Observable<{ liked: boolean; likes: number }> {
    if (environment.staticData) {
      return this.demo(() => demoToggleLike(id));
    }
    return this.http.post<{ liked: boolean; likes: number }>(
      `${this.base}/posts/${id}/like`,
      {},
      { headers: this.authHeaders() }
    );
  }

  postComments(id: number): Observable<PostComment[]> {
    const req = this.http.get<PostComment[]>(`${this.base}/posts/${id}/comments`, {
      headers: this.authHeaders()
    });
    if (environment.staticData) {
      return req.pipe(
        catchError(() => of([] as PostComment[])),
        map((remote) =>
          [...demoPostComments(id), ...remote].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
        )
      );
    }
    return req;
  }

  addPostComment(id: number, body: string): Observable<PostComment> {
    if (environment.staticData) {
      return this.demo(() => demoAddPostComment(id, body));
    }
    return this.http.post<PostComment>(
      `${this.base}/posts/${id}/comments`,
      { body },
      { headers: this.authHeaders() }
    );
  }

  deletePostComment(commentId: number): Observable<void> {
    if (environment.staticData) {
      return this.demo(() => demoDeletePostComment(commentId));
    }
    return this.http.delete<void>(`${this.base}/posts/comments/${commentId}`, {
      headers: this.authHeaders()
    });
  }

  managePosts(status?: string): Observable<Post[]> {
    if (environment.staticData) {
      return this.demo(() => demoAllPosts(status));
    }
    return this.http.get<Post[]>(`${this.base}/posts/manage`, {
      params: this.p({ status }),
      headers: this.authHeaders()
    });
  }

  updatePost(
    id: number,
    meta: { title: string; category?: string; body?: string }
  ): Observable<Post> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdatePost(id, meta)));
    }
    return this.http.put<Post>(`${this.base}/posts/${id}`, meta, { headers: this.authHeaders() });
  }

  takedownPost(id: number): Observable<Post> {
    if (environment.staticData) {
      return this.demo(() => this.requireUpdate(demoUpdatePost(id, { status: 'TAKEN_DOWN' })));
    }
    return this.http.post<Post>(
      `${this.base}/posts/${id}/takedown`,
      {},
      { headers: this.authHeaders() }
    );
  }

  resubmitPost(id: number): Observable<Post> {
    if (environment.staticData) {
      // 靜態模式沒有審核者，重新送出即重新上架
      return this.demo(() => this.requireUpdate(demoUpdatePost(id, {
        status: 'APPROVED',
        reviewNote: '',
        reviewedAt: new Date().toISOString()
      })));
    }
    return this.http.post<Post>(
      `${this.base}/posts/${id}/resubmit`,
      {},
      { headers: this.authHeaders() }
    );
  }

  deletePost(id: number): Observable<void> {
    if (environment.staticData) {
      return this.demo(() => demoDeletePost(id));
    }
    return this.http.delete<void>(`${this.base}/posts/${id}`, {
      headers: this.authHeaders()
    });
  }

  register(req: {
    username: string;
    password: string;
    nickname?: string;
    email?: string;
  }): Observable<Member> {
    return this.http.post<Member>(`${this.base}/members/register`, req);
  }

  login(username: string, password: string) {
    return this.http.post<{ token: string; member: Member }>(`${this.base}/members/login`, {
      username,
      password
    });
  }

  me(): Observable<Member> {
    return this.http.get<Member>(`${this.base}/members/me`, { headers: this.authHeaders() });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.base}/members/logout`, {}, {
      headers: this.authHeaders()
    });
  }

  // ===== 靜態展示模式：localStorage 模擬寫入 =====

  private demo<T>(fn: () => T): Observable<T> {
    if (!localStorage.getItem('star-member')) {
      return throwError(() => new HttpErrorResponse({ status: 401 }));
    }
    return defer(() => {
      try {
        return of(fn());
      } catch (e) {
        return throwError(() =>
          e instanceof HttpErrorResponse
            ? e
            : new HttpErrorResponse({
                status: 507,
                error: { message: '瀏覽器儲存空間不足，請改用較小的圖片或刪除舊內容' }
              })
        );
      }
    }).pipe(delay(300));
  }

  private requireUpdate<T>(item: T | undefined): T {
    if (!item) {
      throw new HttpErrorResponse({ status: 404 });
    }
    return item;
  }

  // 靜態模式：把後台在本機新增的內容合併到快照資料前面
  private withLocal<T>(req: Observable<T[]>, local: () => T[]): Observable<T[]> {
    if (!environment.staticData) {
      return req;
    }
    return req.pipe(
      catchError(() => of([] as T[])),
      map((list) => [...local(), ...list])
    );
  }

  private withLocalCategories(req: Observable<string[]>, local: () => string[]) {
    return this.withLocal(req, local).pipe(
      map((list) => [...new Set(list.filter(Boolean))])
    );
  }

  private toArticle(r: NewsRequest): Omit<Article, 'id'> {
    return {
      title: r.title,
      category: r.category ?? '',
      summary: r.summary ?? '',
      content: r.content ?? '',
      source: r.source ?? '',
      author: r.author ?? '',
      imageUrl: r.imageUrl ?? '',
      imageUrls: r.imageUrls ?? [],
      publishedAt: new Date().toISOString(),
      breaking: !!r.breaking
    };
  }

  private toMagazine(r: MagazineRequest): Omit<Magazine, 'id'> {
    return {
      title: r.title,
      issueNo: r.issueNo ?? '',
      cover: r.cover ?? '',
      publishDate: r.publishDate ?? '',
      price: r.price ?? 0,
      category: r.category ?? '',
      coverStory: r.coverStory ?? '',
      highlights: r.highlights ?? [],
      latest: !!r.latest,
      readUrl: r.readUrl ?? ''
    };
  }

  private toProduct(r: ProductRequest): Omit<Product, 'id'> {
    return {
      name: r.name,
      category: r.category ?? '',
      price: r.price ?? 0,
      originalPrice: r.originalPrice ?? 0,
      image: r.image ?? '',
      rating: r.rating ?? 0,
      stock: r.stock ?? 0,
      description: r.description ?? ''
    };
  }

  private toPage<T>(list: T[], page: number, size: number): Page<T> {
    const start = page * size;
    return {
      content: list.slice(start, start + size),
      totalElements: list.length,
      totalPages: Math.max(1, Math.ceil(list.length / size)),
      number: page,
      size
    };
  }

  private authHeaders(): HttpHeaders {
    const token = localStorage.getItem('star-token');
    return token
      ? new HttpHeaders().set('Authorization', `Bearer ${token}`)
      : new HttpHeaders();
  }

  private p(params: Record<string, unknown>): HttpParams {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return httpParams;
  }
}
