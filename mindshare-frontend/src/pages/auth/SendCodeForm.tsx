import { useState, useCallback, type ChangeEvent } from "react";
import { Input } from "../../components/ui/Input";
import { Button } from "../../components/ui/Button";
import { sendCode } from "../../api/auth";
import toast from "react-hot-toast";
import { ERROR_MESSAGES } from "../../lib/constants";

interface SendCodeFormProps {
  identifier: string;
  onIdentifierChange: (value: string) => void;
  code: string;
  onCodeChange: (value: string) => void;
  identifierLabel?: string;
}

export function SendCodeForm({
  identifier,
  onIdentifierChange,
  code,
  onCodeChange,
  identifierLabel = "手机号 / 邮箱",
}: SendCodeFormProps) {
  const [countdown, setCountdown] = useState(0);

  const handleSendCode = useCallback(async () => {
    if (!identifier.trim()) {
      toast.error("请先输入手机号或邮箱");
      return;
    }
    try {
      await sendCode({ identifier: identifier.trim() });
      toast.success("验证码已发送");
      setCountdown(60);
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } catch (err: unknown) {
      const e = err as { code?: string; message?: string };
      const msg = e.code ? (ERROR_MESSAGES[e.code] ?? e.message) : (e.message ?? "发送失败");
      toast.error(msg || "发送失败");
    }
  }, [identifier]);

  return (
    <div className="space-y-4">
      <Input
        label={identifierLabel}
        placeholder="请输入手机号或邮箱"
        value={identifier}
        onChange={(e: ChangeEvent<HTMLInputElement>) => onIdentifierChange(e.target.value)}
      />
      <div>
        <label className="mb-1.5 block text-sm font-medium text-text-primary">
          验证码
        </label>
        <div className="flex gap-3">
          <Input
            placeholder="6位验证码"
            value={code}
            onChange={(e: ChangeEvent<HTMLInputElement>) => onCodeChange(e.target.value)}
            maxLength={6}
            className="flex-1"
          />
          <Button
            type="button"
            variant="secondary"
            size="md"
            onClick={handleSendCode}
            disabled={countdown > 0}
            className="shrink-0 whitespace-nowrap"
          >
            {countdown > 0 ? `${countdown}s` : "发送验证码"}
          </Button>
        </div>
      </div>
    </div>
  );
}
