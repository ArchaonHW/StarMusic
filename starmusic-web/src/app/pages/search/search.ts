import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { SearchResult } from '../../models';

@Component({
  selector: 'app-search',
  imports: [RouterLink, DatePipe, DecimalPipe],
  templateUrl: './search.html',
  styleUrl: './search.scss'
})
export class SearchPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly keyword = signal('');
  protected readonly result = signal<SearchResult | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const q = params.get('q')?.trim() ?? '';
      this.keyword.set(q);
      this.result.set(null);
      if (!q) {
        return;
      }
      this.loading.set(true);
      this.api.search(q).subscribe({
        next: (r) => {
          this.result.set(r);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    });
  }
}
