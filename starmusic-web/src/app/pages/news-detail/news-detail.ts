import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ApiService } from '../../core/api.service';
import { Article } from '../../models';

const YT_PATTERN = /https?:\/\/(?:www\.)?(?:youtube\.com\/watch\?[^\s]*v=|youtu\.be\/)([\w-]{11})[^\s]*/;

@Component({
  selector: 'app-news-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './news-detail.html',
  styleUrl: './news-detail.scss'
})
export class NewsDetail {
  private readonly api = inject(ApiService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly id = input.required<string>();

  protected readonly article = signal<Article | null>(null);
  protected readonly loadError = signal(false);

  protected readonly embedUrl = computed<SafeResourceUrl | null>(() => {
    const m = this.article()?.content.match(YT_PATTERN);
    return m
      ? this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${m[1]}`)
      : null;
  });

  protected readonly displayContent = computed(() =>
    this.article()?.content.replace(YT_PATTERN, '').trim() ?? ''
  );

  constructor() {
    effect((onCleanup) => {
      const id = Number(this.id());
      this.article.set(null);
      this.loadError.set(false);
      if (!Number.isSafeInteger(id) || id <= 0) {
        this.loadError.set(true);
        return;
      }
      const subscription = this.api.article(id).subscribe({
        next: (article) => this.article.set(article),
        error: () => this.loadError.set(true)
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }
}
