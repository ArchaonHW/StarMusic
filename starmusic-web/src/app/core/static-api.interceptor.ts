import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment';

const STATIC_ROOT = '/assets/api';

// 靜態部署模式：把 /api 的 GET 請求改寫成對應的靜態 JSON 檔，
// 例如 /api/news?category=活動 → /assets/api/news/category=活動.json。
// 非 GET（登入、投稿、按讚、留言等）直接回錯，不再打到不存在的後端。
export const staticApiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!environment.staticData || !req.url.startsWith(environment.apiBase)) {
    return next(req);
  }
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    return throwError(
      () =>
        new HttpErrorResponse({
          status: 501,
          statusText: 'Static hosting',
          error: { message: '靜態展示版不提供此功能（需連接後端主機）' }
        })
    );
  }
  const path = req.url.slice(environment.apiBase.length);
  const query = req.params.toString();
  const file = query ? `${path}/${query}.json` : `${path}.json`;
  return next(req.clone({ url: `${STATIC_ROOT}${file}` }));
};
