import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-member',
  imports: [FormsModule],
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

  submit(): void {
    this.error.set('');
    this.message.set('');
    if (this.mode === 'login') {
      this.auth.login(this.username, this.password).subscribe({
        next: () => this.message.set('登入成功，歡迎回來！'),
        error: (e) => this.error.set(e.error?.message ?? e.error ?? '登入失敗')
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
          error: (e) => this.error.set(e.error?.message ?? e.error ?? '註冊失敗')
        });
    }
  }

  logout(): void {
    this.auth.logout();
    this.message.set('已登出');
  }
}
