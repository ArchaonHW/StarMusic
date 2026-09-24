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
  Product,
  Program,
  SearchResult,
  Video,
  VideoUpload
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;

  videos(category?: string): Observable<Video[]> {
    return this.http.get<Video[]>(`${this.base}/videos`, { params: this.p({ category }) });
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

  products(category?: string): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.base}/products`, { params: this.p({ category }) });
  }

  productCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/products/categories`);
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
