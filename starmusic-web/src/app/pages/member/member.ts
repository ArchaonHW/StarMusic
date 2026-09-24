import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/api.service';
import { AccountTransaction, Member, Post, VideoUpload } from '../../models';

@Component({
  selector: 'app-member',
  imports: [FormsModule, DatePipe, DecimalPipe],
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
  protected readonly pendingUploads = signal<VideoUpload[]>([]);
  protected readonly pendingPosts = signal<Post[]>([]);
  protected readonly allMembers = signal<Member[]>([]);
  protected reviewNote: Record<number, string> = {};
  protected adjustAmount: Record<number, number> = {};
  protected adjustNote: Record<number, string> = {};

  constructor() {
    effect(() => {
      const m = this.auth.member();
      this.transactions.set([]);
      this.pendingUploads.set([]);
      this.allMembers.set([]);
      if (m) {
        this.loadTransactions(m.id);
        if (m.role === 'ADMIN') {
          this.loadAdmin();
        }
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

  review(id: number, approve: boolean): void {
    const note = this.reviewNote[id];
    const call = approve ? this.api.approveUpload(id, note) : this.api.rejectUpload(id, note);
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

  private loadTransactions(memberId: number): void {
    this.api.transactions(memberId).subscribe({
      next: (t) => this.transactions.set(t),
      error: () => this.transactions.set([])
    });
  }

  private loadAdmin(): void {
    this.api.adminUploads('PENDING').subscribe({
      next: (u) => this.pendingUploads.set(u),
      error: () => this.pendingUploads.set([])
    });
    this.api.pendingPosts().subscribe({
      next: (p) => this.pendingPosts.set(p),
      error: () => this.pendingPosts.set([])
    });
    this.api.members().subscribe({
      next: (m) => this.allMembers.set(m),
      error: () => this.allMembers.set([])
    });
  }
}
