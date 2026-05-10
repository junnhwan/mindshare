export type PostVisibility = "public" | "followers" | "school" | "private" | "unlisted";
export type PostStatus = "draft" | "published" | "deleted";

export interface FeedItem {
  id: number;
  title: string;
  description?: string;
  tags?: string[];
  imgUrls?: string[];
  authorAvatar?: string;
  authorNickname?: string;
  authorTagJson?: string;
  publishTime?: string;
  isTop?: boolean;
}

export interface PostDetail {
  id: number;
  creatorId: number;
  status: PostStatus;
  type: string;
  visible: PostVisibility;
  isTop: boolean;
  tagId: number;
  title: string;
  description: string;
  tags: string[];
  imgUrls: string[];
  videoUrl: string;
  contentObjectKey: string;
  contentEtag: string;
  contentSize: number;
  contentSha256: string;
  contentUrl: string;
  publishTime: string;
  createTime: string;
  updateTime: string;
  authorAvatar: string;
  authorNickname: string;
  authorTagJson: string;
}

export interface DraftCreateResponse {
  id: string;
}

export interface ContentConfirmRequest {
  objectKey: string;
  etag: string;
  size: number;
  sha256: string;
}

export interface PatchMetadataRequest {
  title?: string;
  tags?: string[];
  imgUrls?: string[];
  description?: string;
  visible?: PostVisibility;
  isTop?: boolean;
}
