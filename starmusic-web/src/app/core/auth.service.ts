import { Injectable, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from './api.service';
import { AccountTransaction, Member } from '../models';
import { Observable, of, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

interface StoredUser {
  member: Member;
  pw: string;
}

const USERS_KEY = 'star-users';
const TX_KEY = 'star-tx';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);

  readonly member = signal<Member | null>(this.restore());

  login(username: string, password: string): Observable<{ token: string; member: Member }> {
    if (environment.staticData) {
      return this.localLogin(username, password);
    }
    return this.api.login(username, password).pipe(
      tap((res) => this.setSession(res.token, res.member))
    );
  }

  register(req: { username: string; password: string; nickname?: string; email?: string }) {
    if (environment.staticData) {
      return this.localRegister(req);
    }
    return this.api.register(req);
  }

  refresh(): void {
    if (environment.staticData || !localStorage.getItem('star-token')) {
      return;
    }
    this.api.me().subscribe({
      next: (m) => {
        localStorage.setItem('star-member', JSON.stringify(m));
        this.member.set(m);
      },
      error: () => this.logout()
    });
  }

  logout(): void {
    if (!environment.staticData && localStorage.getItem('star-token')) {
      this.api.logout().subscribe({ error: () => {} });
    }
    localStorage.removeItem('star-token');
    localStorage.removeItem('star-member');
    this.member.set(null);
  }

  // ===== 靜態展示模式：用 localStorage 模擬會員系統 =====
  // 僅供無後端的靜態部署展示用，帳號資料存在訪客瀏覽器，無安全性，
  // 也不跨裝置。預設展示帳號 starfan / starfan。

  demoTransactions(memberId: number): AccountTransaction[] {
    const all = this.readTx();
    return all[memberId] ?? [];
  }

  private localLogin(username: string, password: string) {
    const u = this.users().find((x) => x.member.username === username.trim());
    if (!u || u.pw !== this.hash(password)) {
      return throwError(() => new HttpErrorResponse({ status: 401 }));
    }
    const res = { token: `demo-${u.member.id}-${Date.now()}`, member: u.member };
    this.setSession(res.token, res.member);
    return of(res);
  }

  private localRegister(req: {
    username: string;
    password: string;
    nickname?: string;
    email?: string;
  }): Observable<Member> {
    const username = req.username.trim();
    const bad =
      username.length < 3 ||
      username.length > 32 ||
      !req.password ||
      req.password.length < 6 ||
      req.password.length > 72 ||
      (!!req.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(req.email));
    if (bad) {
      return throwError(() => new HttpErrorResponse({ status: 400 }));
    }
    const users = this.users();
    if (users.some((u) => u.member.username === username)) {
      return throwError(() => new HttpErrorResponse({ status: 409 }));
    }
    const member: Member = {
      id: Math.max(0, ...users.map((u) => u.member.id)) + 1,
      username,
      nickname: req.nickname?.trim() || username,
      email: req.email ?? '',
      role: 'MEMBER',
      level: '一般會員',
      balance: 0,
      createdAt: new Date().toISOString()
    };
    users.push({ member, pw: this.hash(req.password) });
    localStorage.setItem(USERS_KEY, JSON.stringify(users));
    return of(member);
  }

  private users(): StoredUser[] {
    let users: StoredUser[] = [];
    try {
      users = JSON.parse(localStorage.getItem(USERS_KEY) ?? '[]');
    } catch {
      users = [];
    }
    let changed = false;
    if (!users.some((u) => u.member.username === 'starfan')) {
      users.push({
        member: {
          id: 1,
          username: 'starfan',
          nickname: '星光粉絲',
          email: 'fan@starmusic.tv',
          role: 'MEMBER',
          level: 'VIP',
          balance: 3680,
          createdAt: '2026-09-01T00:00:00Z'
        },
        pw: this.hash('starfan')
      });
      changed = true;
    }
    if (!users.some((u) => u.member.username === 'admin')) {
      users.push({
        member: {
          id: 2,
          username: 'admin',
          nickname: '系統管理員',
          email: 'twstarmusic@gmail.com',
          role: 'ADMIN',
          level: '管理員',
          balance: 0,
          createdAt: '2026-09-01T00:00:00Z'
        },
        pw: this.hash('admin')
      });
      changed = true;
    }
    if (changed) {
      localStorage.setItem(USERS_KEY, JSON.stringify(users));
    }
    if (users.some((u) => u.member.username === 'starfan')) {
      const tx = this.readTx();
      if (!tx[1]) {
        tx[1] = [
          { id: 3, type: 'CONSUME', amount: -521, balanceAfter: 3680, note: '購買：星光娛樂週刊 NO.521', operator: 'SYSTEM', createdAt: '2026-09-20T08:00:00Z' },
          { id: 2, type: 'CONSUME', amount: -799, balanceAfter: 4201, note: '購買：乘風2026 成團紀念專輯', operator: 'SYSTEM', createdAt: '2026-09-15T08:00:00Z' },
          { id: 1, type: 'TOPUP', amount: 5000, balanceAfter: 5000, note: '開通會員儲值', operator: 'SYSTEM', createdAt: '2026-09-01T00:00:00Z' }
        ];
        localStorage.setItem(TX_KEY, JSON.stringify(tx));
      }
    }
    return users;
  }

  private readTx(): Record<number, AccountTransaction[]> {
    try {
      return JSON.parse(localStorage.getItem(TX_KEY) ?? '{}');
    } catch {
      return {};
    }
  }

  private setSession(token: string, member: Member): void {
    localStorage.setItem('star-token', token);
    localStorage.setItem('star-member', JSON.stringify(member));
    this.member.set(member);
  }

  private hash(s: string): string {
    let h = 5381;
    for (const c of s) {
      h = ((h << 5) + h + c.charCodeAt(0)) >>> 0;
    }
    return 'h' + h.toString(36);
  }

  private restore(): Member | null {
    try {
      const raw = localStorage.getItem('star-member');
      return raw ? (JSON.parse(raw) as Member) : null;
    } catch {
      return null;
    }
  }
}
