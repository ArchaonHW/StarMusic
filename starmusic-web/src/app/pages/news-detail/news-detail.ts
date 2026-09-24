import { Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { Article } from '../../models';

@Component({
  selector: 'app-news-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './news-detail.html',
  styleUrl: './news-detail.scss'
})
export class NewsDetail implements OnInit {
  private readonly api = inject(ApiService);

  readonly id = input.required<string>();

  protected readonly article = signal<Article | null>(null);

  ngOnInit(): void {
    this.api.article(Number(this.id())).subscribe((a) => this.article.set(a));
  }
}
