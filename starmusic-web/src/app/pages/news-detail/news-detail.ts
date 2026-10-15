import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
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
export class NewsDetail implements OnInit {
  private readonly api = inject(ApiService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly id = input.required<string>();

  protected readonly article = signal<Article | null>(null);

  protected readonly embedUrl = computed<SafeResourceUrl | null>(() => {
    const m = this.article()?.content.match(YT_PATTERN);
    return m
      ? this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${m[1]}`)
      : null;
  });

  protected readonly displayContent = computed(() =>
    this.article()?.content.replace(YT_PATTERN, '').trim() ?? ''
  );

  ngOnInit(): void {
    this.api.article(Number(this.id())).subscribe((a) => this.article.set(a));
  }
}
