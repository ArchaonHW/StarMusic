import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Videos } from './pages/videos/videos';
import { Watch } from './pages/watch/watch';
import { Radio } from './pages/radio/radio';
import { News } from './pages/news/news';
import { NewsDetail } from './pages/news-detail/news-detail';
import { Magazines } from './pages/magazines/magazines';
import { Shop } from './pages/shop/shop';
import { CartPage } from './pages/cart/cart';
import { MemberPage } from './pages/member/member';
import { SearchPage } from './pages/search/search';

export const routes: Routes = [
  { path: '', component: Home, title: '星光全球娛樂台' },
  { path: 'videos', component: Videos, title: '影視 - 星光全球娛樂台' },
  { path: 'videos/:id', component: Watch, title: '觀看 - 星光全球娛樂台' },
  { path: 'radio', component: Radio, title: '星光電台 - 星光全球娛樂台' },
  { path: 'news', component: News, title: '新聞 - 星光全球娛樂台' },
  { path: 'news/:id', component: NewsDetail, title: '新聞 - 星光全球娛樂台' },
  { path: 'magazines', component: Magazines, title: '電子雜誌 - 星光全球娛樂台' },
  { path: 'shop', component: Shop, title: '星光購物 - 星光全球娛樂台' },
  { path: 'cart', component: CartPage, title: '購物車 - 星光全球娛樂台' },
  { path: 'member', component: MemberPage, title: '會員中心 - 星光全球娛樂台' },
  { path: 'search', component: SearchPage, title: '搜尋 - 星光全球娛樂台' },
  { path: '**', redirectTo: '' }
];
