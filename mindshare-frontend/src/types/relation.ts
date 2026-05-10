export type RelationStatus = "self" | "following" | "followedBy" | "mutual" | "none";

export interface RelationStatusResponse {
  status: RelationStatus;
}

export interface RelationCountsResponse {
  following: number;
  followers: number;
}

export interface RelationUser {
  id: number;
  nickname: string;
  avatar: string;
  bio: string;
  zgId: string;
  tagsJson: string;
}
