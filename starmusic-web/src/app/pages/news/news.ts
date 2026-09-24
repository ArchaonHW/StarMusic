import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { Article } from '../../models';

@Component({
  selector: 'app-news',
  imports: [RouterLink, DatePipe],
  templateUrl: './news.html',
  styleUrl: './news.scss'
})
export class News implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly articles = signal<Article[]>([]);
  protected readonly categories = signal<string[]>([]);
  protected readonly activeCategory = signal('');

  ngOnInit(): void {
    this.api.newsCategories().subscribe((c) => this.categories.set(c));
    this.load();
  }

  select(category: string): void {
    this.activeCategory.set(category);
    this.load();
  }

  private load(): void {
    this.api.news(this.activeCategory() || undefined).subscribe((a) => this.articles.set(a));
  }
}
