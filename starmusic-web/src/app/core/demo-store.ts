import {
  Article,
  Banner,
  BannerRequest,
  Magazine,
  Member,
  Post,
  PostComment,
  Product,
  Video,
  VideoUpload
} from '../models';

// 靜態展示模式：上傳/投稿資料存在訪客瀏覽器 localStorage。
// 檔案本身不保存（靜態空間無處存檔），僅記錄中繼資料與檔名。

const UPLOADS_KEY = 'star-uploads';
const POSTS_KEY = 'star-posts';
const VIDEOS_KEY = 'star-videos';
const BANNERS_KEY = 'star-banners';
const LIKES_KEY = 'star-post-likes';
const COMMENTS_KEY = 'star-post-comments';
const NEWS_KEY = 'star-news';
const MAGAZINES_KEY = 'star-magazines';
const PRODUCTS_KEY = 'star-products';
const USERS_KEY = 'star-users';

export function currentMember(): Member | null {
  try {
    return JSON.parse(localStorage.getItem('star-member') ?? 'null') as Member | null;
  } catch {
    return null;
  }
}

export function currentUsername(): string | null {
  return currentMember()?.username ?? null;
}

function read<T>(key: string): T[] {
  try {
    return JSON.parse(localStorage.getItem(key) ?? '[]');
  } catch {
    return [];
  }
}

function write<T>(key: string, list: T[]): void {
  localStorage.setItem(key, JSON.stringify(list));
}

function nextId(items: { id: number }[]): number {
  return Math.max(100000, ...items.map((i) => i.id)) + 1;
}

function addItem<T extends { id: number }>(key: string, data: Omit<T, 'id'>): T {
  const list = read<T>(key);
  const item = { ...data, id: nextId(list) } as T;
  list.unshift(item);
  write(key, list);
  return item;
}

function updateItem<T extends { id: number }>(key: string, id: number, patch: Partial<T>): T | undefined {
  const list = read<T>(key);
  const item = list.find((x) => x.id === id);
  if (item) {
    Object.assign(item, patch);
    write(key, list);
  }
  return item;
}

function removeItem<T extends { id: number }>(key: string, id: number): void {
  write(key, read<T>(key).filter((x) => x.id !== id));
}

// ===== 輪播圖 Banner =====

const DEFAULT_BANNERS: Banner[] = [
  {
    id: 1,
    title: '告五人 Here @ World Tour 2026',
    imageUrl: 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=1200&h=400&fit=crop',
    linkUrl: '/videos',
    description: '台北小巨蛋 11/6-11/8 全場完售',
    sortOrder: 1,
    active: true
  },
  {
    id: 2,
    title: 'René 飛行日 巡迴演唱會',
    imageUrl: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&h=400&fit=crop',
    linkUrl: '/videos',
    description: '12/5 台北小巨蛋 FINAL CALL',
    sortOrder: 2,
    active: true
  },
  {
    id: 3,
    title: '鼓鼓呂思緯 我現在又在想你了',
    imageUrl: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&h=400&fit=crop',
    linkUrl: '/videos',
    description: '12/26 台北流行音樂中心',
    sortOrder: 3,
    active: true
  }
];

// 第一次使用時寫入預設輪播圖；之後完全由後台維護（全部刪除也不會自動補回）
export function demoAllBanners(): Banner[] {
  if (localStorage.getItem(BANNERS_KEY) === null) {
    write(BANNERS_KEY, DEFAULT_BANNERS);
  }
  return read<Banner>(BANNERS_KEY).sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
}

export function demoBanners(): Banner[] {
  return demoAllBanners().filter((b) => b.active !== false);
}

export function demoAddBanner(req: BannerRequest): Banner {
  demoAllBanners();
  return addItem<Banner>(BANNERS_KEY, req);
}

export function demoUpdateBanner(id: number, req: BannerRequest): Banner | undefined {
  demoAllBanners();
  return updateItem<Banner>(BANNERS_KEY, id, req);
}

export function demoDeleteBanner(id: number): void {
  demoAllBanners();
  removeItem<Banner>(BANNERS_KEY, id);
}

// ===== 新聞 / 雜誌 / 商品（後台新增的內容）=====

export const demoNews = () => read<Article>(NEWS_KEY);
export const demoAddNews = (a: Omit<Article, 'id'>) => addItem<Article>(NEWS_KEY, a);
export const demoUpdateNews = (id: number, a: Partial<Article>) =>
  updateItem<Article>(NEWS_KEY, id, a);
export const demoDeleteNews = (id: number) => removeItem<Article>(NEWS_KEY, id);

export const demoMagazines = () => read<Magazine>(MAGAZINES_KEY);
export const demoAddMagazine = (m: Omit<Magazine, 'id'>) => addItem<Magazine>(MAGAZINES_KEY, m);
export const demoUpdateMagazine = (id: number, m: Partial<Magazine>) =>
  updateItem<Magazine>(MAGAZINES_KEY, id, m);
export const demoDeleteMagazine = (id: number) => removeItem<Magazine>(MAGAZINES_KEY, id);

export const demoProducts = () => read<Product>(PRODUCTS_KEY);
export const demoAddProduct = (p: Omit<Product, 'id'>) => addItem<Product>(PRODUCTS_KEY, p);
export const demoUpdateProduct = (id: number, p: Partial<Product>) =>
  updateItem<Product>(PRODUCTS_KEY, id, p);
export const demoDeleteProduct = (id: number) => removeItem<Product>(PRODUCTS_KEY, id);

// ===== 會員（AuthService 存在 localStorage 的帳號）=====

export function demoMembers(): Member[] {
  return read<{ member: Member }>(USERS_KEY).map((u) => u.member);
}

// ===== 影片上傳 =====

export function demoMyUploads(): VideoUpload[] {
  const u = currentUsername();
  return u
    ? read<VideoUpload>(UPLOADS_KEY).filter((x) => x.uploader === u)
    : [];
}

export function demoAllUploads(status?: string): VideoUpload[] {
  const list = read<VideoUpload>(UPLOADS_KEY);
  return status ? list.filter((x) => x.status === status) : list;
}

// ===== 本機影片庫：上傳核准的影片會出現在首頁/列表 =====

export function demoLocalVideos(): Video[] {
  return read<Video>(VIDEOS_KEY);
}

export function demoFindVideo(id: number): Video | undefined {
  return read<Video>(VIDEOS_KEY).find((v) => v.id === id);
}

function toVideo(u: VideoUpload): Video {
  return {
    id: u.id,
    title: u.title,
    category: u.category || '會員上傳',
    cover: '',
    description: u.description,
    duration: '',
    views: 0,
    tags: ['會員上傳'],
    hot: true,
    videoUrl: null,
    vip: false,
    featured: true,
    updateNote: `由 ${u.uploader} 上傳`
  };
}

function syncVideo(u: VideoUpload): void {
  const list = read<Video>(VIDEOS_KEY).filter((v) => v.id !== u.id);
  if (u.status === 'APPROVED') {
    list.unshift(toVideo(u));
  }
  write(VIDEOS_KEY, list);
}

export function demoAddUpload(
  file: File,
  meta: { title: string; category?: string; description?: string }
): VideoUpload {
  const list = read<VideoUpload>(UPLOADS_KEY);
  const item: VideoUpload = {
    id: nextId(list),
    title: meta.title,
    category: meta.category ?? '',
    description: meta.description ?? '',
    originalFilename: file.name,
    uploader: currentUsername() ?? '',
    status: 'APPROVED',
    reviewNote: '',
    createdAt: new Date().toISOString(),
    reviewedAt: new Date().toISOString()
  };
  list.unshift(item);
  write(UPLOADS_KEY, list);
  syncVideo(item);
  return item;
}

export function demoUpdateUpload(
  id: number,
  patch: Partial<VideoUpload>
): VideoUpload | undefined {
  const list = read<VideoUpload>(UPLOADS_KEY);
  const item = list.find((x) => x.id === id);
  if (item) {
    Object.assign(item, patch);
    write(UPLOADS_KEY, list);
    syncVideo(item);
  }
  return item;
}

export function demoDeleteUpload(id: number): void {
  write(
    UPLOADS_KEY,
    read<VideoUpload>(UPLOADS_KEY).filter((x) => x.id !== id)
  );
  write(
    VIDEOS_KEY,
    read<Video>(VIDEOS_KEY).filter((v) => v.id !== id)
  );
}

// ===== 投稿 =====

export function demoMyPosts(): Post[] {
  const u = currentUsername();
  return u ? read<Post>(POSTS_KEY).filter((x) => x.author === u) : [];
}

export function demoAllPosts(status?: string): Post[] {
  const list = read<Post>(POSTS_KEY).map((p) => ({ ...p, likeCount: demoLikeCount(p.id) }));
  return status ? list.filter((x) => x.status === status) : list;
}

export function demoFindPost(id: number): Post | undefined {
  const p = read<Post>(POSTS_KEY).find((x) => x.id === id);
  return p && { ...p, likeCount: demoLikeCount(p.id) };
}

// 靜態模式沒有審核者，投稿直接上架；mediaUrl 為壓縮後的圖片 data URL（影音檔不保存）
export function demoAddPost(
  meta: { type: Post['type']; title: string; category?: string; body?: string },
  file?: File | null,
  mediaUrl: string | null = null
): Post {
  const list = read<Post>(POSTS_KEY);
  const item: Post = {
    id: nextId(list),
    type: meta.type,
    title: meta.title,
    category: meta.category ?? '',
    body: meta.body ?? '',
    mediaUrl,
    originalFilename: file?.name ?? '',
    author: currentUsername() ?? '',
    status: 'APPROVED',
    reviewNote: '',
    likeCount: 0,
    createdAt: new Date().toISOString(),
    reviewedAt: new Date().toISOString()
  };
  list.unshift(item);
  write(POSTS_KEY, list);
  return item;
}

export function demoUpdatePost(id: number, patch: Partial<Post>): Post | undefined {
  updateItem<Post>(POSTS_KEY, id, patch);
  return demoFindPost(id);
}

export function demoDeletePost(id: number): void {
  removeItem<Post>(POSTS_KEY, id);
  const likes = readLikes();
  delete likes[id];
  localStorage.setItem(LIKES_KEY, JSON.stringify(likes));
  write(COMMENTS_KEY, read<PostComment>(COMMENTS_KEY).filter((c) => c.postId !== id));
}

// ===== 投稿按讚 / 留言 =====

function readLikes(): Record<number, string[]> {
  try {
    return JSON.parse(localStorage.getItem(LIKES_KEY) ?? '{}');
  } catch {
    return {};
  }
}

export function demoLikeCount(postId: number): number {
  return readLikes()[postId]?.length ?? 0;
}

export function demoPostLikes(postId: number): { likes: number; liked: boolean } {
  const users = readLikes()[postId] ?? [];
  const u = currentUsername();
  return { likes: users.length, liked: !!u && users.includes(u) };
}

export function demoToggleLike(postId: number): { likes: number; liked: boolean } {
  const u = currentUsername();
  const likes = readLikes();
  const users = likes[postId] ?? [];
  likes[postId] = u && users.includes(u) ? users.filter((x) => x !== u) : [...users, u ?? ''];
  localStorage.setItem(LIKES_KEY, JSON.stringify(likes));
  return demoPostLikes(postId);
}

export function demoPostComments(postId: number): PostComment[] {
  return read<PostComment>(COMMENTS_KEY).filter((c) => c.postId === postId);
}

export function demoAddPostComment(postId: number, body: string): PostComment {
  const m = currentMember();
  return addItem<PostComment>(COMMENTS_KEY, {
    postId,
    body,
    author: m?.username ?? '',
    memberId: m?.id ?? 0,
    createdAt: new Date().toISOString()
  });
}

export function demoDeletePostComment(commentId: number): void {
  removeItem<PostComment>(COMMENTS_KEY, commentId);
}
