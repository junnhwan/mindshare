import { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getProfile, updateProfile, uploadAvatar } from "../../api/profile";
import { updateProfileSchema } from "../../lib/validators";
import { GENDER_OPTIONS } from "../../lib/constants";
import { useAuthStore } from "../../auth/AuthProvider";
import { Avatar } from "../../components/ui/Avatar";
import { Input } from "../../components/ui/Input";
import { Textarea } from "../../components/ui/Textarea";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Tag } from "../../components/ui/Tag";
import { Skeleton } from "../../components/ui/Skeleton";
import toast from "react-hot-toast";
import { Plus } from "lucide-react";
import type { z } from "zod";
import type { AuthUser } from "../../types/auth";

type FormValues = z.infer<typeof updateProfileSchema>;

function parseTags(tagsJson?: string | null): string[] {
  if (!tagsJson) return [];
  try {
    const parsed = JSON.parse(tagsJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function ProfileEditPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const setUser = useAuthStore((s) => s.setUser);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [avatarUploading, setAvatarUploading] = useState(false);

  // Load profile
  const { data: profile, isLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
    staleTime: 60_000,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(updateProfileSchema),
    defaultValues: {
      nickname: "",
      bio: "",
      gender: "UNKNOWN",
      birthday: "",
      school: "",
      tagsJson: "",
    },
  });

  // Populate form when profile loads
  useEffect(() => {
    if (profile) {
      reset({
        nickname: profile.nickname ?? "",
        bio: profile.bio ?? "",
        gender: (profile.gender as FormValues["gender"]) ?? "UNKNOWN",
        birthday: profile.birthday ?? "",
        school: profile.school ?? "",
        tagsJson: profile.tagsJson ?? "",
      });
      setTags(parseTags(profile.tagsJson));
      if (profile.avatar) {
        setAvatarPreview(profile.avatar);
      }
    }
  }, [profile, reset]);

  // Update profile mutation
  const updateMutation = useMutation({
    mutationFn: (body: FormValues & { tagsJson: string }) => updateProfile(body),
    onSuccess: (data) => {
      // Merge updated profile into auth store
      const currentUser = useAuthStore.getState().user;
      if (currentUser) {
        setUser({ ...currentUser, ...data } as AuthUser);
      }
      queryClient.invalidateQueries({ queryKey: ["profile"] });
      toast.success("资料已更新");
      navigate("/profile");
    },
    onError: () => toast.error("保存失败，请重试"),
  });

  // Avatar upload
  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setAvatarUploading(true);
    setAvatarPreview(URL.createObjectURL(file));

    try {
      const updated = await uploadAvatar(file);
      const currentUser = useAuthStore.getState().user;
      if (currentUser) {
        setUser({ ...currentUser, ...updated } as AuthUser);
      }
      queryClient.invalidateQueries({ queryKey: ["profile"] });
      toast.success("头像已更新");
    } catch {
      toast.error("头像上传失败");
      // Revert preview to previous avatar
      setAvatarPreview(profile?.avatar ?? null);
    } finally {
      setAvatarUploading(false);
      // Reset file input so the same file can be re-selected
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  // Tag management
  const addTag = () => {
    const trimmed = tagInput.trim();
    if (trimmed && !tags.includes(trimmed)) {
      setTags((prev) => [...prev, trimmed]);
    }
    setTagInput("");
  };

  const removeTag = (tag: string) => {
    setTags((prev) => prev.filter((t) => t !== tag));
  };

  const handleTagKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addTag();
    }
  };

  const onSubmit = (values: FormValues) => {
    updateMutation.mutate({
      ...values,
      tagsJson: JSON.stringify(tags),
    });
  };

  // --- Loading skeleton ---
  if (isLoading) {
    return (
      <div className="mx-auto max-w-lg space-y-6">
        <Skeleton className="h-8 w-32" />
        <Card padding="lg">
          <div className="space-y-5">
            <div className="flex items-center gap-4">
              <Skeleton variant="circular" width={80} height={80} />
              <Skeleton className="h-9 w-24 !rounded-md" />
            </div>
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-28 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <h1 className="text-2xl font-bold text-text-primary">编辑资料</h1>

      <Card padding="lg">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* Avatar */}
          <div className="flex items-center gap-4">
            <Avatar
              src={avatarPreview}
              alt={profile?.nickname ?? "头像"}
              size="xl"
            />
            <div>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleAvatarChange}
              />
              <Button
                type="button"
                variant="secondary"
                size="sm"
                loading={avatarUploading}
                onClick={() => fileInputRef.current?.click()}
              >
                更换头像
              </Button>
              <p className="mt-1 text-xs text-text-tertiary">
                支持 JPG、PNG 格式
              </p>
            </div>
          </div>

          {/* Nickname */}
          <Input
            label="昵称"
            placeholder="输入昵称"
            error={errors.nickname?.message}
            {...register("nickname")}
          />

          {/* Bio */}
          <Textarea
            label="简介"
            placeholder="介绍一下自己..."
            error={errors.bio?.message}
            {...register("bio")}
          />

          {/* Gender */}
          <fieldset>
            <legend className="mb-1.5 text-sm font-medium text-text-primary">
              性别
            </legend>
            <div className="flex gap-4">
              {GENDER_OPTIONS.map((opt) => (
                <label
                  key={opt.value}
                  className="flex items-center gap-1.5 text-sm text-text-secondary cursor-pointer"
                >
                  <input
                    type="radio"
                    value={opt.value}
                    className="accent-azure-500"
                    {...register("gender")}
                  />
                  {opt.label}
                </label>
              ))}
            </div>
          </fieldset>

          {/* Birthday */}
          <Input
            label="生日"
            type="date"
            error={errors.birthday?.message}
            {...register("birthday")}
          />

          {/* School */}
          <Input
            label="学校"
            placeholder="毕业院校"
            error={errors.school?.message}
            {...register("school")}
          />

          {/* Tags */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-text-primary">
              标签
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={handleTagKeyDown}
                placeholder="添加标签后按回车"
                className="flex-1 rounded-md border border-white/70 bg-white/60 px-4 py-2.5 text-sm text-text-primary backdrop-blur-sm placeholder:text-text-tertiary transition-all duration-200 focus:border-azure-500 focus:outline-none focus:ring-2 focus:ring-azure-500/20"
              />
              <Button
                type="button"
                variant="secondary"
                size="md"
                onClick={addTag}
              >
                <Plus size={16} />
              </Button>
            </div>
            {tags.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1.5">
                {tags.map((tag) => (
                  <Tag key={tag} onRemove={() => removeTag(tag)}>
                    {tag}
                  </Tag>
                ))}
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <Button
              type="submit"
              loading={updateMutation.isPending}
              className="flex-1"
            >
              保存
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => navigate("/profile")}
            >
              取消
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
