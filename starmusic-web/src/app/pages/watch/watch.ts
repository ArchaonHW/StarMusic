import { Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Video } from '../../models';

@Component({
  selector: 'app-watch',
  imports: [RouterLink],
  templateUrl: './watch.html',
  styleUrl: './watch.scss'
})
export class Watch implements OnInit {
  private readonly api = inject(ApiService);

  readonly id = input.required<string>();

  protected readonly video = signal<Video | null>(null);
  protected readonly notFound = signal(false);

  ngOnInit(): void {
    this.api.video(Number(this.id())).subscribe({
      next: (v) => this.video.set(v),
      error: () => this.notFound.set(true)
    });
  }

  formatViews(views: number): string {
    return views >= 10000 ? (views / 10000).toFixed(1) + ' 萬' : String(views);
  }
}
