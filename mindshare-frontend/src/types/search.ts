export interface SearchResultItem {
  id: number;
  title: string;
  description: string;
  tags: string[];
  imgUrls: string[];
  authorAvatar: string;
  authorNickname: string;
  authorTagJson: string;
  publishTime: string;
  isTop: boolean;
}

export interface SearchResponse {
  items: SearchResultItem[];
  nextAfter: string | null;
  hasMore: boolean;
}

export interface SuggestResponse {
  items: string[];
}
