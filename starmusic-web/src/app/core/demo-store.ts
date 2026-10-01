import { Banner, Post, Video, VideoUpload } from '../models';

// 靜態展示模式：上傳/投稿資料存在訪客瀏覽器 localStorage。
// 檔案本身不保存（靜態空間無處存檔），僅記錄中繼資料與檔名。

const UPLOADS_KEY = 'star-uploads';
const POSTS_KEY = 'star-posts';
const VIDEOS_KEY = 'star-videos';
const BANNERS_KEY = 'star-banners';

// ===== 輪播圖 Banner =====

export function demoBanners(): Banner[] {
  const stored = read<Banner>(BANNERS_KEY);
  if (stored.length > 0) {
    return stored;
  }
  // 默認測試數據
  const defaultBanners: Banner[] = [
    {
      id: 1,
      title: '告五人 Here @ World Tour 2026',
      imageUrl: 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=1200&h=400&fit=crop',
      linkUrl: '/videos',
      description: '台北小巨蛋 11/6-11/8 全場完售'
    },
    {
      id: 2,
      title: 'René 飛行日 巡迴演唱會',
      imageUrl: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=1200&h=400&fit=crop',
      linkUrl: '/videos',
      description: '12/5 台北小巨蛋 FINAL CALL'
    },
    {
      id: 3,
      title: '鼓鼓呂思緯 我現在又在想你了',
      imageUrl: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&h=400&fit=crop',
      linkUrl: '/videos',
      description: '12/26 台北流行音樂中心'
    }
  ];
  write(BANNERS_KEY, defaultBanners);
  return defaultBanners;
}

export function currentUsername(): string | null {
  try {
    return (JSON.parse(localStorage.getItem('star-member') ?? 'null') as { username?: string })
      ?.username ?? null;
  } catch {
    return null;
  }
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
  const list = read<Post>(POSTS_KEY);
  return status ? list.filter((x) => x.status === status) : list;
}

export function demoFindPost(id: number): Post | undefined {
  return read<Post>(POSTS_KEY).find((p) => p.id === id);
}

export function demoAddPost(
  meta: { type: Post['type']; title: string; category?: string; body?: string },
  file?: File | null
): Post {
  const list = read<Post>(POSTS_KEY);
  const item: Post = {
    id: nextId(list),
    type: meta.type,
    title: meta.title,
    category: meta.category ?? '',
    body: meta.body ?? '',
    mediaUrl: null,
    originalFilename: file?.name ?? '',
    author: currentUsername() ?? '',
    status: 'PENDING',
    reviewNote: '',
    likeCount: 0,
    createdAt: new Date().toISOString(),
    reviewedAt: null
  };
  list.unshift(item);
  write(POSTS_KEY, list);
  return item;
}

export function demoUpdatePost(id: number, patch: Partial<Post>): Post | undefined {
  const list = read<Post>(POSTS_KEY);
  const item = list.find((x) => x.id === id);
  if (item) {
    Object.assign(item, patch);
    write(POSTS_KEY, list);
  }
  return item;
}

export function demoDeletePost(id: number): void {
  write(
    POSTS_KEY,
    read<Post>(POSTS_KEY).filter((x) => x.id !== id)
  );
}
