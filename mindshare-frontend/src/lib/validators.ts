import { z } from "zod";

export const loginSchema = z.object({
  identifier: z.string().min(1, "请输入手机号或邮箱"),
  password: z.string().min(1, "请输入密码"),
});

export const loginCodeSchema = z.object({
  identifier: z.string().min(1, "请输入手机号或邮箱"),
  code: z.string().length(6, "验证码为6位数字"),
  channel: z.literal("CODE"),
});

export const registerSchema = z
  .object({
    identifier: z.string().min(1, "请输入手机号或邮箱"),
    code: z.string().length(6, "验证码为6位数字"),
    password: z
      .string()
      .min(8, "密码至少8位")
      .regex(/[a-zA-Z]/, "密码需包含字母")
      .regex(/[0-9]/, "密码需包含数字"),
    confirmPassword: z.string(),
    nickname: z.string().optional(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "两次密码输入不一致",
    path: ["confirmPassword"],
  });

export const sendCodeSchema = z.object({
  identifier: z.string().min(1, "请输入手机号或邮箱"),
});

export const resetPasswordSchema = z
  .object({
    identifier: z.string().min(1, "请输入手机号或邮箱"),
    code: z.string().length(6, "验证码为6位数字"),
    newPassword: z
      .string()
      .min(8, "密码至少8位")
      .regex(/[a-zA-Z]/, "密码需包含字母")
      .regex(/[0-9]/, "密码需包含数字"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "两次密码输入不一致",
    path: ["confirmPassword"],
  });

export const patchMetadataSchema = z.object({
  title: z.string().min(1, "请输入标题").max(255, "标题过长").optional(),
  tags: z.array(z.string()).max(20, "最多20个标签").optional(),
  imgUrls: z.array(z.string()).optional(),
  description: z.string().max(512, "摘要过长").optional(),
  visible: z
    .enum(["public", "followers", "school", "private", "unlisted"])
    .optional(),
  isTop: z.boolean().optional(),
});

export const updateProfileSchema = z.object({
  nickname: z.string().max(64, "昵称过长").optional(),
  bio: z.string().max(512, "简介过长").optional(),
  gender: z.enum(["MALE", "FEMALE", "OTHER", "UNKNOWN"]).optional(),
  birthday: z.string().optional(),
  school: z.string().max(128, "学校名过长").optional(),
  tagsJson: z.string().optional(),
});
