import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link } from "react-router-dom";
import { Input } from "../../components/ui/Input";
import { Button } from "../../components/ui/Button";
import { SendCodeForm } from "./SendCodeForm";
import { resetPassword } from "../../api/auth";
import { resetPasswordSchema } from "../../lib/validators";
import { ERROR_MESSAGES, detectIdentifierType } from "../../lib/constants";
import { Eye, EyeOff } from "lucide-react";
import toast from "react-hot-toast";
import type { z } from "zod";

type ResetFormData = z.infer<typeof resetPasswordSchema>;

export function ForgotPasswordPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ResetFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      identifier: "",
      code: "",
      newPassword: "",
      confirmPassword: "",
    },
  });

  const identifier = watch("identifier");
  const code = watch("code");

  const onSubmit = async (data: ResetFormData) => {
    setLoading(true);
    try {
      await resetPassword({
        identifierType: detectIdentifierType(data.identifier),
        identifier: data.identifier,
        code: data.code,
        newPassword: data.newPassword,
      });
      toast.success("密码重置成功");
      setDone(true);
    } catch (err: unknown) {
      const e = err as { code?: string; message?: string };
      const msg = e.code ? (ERROR_MESSAGES[e.code] ?? e.message) : (e.message ?? "重置失败");
      toast.error(msg || "重置失败");
    } finally {
      setLoading(false);
    }
  };

  if (done) {
    return (
      <div className="text-center">
        <h2 className="text-xl font-semibold text-text-primary">密码已重置</h2>
        <p className="mt-2 text-sm text-text-secondary">
          请使用新密码登录
        </p>
        <Link to="/login" className="mt-6 inline-block">
          <Button>返回登录</Button>
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-xl font-semibold text-text-primary text-center">重置密码</h2>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
        <SendCodeForm
          identifier={identifier}
          onIdentifierChange={(v) => setValue("identifier", v, { shouldValidate: true })}
          code={code}
          onCodeChange={(v) => setValue("code", v, { shouldValidate: true })}
          scene="RESET_PASSWORD"
        />
        {errors.identifier && (
          <p className="text-xs text-error">{errors.identifier.message}</p>
        )}
        {errors.code && (
          <p className="text-xs text-error">{errors.code.message}</p>
        )}

        <Input
          label="新密码"
          type={showPassword ? "text" : "password"}
          placeholder="至少8位，需包含字母和数字"
          error={errors.newPassword?.message}
          rightIcon={showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          onRightIconClick={() => setShowPassword(!showPassword)}
          {...register("newPassword")}
        />
        <Input
          label="确认新密码"
          type="password"
          placeholder="再次输入新密码"
          error={errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />

        <Button type="submit" loading={loading} className="w-full" size="lg">
          重置密码
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-text-secondary">
        <Link to="/login" className="text-azure-500 hover:text-azure-600 font-medium">
          返回登录
        </Link>
      </p>
    </div>
  );
}
