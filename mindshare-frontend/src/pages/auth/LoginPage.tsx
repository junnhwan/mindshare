import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Input } from "../../components/ui/Input";
import { Button } from "../../components/ui/Button";
import { SendCodeForm } from "./SendCodeForm";
import { login } from "../../api/auth";
import { useAuthStore } from "../../store/authStore";
import { setTokens } from "../../api/client";
import { persistTokens } from "../../auth/tokenStore";
import { loginSchema } from "../../lib/validators";
import { ERROR_MESSAGES, detectIdentifierType } from "../../lib/constants";
import { Eye, EyeOff } from "lucide-react";
import toast from "react-hot-toast";
import type { z } from "zod";

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginPage() {
  const [tab, setTab] = useState<"password" | "code">("password");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);

  const from = (location.state as { from?: string })?.from || "/";

  // Password form
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { identifier: "", password: "" },
  });

  // Code form
  const [codeIdentifier, setCodeIdentifier] = useState("");
  const [code, setCode] = useState("");

  const onPasswordSubmit = async (data: LoginFormData) => {
    setLoading(true);
    try {
      const result = await login({ identifierType: detectIdentifierType(data.identifier), identifier: data.identifier, password: data.password });
      setAuth(result.user, result.token.accessToken, result.token.refreshToken);
      setTokens(result.token.accessToken, result.token.refreshToken);
      persistTokens(result.token.accessToken, result.token.refreshToken);
      toast.success("登录成功");
      navigate(from, { replace: true });
    } catch (err: unknown) {
      const e = err as { code?: string; message?: string };
      const msg = e.code ? (ERROR_MESSAGES[e.code] ?? e.message) : (e.message ?? "登录失败");
      toast.error(msg || "登录失败");
    } finally {
      setLoading(false);
    }
  };

  const onCodeSubmit = async () => {
    if (!codeIdentifier.trim() || code.length !== 6) return;
    setLoading(true);
    try {
      const result = await login({
        identifierType: detectIdentifierType(codeIdentifier.trim()),
        identifier: codeIdentifier.trim(),
        code,
      });
      setAuth(result.user, result.token.accessToken, result.token.refreshToken);
      setTokens(result.token.accessToken, result.token.refreshToken);
      persistTokens(result.token.accessToken, result.token.refreshToken);
      toast.success("登录成功");
      navigate(from, { replace: true });
    } catch (err: unknown) {
      const e = err as { code?: string; message?: string };
      const msg = e.code ? (ERROR_MESSAGES[e.code] ?? e.message) : (e.message ?? "登录失败");
      toast.error(msg || "登录失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 className="text-xl font-semibold text-text-primary text-center">欢迎回来</h2>

      {/* Tabs */}
      <div className="mt-6 flex rounded-lg bg-white/40 p-1">
        <button
          onClick={() => setTab("password")}
          className={`flex-1 rounded-md py-2 text-sm font-medium transition-all ${
            tab === "password"
              ? "bg-white/80 text-text-primary shadow-sm"
              : "text-text-secondary hover:text-text-primary"
          }`}
        >
          密码登录
        </button>
        <button
          onClick={() => setTab("code")}
          className={`flex-1 rounded-md py-2 text-sm font-medium transition-all ${
            tab === "code"
              ? "bg-white/80 text-text-primary shadow-sm"
              : "text-text-secondary hover:text-text-primary"
          }`}
        >
          验证码登录
        </button>
      </div>

      {/* Password form */}
      {tab === "password" && (
        <form onSubmit={handleSubmit(onPasswordSubmit)} className="mt-6 space-y-4">
          <Input
            label="手机号 / 邮箱"
            placeholder="请输入手机号或邮箱"
            error={errors.identifier?.message}
            {...register("identifier")}
          />
          <Input
            label="密码"
            type={showPassword ? "text" : "password"}
            placeholder="请输入密码"
            error={errors.password?.message}
            rightIcon={showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            onRightIconClick={() => setShowPassword(!showPassword)}
            {...register("password")}
          />
          <div className="flex items-center justify-end">
            <Link
              to="/forgot-password"
              className="text-xs text-azure-500 hover:text-azure-600"
            >
              忘记密码？
            </Link>
          </div>
          <Button type="submit" loading={loading} className="w-full" size="lg">
            登录
          </Button>
        </form>
      )}

      {/* Code form */}
      {tab === "code" && (
        <div className="mt-6 space-y-4">
          <SendCodeForm
            identifier={codeIdentifier}
            onIdentifierChange={setCodeIdentifier}
            code={code}
            onCodeChange={setCode}
            scene="LOGIN"
          />
          <Button
            onClick={onCodeSubmit}
            loading={loading}
            className="w-full"
            size="lg"
          >
            登录
          </Button>
        </div>
      )}

      {/* Register link */}
      <p className="mt-6 text-center text-sm text-text-secondary">
        还没有账号？{" "}
        <Link to="/register" className="text-azure-500 hover:text-azure-600 font-medium">
          立即注册
        </Link>
      </p>
    </div>
  );
}
