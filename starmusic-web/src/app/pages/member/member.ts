import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/api.service';
import { AccountTransaction, Video, WatchHistoryItem } from '../../models';

@Component({
  selector: 'app-member',
  imports: [FormsModule, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './member.html',
  styleUrl: './member.scss'
})
export class MemberPage {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  protected mode: 'login' | 'register' = 'login';
  protected username = '';
  protected password = '';
  protected nickname = '';
  protected email = '';
  protected readonly error = signal('');
  protected readonly message = signal('');

  protected readonly transactions = signal<AccountTransaction[]>([]);
  protected readonly myFavorites = signal<Video[]>([]);
  protected readonly myHistory = signal<WatchHistoryItem[]>([]);

  constructor() {
    effect(() => {
      const m = this.auth.member();
      this.transactions.set([]);
      this.myFavorites.set([]);
      this.myHistory.set([]);
      if (m) {
        this.loadTransactions(m.id);
        this.loadLibrary();
      }
    });
  }

  private errMsg(e: { status?: number }, fallback: string): string {
    if (e?.status === 429) return '嘗試次數過多，請稍後再試';
    if (e?.status === 401) return '帳號或密碼錯誤';
    if (e?.status === 403) return '沒有權限';
    if (e?.status === 409) return '帳號已被使用';
    return fallback;
  }

  submit(): void {
    this.error.set('');
    this.message.set('');
    if (this.mode === 'login') {
      this.auth.login(this.username, this.password).subscribe({
        next: () => this.message.set('登入成功，歡迎回來！'),
        error: (e) => this.error.set(this.errMsg(e, '登入失敗'))
      });
    } else {
      this.api
        .register({
          username: this.username,
          password: this.password,
          nickname: this.nickname,
          email: this.email
        })
        .subscribe({
          next: () => {
            this.message.set('註冊成功，請登入');
            this.mode = 'login';
          },
          error: (e) => this.error.set(this.errMsg(e, '註冊失敗'))
        });
    }
  }

  logout(): void {
    this.auth.logout();
    this.message.set('已登出');
  }

  typeLabel(type: string): string {
    return { TOPUP: '儲值', CONSUME: '消費', REFUND: '退款', ADJUST: '調整' }[type] ?? type;
  }

  private loadTransactions(memberId: number): void {
    this.api.transactions(memberId).subscribe({
      next: (t) => this.transactions.set(t),
      error: () => this.transactions.set([])
    });
  }

  private loadLibrary(): void {
    this.api.myFavorites().subscribe({
      next: (v) => this.myFavorites.set(v),
      error: () => this.myFavorites.set([])
    });
    this.api.myHistory().subscribe({
      next: (h) => this.myHistory.set(h),
      error: () => this.myHistory.set([])
    });
  }
}
