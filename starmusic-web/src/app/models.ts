export interface Video {
  id: number;
  title: string;
  category: string;
  cover: string;
  description: string;
  duration: string;
  views: number;
  tags: string[];
  hot: boolean;
  videoUrl: string | null;
  vip: boolean;
  featured: boolean;
  updateNote: string | null;
}

export interface VideoSection {
  category: string;
  videos: Video[];
}

export interface VideoHome {
  featured: Video[];
  sections: VideoSection[];
  ranking: Video[];
}

export interface Channel {
  id: number;
  name: string;
  frequency: string;
  slogan: string;
  genre: string;
  streamUrl: string;
  live: boolean;
}

export interface Program {
  id: number;
  channelId: number;
  title: string;
  dj: string;
  timeSlot: string;
  category: string;
  description: string;
}

export interface Article {
  id: number;
  title: string;
  category: string;
  summary: string;
  content: string;
  source: string;
  author: string;
  imageUrl: string;
  imageUrls: string[];
  publishedAt: string;
  breaking: boolean;
}

export interface Magazine {
  id: number;
  title: string;
  issueNo: string;
  cover: string;
  publishDate: string;
  price: number;
  category: string;
  coverStory: string;
  highlights: string[];
  latest: boolean;
}

export interface Product {
  id: number;
  name: string;
  category: string;
  price: number;
  originalPrice: number;
  image: string;
  rating: number;
  stock: number;
  description: string;
}

export interface CartItem {
  productId: number;
  name: string;
  price: number;
  quantity: number;
}

export interface Cart {
  items: CartItem[];
  total: number;
}

export interface Order {
  id: number;
  items: CartItem[];
  total: number;
  createdAt: string;
}

export interface Member {
  id: number;
  username: string;
  nickname: string;
  email: string;
  role: 'MEMBER' | 'ADMIN';
  level: string;
  balance: number;
  createdAt: string;
}

export interface VideoUpload {
  id: number;
  title: string;
  category: string;
  description: string;
  originalFilename: string;
  uploader: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewNote: string;
  createdAt: string;
  reviewedAt: string | null;
}

export interface AccountTransaction {
  id: number;
  type: 'TOPUP' | 'CONSUME' | 'REFUND' | 'ADJUST';
  amount: number;
  balanceAfter: number;
  note: string;
  operator: string;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  member: Member;
}

export interface Post {
  id: number;
  type: 'VIDEO' | 'AUDIO' | 'IMAGE' | 'ARTICLE';
  title: string;
  category: string;
  body: string;
  mediaUrl: string | null;
  originalFilename: string;
  author: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewNote: string;
  createdAt: string;
  reviewedAt: string | null;
}

export interface SearchResult {
  keyword: string;
  total: number;
  videos: Video[];
  news: Article[];
  magazines: Magazine[];
  products: Product[];
  channels: Channel[];
  programs: Program[];
  posts: Post[];
}
