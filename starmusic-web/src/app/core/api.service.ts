import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AccountTransaction,
  Article,
  Channel,
  Magazine,
  Member,
  Page,
  Post,
  PostComment,
  Product,
  Program,
  SearchResult,
  Video,
  VideoComment,
  VideoHome,
  VideoUpload,
  WatchHistoryItem
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;

  videos(category?: string, vip?: boolean): Observable<Video[]> {
    return this.http.get<Video[]>(`${this.base}/videos`, {
      params: this.p({ category, vip })
    });
  }

  video(id: number): Observable<Video> {
    return this.http.get<Video>(`${this.base}/videos/${id}`);
  }

  videoCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/videos/categories`);
  }

  videoRanking(): Observable<Video[]> {
    return this.http.get<Video[]>(`${this.base}/videos/ranking`);
  }

  videoHome(): Observable<VideoHome> {
    return this.http.get<VideoHome>(`${this.base}/videos/home`);
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
    return this.http.get<Article[]>(`${this.base}/news`, { params: this.p({ category }) });
  }

  article(id: number): Observable<Article> {
    return this.http.get<Article>(`${this.base}/news/${id}`);
  }

  breakingNews(): Observable<Article[]> {
    return this.http.get<Article[]>(`${this.base}/news/breaking`);
  }

  newsCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/news/categories`);
  }

  managedNews(): Observable<Article[]> {
    return this.http.get<Article[]>(`${this.base}/news/manage`, {
      headers: this.authHeaders()
    });
  }

  createNews(req: {
    title: string;
    category?: string;
    summary?: string;
    content?: string;
    source?: string;
    author?: string;
    imageUrl?: string;
    imageUrls?: string[];
    breaking?: boolean;
  }): Observable<Article> {
    return this.http.post<Article>(`${this.base}/news/manage`, req, {
      headers: this.authHeaders()
    });
  }

  deleteNewsItem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/news/manage/${id}`, {
      headers: this.authHeaders()
    });
  }

  magazines(category?: string): Observable<Magazine[]> {
    return this.http.get<Magazine[]>(`${this.base}/magazines`, {
      params: this.p({ category })
    });
  }

  latestMagazines(): Observable<Magazine[]> {
    return this.http.get<Magazine[]>(`${this.base}/magazines/latest`);
  }

  magazineCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/magazines/categories`);
  }

  managedMagazines(): Observable<Magazine[]> {
    return this.http.get<Magazine[]>(`${this.base}/magazines/manage`, {
      headers: this.authHeaders()
    });
  }

  createMagazine(req: {
    title: string;
    issueNo?: string;
    cover?: string;
    publishDate?: string;
    price?: number;
    category?: string;
    coverStory?: string;
    highlights?: string[];
    latest?: boolean;
  }): Observable<Magazine> {
    return this.http.post<Magazine>(`${this.base}/magazines/manage`, req, {
      headers: this.authHeaders()
    });
  }

  deleteMagazineItem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/magazines/manage/${id}`, {
      headers: this.authHeaders()
    });
  }

  products(category?: string): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.base}/products`, { params: this.p({ category }) });
  }

  productCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/products/categories`);
  }

  managedProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.base}/products/manage`, {
      headers: this.authHeaders()
    });
  }

  createProduct(req: {
    name: string;
    category?: string;
    price?: number;
    originalPrice?: number;
    image?: string;
    rating?: number;
    stock?: number;
    description?: string;
  }): Observable<Product> {
    return this.http.post<Product>(`${this.base}/products/manage`, req, {
      headers: this.authHeaders()
    });
  }

  deleteProductItem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/products/manage/${id}`, {
      headers: this.authHeaders()
    });
  }

  search(q: string): Observable<SearchResult> {
    return this.http.get<SearchResult>(`${this.base}/search`, { params: this.p({ q }) });
  }

  uploadVideo(file: File, meta: { title: string; category?: string; description?: string }) {
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
    return this.http.get<VideoUpload[]>(`${this.base}/videos/uploads/mine`, {
      headers: this.authHeaders()
    });
  }

  adminUploads(status?: string): Observable<VideoUpload[]> {
    return this.http.get<VideoUpload[]>(`${this.base}/videos/uploads`, {
      params: this.p({ status }),
      headers: this.authHeaders()
    });
  }

  approveUpload(id: number, note?: string): Observable<VideoUpload> {
    return this.http.post<VideoUpload>(
      `${this.base}/videos/uploads/${id}/approve`,
      { note },
      { headers: this.authHeaders() }
    );
  }

  rejectUpload(id: number, note?: string): Observable<VideoUpload> {
    return this.http.post<VideoUpload>(
      `${this.base}/videos/uploads/${id}/reject`,
      { note },
      { headers: this.authHeaders() }
    );
  }

  takedownUpload(id: number): Observable<VideoUpload> {
    return this.http.post<VideoUpload>(
      `${this.base}/videos/uploads/${id}/takedown`,
      {},
      { headers: this.authHeaders() }
    );
  }

  resubmitUpload(id: number): Observable<VideoUpload> {
    return this.http.post<VideoUpload>(
      `${this.base}/videos/uploads/${id}/resubmit`,
      {},
      { headers: this.authHeaders() }
    );
  }

  deleteUpload(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/videos/uploads/${id}`, {
      headers: this.authHeaders()
    });
  }

  members(): Observable<Member[]> {
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

  posts(type?: string, page = 0, size = 12): Observable<Page<Post>> {
    return this.http.get<Page<Post>>(`${this.base}/posts`, {
      params: this.p({ type, page, size })
    });
  }

  createPost(
    meta: { type: string; title: string; category?: string; body?: string },
    file?: File | null
  ) {
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
    return this.http.get<Page<Post>>(`${this.base}/posts/mine`, {
      headers: this.authHeaders(),
      params: this.p({ page, size })
    });
  }

  pendingPosts(page = 0, size = 20): Observable<Page<Post>> {
    return this.http.get<Page<Post>>(`${this.base}/posts/pending`, {
      headers: this.authHeaders(),
      params: this.p({ page, size })
    });
  }

  reviewPost(id: number, approve: boolean, note?: string): Observable<Post> {
    return this.http.post<Post>(
      `${this.base}/posts/${id}/${approve ? 'approve' : 'reject'}`,
      { note },
      { headers: this.authHeaders() }
    );
  }

  post(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.base}/posts/${id}`, {
      headers: this.authHeaders()
    });
  }

  postLikes(id: number): Observable<{ likes: number; liked: boolean }> {
    return this.http.get<{ likes: number; liked: boolean }>(
      `${this.base}/posts/${id}/likes`,
      { headers: this.authHeaders() }
    );
  }

  likePost(id: number): Observable<{ liked: boolean; likes: number }> {
    return this.http.post<{ liked: boolean; likes: number }>(
      `${this.base}/posts/${id}/like`,
      {},
      { headers: this.authHeaders() }
    );
  }

  postComments(id: number): Observable<PostComment[]> {
    return this.http.get<PostComment[]>(`${this.base}/posts/${id}/comments`, {
      headers: this.authHeaders()
    });
  }

  addPostComment(id: number, body: string): Observable<PostComment> {
    return this.http.post<PostComment>(
      `${this.base}/posts/${id}/comments`,
      { body },
      { headers: this.authHeaders() }
    );
  }

  deletePostComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/posts/comments/${commentId}`, {
      headers: this.authHeaders()
    });
  }

  managePosts(status?: string): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.base}/posts/manage`, {
      params: this.p({ status }),
      headers: this.authHeaders()
    });
  }

  takedownPost(id: number): Observable<Post> {
    return this.http.post<Post>(
      `${this.base}/posts/${id}/takedown`,
      {},
      { headers: this.authHeaders() }
    );
  }

  resubmitPost(id: number): Observable<Post> {
    return this.http.post<Post>(
      `${this.base}/posts/${id}/resubmit`,
      {},
      { headers: this.authHeaders() }
    );
  }

  deletePost(id: number): Observable<void> {
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
