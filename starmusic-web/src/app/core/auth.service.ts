import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Member } from '../models';
import { tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);

  readonly member = signal<Member | null>(this.restore());

  login(username: string, password: string) {
    return this.api.login(username, password).pipe(
      tap((res) => {
        localStorage.setItem('star-token', res.token);
        localStorage.setItem('star-member', JSON.stringify(res.member));
        this.member.set(res.member);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('star-token');
    localStorage.removeItem('star-member');
    this.member.set(null);
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
