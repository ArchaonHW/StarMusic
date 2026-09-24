import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Article,
  Channel,
  Magazine,
  Member,
  Product,
  Program,
  SearchResult,
  Video
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;

  videos(category?: string): Observable<Video[]> {
    return this.http.get<Video[]>(`${this.base}/videos`, { params: this.p({ category }) });
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
