import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Video } from '../../models';

@Component({
  selector: 'app-watch',
  imports: [RouterLink],
  templateUrl: './watch.html',
  styleUrl: './watch.scss'
})
export class Watch {
  private readonly api = inject(ApiService);

  readonly id = input.required<string>();

  protected readonly video = signal<Video | null>(null);
  protected readonly related = signal<Video[]>([]);
  protected readonly notFound = signal(false);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      this.video.set(null);
      this.related.set([]);
      this.notFound.set(false);
      this.api.video(id).subscribe({
        next: (v) => {
          this.video.set(v);
          this.loadRelated(v);
        },
        error: () => this.notFound.set(true)
      });
    });
  }

  private loadRelated(v: Video): void {
    this.api.videos(v.category).subscribe((list) =>
      this.related.set(list.filter((x) => x.id !== v.id).slice(0, 6))
    );
  }

  formatViews(views: number): string {
    return views >= 10000 ? (views / 10000).toFixed(1) + ' 萬' : String(views);
  }
}
