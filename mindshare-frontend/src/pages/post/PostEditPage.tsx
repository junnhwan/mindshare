import { useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { Save } from "lucide-react";
import { getDetail, patchMetadata } from "../../api/knowpost";
import { patchMetadataSchema } from "../../lib/validators";
import { VISIBILITY_OPTIONS } from "../../lib/constants";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Textarea } from "../../components/ui/Textarea";
import { TagInput } from "../../components/ui/TagInput";
import { Card } from "../../components/ui/Card";
import { Spinner } from "../../components/ui/Spinner";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type EditFormValues = z.infer<typeof patchMetadataSchema>;

// ---------------------------------------------------------------------------
// Page Component
// ---------------------------------------------------------------------------

export function PostEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // ---- Load existing post ----
  const {
    data: post,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["post", id],
    queryFn: () => getDetail(id!),
    enabled: !!id,
    staleTime: 60_000,
  });

  // ---- Form ----
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<EditFormValues>({
    resolver: zodResolver(patchMetadataSchema),
    defaultValues: {
      title: "",
      tags: [],
      imgUrls: [],
      description: "",
      visible: "public",
      isTop: false,
    },
  });

  // Pre-fill form once post data arrives
  useEffect(() => {
    if (post) {
      reset({
        title: post.title ?? "",
        tags: post.tags ?? [],
        imgUrls: post.imgUrls ?? [],
        description: post.description ?? "",
        visible: post.visible ?? "public",
        isTop: post.isTop ?? false,
      });
    }
  }, [post, reset]);

  // ---- Save mutation ----
  const saveMutation = useMutation({
    mutationFn: (data: EditFormValues) => patchMetadata(id!, data),
    onSuccess: () => {
      toast.success("修改已保存");
      navigate(`/post/${id}`);
    },
    onError: (err) => {
      toast.error(err instanceof Error ? err.message : "保存失败");
    },
  });

  const onSubmit = (data: EditFormValues) => {
    saveMutation.mutate(data);
  };

  // ---- Loading state ----
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  // ---- Error / not found state ----
  if (isError || !post) {
    return (
      <div className="py-20 text-center">
        <p className="text-lg font-semibold text-text-primary">帖子不存在</p>
        <p className="mt-1 text-sm text-text-secondary">
          该帖子可能已被删除或你没有访问权限
        </p>
        <Link
          to="/"
          className="mt-4 inline-block text-azure-500 hover:text-azure-600 transition-colors"
        >
          返回首页
        </Link>
      </div>
    );
  }

  const isSaving = isSubmitting || saveMutation.isPending;

  // ---- Render ----
  return (
    <div className="mx-auto max-w-2xl">
      {/* Back link */}
      <Link
        to={`/post/${id}`}
        className="mb-6 inline-block text-sm text-text-secondary hover:text-azure-500 transition-colors"
      >
        &larr; 返回帖子
      </Link>

      <Card padding="lg">
        <h1 className="text-xl font-bold text-text-primary">编辑帖子</h1>

        <form
          id="edit-form"
          onSubmit={handleSubmit(onSubmit)}
          className="mt-6 flex flex-col gap-5"
        >
          {/* Title */}
          <Input
            label="标题"
            placeholder="请输入帖子标题"
            {...register("title")}
            error={errors.title?.message}
          />

          {/* Tags */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-text-primary">
              标签
            </label>
            <Controller
              name="tags"
              control={control}
              render={({ field }) => (
                <TagInput
                  value={field.value ?? []}
                  onChange={field.onChange}
                  placeholder="输入标签后按回车"
                  maxTags={20}
                />
              )}
            />
            {errors.tags?.message && (
              <p className="mt-1 text-xs text-error">{errors.tags.message}</p>
            )}
          </div>

          {/* Description */}
          <Textarea
            label="摘要"
            placeholder="简短描述帖子内容（可选）"
            {...register("description")}
            error={errors.description?.message}
            rows={3}
          />

          {/* Visibility */}
          <fieldset>
            <legend className="mb-2.5 text-sm font-medium text-text-primary">
              可见性
            </legend>
            <div className="flex flex-wrap gap-x-6 gap-y-2">
              {VISIBILITY_OPTIONS.map((opt) => (
                <label
                  key={opt.value}
                  className="flex cursor-pointer items-center gap-2"
                >
                  <input
                    type="radio"
                    value={opt.value}
                    {...register("visible")}
                    className="h-4 w-4 border-white/60 bg-white/40 text-azure-500 focus:ring-azure-500/30"
                  />
                  <span className="text-sm text-text-secondary">{opt.label}</span>
                </label>
              ))}
            </div>
          </fieldset>

          {/* IsTop */}
          <label className="flex cursor-pointer items-center gap-2">
            <input
              type="checkbox"
              {...register("isTop")}
              className="h-4 w-4 rounded border-white/60 bg-white/40 text-azure-500 focus:ring-azure-500/30"
            />
            <span className="text-sm text-text-secondary">置顶帖子</span>
          </label>

          {/* Action buttons */}
          <div className="mt-2 flex items-center justify-end gap-3 border-t border-white/20 pt-5">
            <Button
              variant="ghost"
              size="md"
              type="button"
              onClick={() => navigate(`/post/${id}`)}
            >
              取消
            </Button>

            <Button
              variant="primary"
              size="md"
              type="submit"
              loading={isSaving}
              icon={<Save size={18} />}
            >
              保存修改
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
