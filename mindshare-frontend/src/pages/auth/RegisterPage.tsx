import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate } from "react-router-dom";
import { Input } from "../../components/ui/Input";
import { Button } from "../../components/ui/Button";
import { SendCodeForm } from "./SendCodeForm";
import { register as registerApi } from "../../api/auth";
import { login } from "../../api/auth";
import { useAuthStore } from "../../store/authStore";
import { setTokens } from "../../api/client";
import { persistTokens } from "../../auth/tokenStore";
import { registerSchema } from "../../lib/validators";
import { ERROR_MESSAGES } from "../../lib/constants";
import { Eye, EyeOff } from "lucide-react";
import toast from "react-hot-toast";
import type { z } from "zod";

type RegisterFormData = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      identifier: "",
      code: "",
      password: "",
      confirmPassword: "",
      nickname: "",
    },
  });

  const identifier = watch("identifier");
  const code = watch("code");

  const onSubmit = async (data: RegisterFormData) => {
    setLoading(true);
    try {
      await registerApi({
        identifier: data.identifier,
        code: data.code,
        password: data.password,
        nickname: data.nickname || undefined,
      });
      const result = await login({
        identifier: data.identifier,
        password: data.password,
        channel: "PASSWORD",
      });
      setAuth(result.user, result.token.accessToken, result.token.refreshToken);
      setTokens(result.token.accessToken, result.token.refreshToken);
      persistTokens(result.token.accessToken, result.token.refreshToken);
      toast.success("注册成功");
      navigate("/", { replace: true });
    } catch (err: unknown) {
      const e = err as { code?: string; message?: string };
      const msg = e.code ? (ERROR_MESSAGES[e.code] ?? e.message) : (e.message ?? "注册失败");
      toast.error(msg || "注册失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 className="text-xl font-semibold text-text-primary text-center">创建账号</h2>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
        <SendCodeForm
          identifier={identifier}
          onIdentifierChange={(v) => setValue("identifier", v, { shouldValidate: true })}
          code={code}
          onCodeChange={(v) => setValue("code", v, { shouldValidate: true })}
        />
        {errors.identifier && (
          <p className="text-xs text-error">{errors.identifier.message}</p>
        )}
        {errors.code && (
          <p className="text-xs text-error">{errors.code.message}</p>
        )}

        <Input
          label="密码"
          type={showPassword ? "text" : "password"}
          placeholder="至少8位，需包含字母和数字"
          error={errors.password?.message}
          rightIcon={showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          onRightIconClick={() => setShowPassword(!showPassword)}
          {...register("password")}
        />
        <Input
          label="确认密码"
          type="password"
          placeholder="再次输入密码"
          error={errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />
        <Input
          label="昵称 (选填)"
          placeholder="设置你的昵称"
          {...register("nickname")}
        />

        <Button type="submit" loading={loading} className="w-full" size="lg">
          注册
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-text-secondary">
        已有账号？{" "}
        <Link to="/login" className="text-azure-500 hover:text-azure-600 font-medium">
          立即登录
        </Link>
      </p>
    </div>
  );
}
