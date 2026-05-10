export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  hasMore: boolean;
}

export interface CursorPageResponse<T> {
  items: T[];
  nextAfter: string | null;
  hasMore: boolean;
}
