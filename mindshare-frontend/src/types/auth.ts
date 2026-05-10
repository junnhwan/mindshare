export interface AuthUser {
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
  gender: "MALE" | "FEMALE" | "OTHER" | "UNKNOWN";
  createTime: string;
  updateTime: string;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface LoginResponse {
  user: AuthUser;
  token: TokenPair;
}

export interface LoginRequest {
  identifier: string;
  password?: string;
  code?: string;
  channel: "PASSWORD" | "CODE";
}

export interface RegisterRequest {
  identifier: string;
  code: string;
  password: string;
  nickname?: string;
}

export interface SendCodeRequest {
  identifier: string;
}

export interface ResetPasswordRequest {
  identifier: string;
  code: string;
  newPassword: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}
