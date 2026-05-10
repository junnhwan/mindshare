export const APP_NAME = "MindShare";

export const VISIBILITY_OPTIONS = [
  { value: "public", label: "公开" },
  { value: "followers", label: "粉丝可见" },
  { value: "school", label: "校友可见" },
  { value: "private", label: "仅自己" },
  { value: "unlisted", label: "不公开到列表" },
] as const;

export const GENDER_OPTIONS = [
  { value: "MALE", label: "男" },
  { value: "FEMALE", label: "女" },
  { value: "OTHER", label: "其他" },
  { value: "UNKNOWN", label: "保密" },
] as const;

export const ERROR_MESSAGES: Record<string, string> = {
  IDENTIFIER_EXISTS: "账号已存在",
  IDENTIFIER_NOT_FOUND: "账号不存在",
  ZGID_EXISTS: "知光号已被占用",
  VERIFICATION_RATE_LIMIT: "验证码发送过于频繁，请稍后再试",
  VERIFICATION_DAILY_LIMIT: "今日验证码发送次数已达上限",
  VERIFICATION_NOT_FOUND: "请先发送验证码",
  VERIFICATION_MISMATCH: "验证码错误",
  VERIFICATION_TOO_MANY_ATTEMPTS: "验证码尝试次数过多，请重新发送",
  INVALID_CREDENTIALS: "账号或密码错误",
  PASSWORD_POLICY_VIOLATION: "密码长度至少8位，需包含字母和数字",
  TERMS_NOT_ACCEPTED: "请同意服务条款",
  REFRESH_TOKEN_INVALID: "登录已过期，请重新登录",
  BAD_REQUEST: "请求参数错误",
  INTERNAL_ERROR: "服务器内部错误，请稍后再试",
};

export const PAGE_SIZE = 20;

export function detectIdentifierType(identifier: string): "PHONE" | "EMAIL" {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(identifier.trim()) ? "EMAIL" : "PHONE";
}
