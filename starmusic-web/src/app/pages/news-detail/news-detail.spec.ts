import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { NewsDetail } from './news-detail';
import { ApiService } from '../../core/api.service';
import { Article } from '../../models';

describe('NewsDetail loading', () => {
  function setup() {
    const requests: Subject<Article>[] = [];
    const api = { article: jasmine.createSpy('article').and.callFake(() => {
      const request = new Subject<Article>();
      requests.push(request);
      return request;
    }) };
    TestBed.configureTestingModule({
      imports: [NewsDetail],
      providers: [provideRouter([]), { provide: ApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(NewsDetail);
    return { fixture, requests, api };
  }

  const article = (id: number): Article => ({
    id, title: `新聞 ${id}`, category: '活動', summary: '', content: '',
    source: '', author: '', imageUrl: '', imageUrls: [],
    publishedAt: '2026-10-28T00:00:00', breaking: false
  });

  it('loads new route inputs and cancels previous requests', () => {
    const { fixture, requests, api } = setup();
    fixture.componentRef.setInput('id', '13');
    fixture.detectChanges();
    requests[0].next(article(13));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('新聞 13');
    fixture.componentRef.setInput('id', '14');
    fixture.detectChanges();
    expect(requests[0].observed).toBeFalse();
    expect(fixture.nativeElement.querySelector('h1')).toBeNull();
    requests[0].next(article(13));
    requests[1].next(article(14));
    fixture.detectChanges();
    expect(api.article).toHaveBeenCalledWith(14);
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('新聞 14');
    fixture.destroy();
    expect(requests[1].observed).toBeFalse();
  });

  it('shows a return link after a failed request and recovers on navigation', () => {
    const { fixture, requests } = setup();
    fixture.componentRef.setInput('id', '13');
    fixture.detectChanges();
    requests[0].error(new Error('not found'));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('無法載入這則新聞');
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/news');
    fixture.componentRef.setInput('id', '14');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('載入中');
    requests[1].next(article(14));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('新聞 14');
  });

  it('rejects invalid IDs without making a request', () => {
    const { fixture, api } = setup();
    fixture.componentRef.setInput('id', 'invalid');
    fixture.detectChanges();
    expect(api.article).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('無法載入這則新聞');
  });
});
