export interface ProfileData {
  id: number;
  phone: string;
  email: string;
  nickname: string;
  avatar: string;
  bio: string;
  zgId: string;
  tagsJson: string;
  birthday: string;
  school: string;
  gender: string;
}

export interface UpdateProfileRequest {
  nickname?: string;
  bio?: string;
  gender?: string;
  birthday?: string;
  school?: string;
  tagsJson?: string;
}

export interface CountsData {
  following: number;
  followers: number;
}
