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

export type IdentifierType = "PHONE" | "EMAIL";

export interface LoginRequest {
  identifierType: IdentifierType;
  identifier: string;
  password?: string;
  code?: string;
}

export interface RegisterRequest {
  identifierType: IdentifierType;
  identifier: string;
  code: string;
  password: string;
  nickname?: string;
}

export interface SendCodeRequest {
  scene: "REGISTER" | "LOGIN" | "RESET_PASSWORD";
  identifierType: IdentifierType;
  identifier: string;
}

export interface ResetPasswordRequest {
  identifierType: IdentifierType;
  identifier: string;
  code: string;
  newPassword: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}
