import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import { ChevronLeft, ChevronRight, Check, Upload, FileText } from "lucide-react";
import { createDraft, confirmContent, patchMetadata, publish } from "../../api/knowpost";
import { presign } from "../../api/storage";
import { patchMetadataSchema } from "../../lib/validators";
import { VISIBILITY_OPTIONS } from "../../lib/constants";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Textarea } from "../../components/ui/Textarea";
import { TagInput } from "../../components/ui/TagInput";
import { ImageUpload } from "../../components/ui/ImageUpload";
import { Card } from "../../components/ui/Card";
import { cn } from "../../lib/cn";

// ---------------------------------------------------------------------------
// Schema & Types
// ---------------------------------------------------------------------------

const createMetadataSchema = patchMetadataSchema.extend({
  title: z.string().min(1, "请输入标题").max(255, "标题过长"),
});

type MetadataFormValues = z.infer<typeof createMetadataSchema>;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Derive the public read URL from a presigned PUT URL (strip query string). */
function getPublicUrl(putUrl: string): string {
  const url = new URL(putUrl);
  return `${url.origin}${url.pathname}`;
}

/** Compute SHA-256 (hex) of a UTF-8 string using Web Crypto. */
async function computeSha256(content: string): Promise<string> {
  const data = new TextEncoder().encode(content);
  const hashBuffer = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

/** Upload a body to a presigned OSS URL. Returns the response ETag. */
async function uploadToOss(
  putUrl: string,
  headers: Record<string, string>,
  body: BodyInit,
): Promise<{ etag: string }> {
  const resp = await fetch(putUrl, { method: "PUT", headers, body });
  if (!resp.ok) {
    const detail = await resp.text().catch(() => "");
    throw new Error(`上传失败 (${resp.status})${detail ? `: ${detail.slice(0, 200)}` : ""}`);
  }
  const etag = resp.headers.get("ETag") ?? resp.headers.get("etag") ?? "";
  return { etag };
}

// ---------------------------------------------------------------------------
// Step definitions
// ---------------------------------------------------------------------------

const STEPS = [
  { num: 1, label: "创建草稿" },
  { num: 2, label: "内容编辑" },
  { num: 3, label: "图片上传" },
  { num: 4, label: "发布信息" },
] as const;

// ---------------------------------------------------------------------------
// Page Component
// ---------------------------------------------------------------------------

export function PostCreatePage() {
  const navigate = useNavigate();

  // ---- Wizard state ----
  const [step, setStep] = useState(1);

  // Step 1
  const [draftId, setDraftId] = useState<string | null>(null);
  const [isCreatingDraft, setIsCreatingDraft] = useState(false);

  // Step 2
  const [content, setContent] = useState("");
  const [contentUploaded, setContentUploaded] = useState(false);
  const [isUploadingContent, setIsUploadingContent] = useState(false);
  const [contentProgress, setContentProgress] = useState<
    "idle" | "presigning" | "uploading" | "confirming" | "done"
  >("idle");

  // Step 3
  const [images, setImages] = useState<{ url: string; file?: File }[]>([]);
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const [isUploadingImages, setIsUploadingImages] = useState(false);

  // Step 4
  const [isPublishing, setIsPublishing] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<MetadataFormValues>({
    resolver: zodResolver(createMetadataSchema),
    defaultValues: {
      title: "",
      tags: [],
      imgUrls: [],
      description: "",
      visible: "public",
      isTop: false,
    },
  });

  // ---- Step actions ----

  const handleCreateDraft = async () => {
    setIsCreatingDraft(true);
    try {
      const { id } = await createDraft();
      setDraftId(id);
      toast.success("草稿已创建");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "创建草稿失败");
    } finally {
      setIsCreatingDraft(false);
    }
  };

  const handleUploadContent = async () => {
    if (!draftId || !content.trim()) return;
    setIsUploadingContent(true);

    try {
      // 1. Presign
      setContentProgress("presigning");
      const presignResult = await presign({
        postId: draftId,
        scene: "knowpost_content",
        contentType: "text/markdown",
        ext: "md",
      });

      // 2. PUT to OSS
      setContentProgress("uploading");
      const blob = new Blob([content], { type: "text/markdown" });
      const { etag } = await uploadToOss(presignResult.putUrl, presignResult.headers, blob);

      // 3. Compute SHA-256 & size
      const sha256 = await computeSha256(content);
      const size = new TextEncoder().encode(content).length;

      // 4. Confirm
      setContentProgress("confirming");
      await confirmContent(draftId, {
        objectKey: presignResult.objectKey,
        etag,
        size,
        sha256,
      });

      setContentProgress("done");
      setContentUploaded(true);
      toast.success("内容已保存");
    } catch (err) {
      setContentProgress("idle");
      setContentUploaded(false);
      toast.error(err instanceof Error ? err.message : "内容上传失败");
    } finally {
      setIsUploadingContent(false);
    }
  };

  const handleUploadImages = async () => {
    if (!draftId || images.length === 0) return;

    const pending = images.filter((img) => img.file);
    if (pending.length === 0) {
      // All images are already uploaded — just advance
      return;
    }

    setIsUploadingImages(true);
    try {
      const urls = new Map(imageUrls.map((u) => [u, true])); // dedup
      const newUrls: string[] = [...imageUrls];

      for (const img of pending) {
        const file = img.file!;
        const ext = file.name.split(".").pop() ?? "png";

        const presignResult = await presign({
          postId: draftId,
          scene: "knowpost_image",
          contentType: file.type,
          ext,
        });

        await uploadToOss(presignResult.putUrl, presignResult.headers, file);

        const publicUrl = getPublicUrl(presignResult.putUrl);
        if (!urls.has(publicUrl)) {
          urls.set(publicUrl, true);
          newUrls.push(publicUrl);
        }
      }

      setImageUrls(newUrls);
      setValue("imgUrls", newUrls);

      // Mark local images as uploaded (remove File references)
      setImages(images.map((img) => ({ url: img.url })));

      toast.success(`已上传 ${pending.length} 张图片`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "图片上传失败");
    } finally {
      setIsUploadingImages(false);
    }
  };

  const handlePublish = async (data: MetadataFormValues) => {
    if (!draftId) return;
    setIsPublishing(true);
    try {
      await patchMetadata(draftId, {
        ...data,
        imgUrls: data.imgUrls?.length ? data.imgUrls : imageUrls,
      });
      await publish(draftId);
      toast.success("帖子已发布");
      navigate(`/post/${draftId}`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "发布失败");
    } finally {
      setIsPublishing(false);
    }
  };

  // ---- Navigation helpers ----

  const canGoNext = (): boolean => {
    switch (step) {
      case 1:
        return draftId !== null;
      case 2:
        return contentUploaded;
      case 3:
        return images.length === 0 || isUploadingImages === false; // images are optional
      default:
        return true;
    }
  };

  const goBack = () => {
    if (step > 1) setStep((s) => s - 1);
  };

  const goNext = () => {
    if (step < 4) setStep((s) => s + 1);
  };

  const handleNext = async () => {
    if (step === 1) {
      if (!draftId) return;
      goNext();
      return;
    }
    if (step === 2) {
      if (!contentUploaded) return;
      goNext();
      return;
    }
    if (step === 3) {
      if (images.some((img) => img.file)) {
        await handleUploadImages();
      }
      goNext();
      return;
    }
    // Step 4 — handled by form submit
  };

  // ---- Progress label for content upload ----
  const contentProgressLabel: Record<string, string> = {
    idle: "上传内容至云端",
    presigning: "正在获取上传凭证...",
    uploading: "正在上传...",
    confirming: "正在确认...",
    done: "内容已保存",
  };

  // ---- Render ----

  return (
    <div className="mx-auto max-w-2xl">
      {/* --- Step Indicator --- */}
      <nav aria-label="发布步骤" className="mb-8">
        <ol className="flex items-center justify-between">
          {STEPS.map((s, idx) => {
            const isActive = step === s.num;
            const isCompleted = step > s.num;
            const isFuture = step < s.num;

            return (
              <li key={s.num} className="flex flex-1 items-center">
                {/* Connector line (except first) */}
                {idx > 0 && (
                  <div
                    className={cn(
                      "h-px flex-1 transition-colors duration-300",
                      step > s.num ? "bg-azure-500" : "bg-white/20",
                    )}
                  />
                )}

                {/* Step circle + label */}
                <div className="flex flex-col items-center gap-1.5">
                  <div
                    className={cn(
                      "flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold transition-all duration-300",
                      isActive && "bg-azure-500 text-white shadow-md shadow-azure-500/30",
                      isCompleted && "bg-azure-500/80 text-white",
                      isFuture && "bg-white/20 text-text-tertiary",
                    )}
                  >
                    {isCompleted ? <Check size={14} /> : s.num}
                  </div>
                  <span
                    className={cn(
                      "text-xs font-medium transition-colors duration-300",
                      isActive && "text-azure-500",
                      isCompleted && "text-azure-500/70",
                      isFuture && "text-text-tertiary",
                    )}
                  >
                    {s.label}
                  </span>
                </div>

                {/* Connector line (except last) */}
                {idx < STEPS.length - 1 && (
                  <div
                    className={cn(
                      "h-px flex-1 transition-colors duration-300",
                      step > s.num ? "bg-azure-500" : "bg-white/20",
                    )}
                  />
                )}
              </li>
            );
          })}
        </ol>
      </nav>

      {/* --- Step Content --- */}
      <Card padding="lg">
        {/* ===== Step 1: Create Draft ===== */}
        {step === 1 && (
          <div className="flex flex-col items-center gap-6 py-8">
            <div className="rounded-full bg-azure-100 p-4 text-azure-500">
              <FileText size={32} />
            </div>
            <div className="text-center">
              <h2 className="text-xl font-bold text-text-primary">创建草稿</h2>
              <p className="mt-1 text-sm text-text-secondary">
                为你的知识分享帖子创建一个草稿
              </p>
            </div>

            {draftId ? (
              <div className="flex items-center gap-2 rounded-lg bg-emerald-50/80 px-4 py-2.5 text-sm text-emerald-700 backdrop-blur-sm">
                <Check size={16} />
                草稿已创建（ID: {draftId}）
              </div>
            ) : (
              <Button
                variant="primary"
                size="lg"
                loading={isCreatingDraft}
                onClick={handleCreateDraft}
                icon={<Upload size={18} />}
              >
                创建草稿
              </Button>
            )}
          </div>
        )}

        {/* ===== Step 2: Content ===== */}
        {step === 2 && (
          <div className="flex flex-col gap-5">
            <div>
              <h2 className="text-lg font-bold text-text-primary">内容编辑</h2>
              <p className="mt-1 text-sm text-text-secondary">
                使用 Markdown 编写帖子内容
              </p>
            </div>

            <Textarea
              label="内容 (Markdown)"
              placeholder="在此编写 Markdown 内容..."
              value={content}
              onChange={(e) => {
                setContent(e.target.value);
                // Invalidate previous upload if content changed after upload
                if (contentUploaded) {
                  setContentUploaded(false);
                  setContentProgress("idle");
                }
              }}
              rows={14}
              disabled={isUploadingContent}
            />

            <div className="flex items-center gap-3">
              <Button
                variant="secondary"
                loading={isUploadingContent}
                disabled={!content.trim() || (contentUploaded && !isUploadingContent)}
                onClick={handleUploadContent}
                icon={contentUploaded ? <Check size={16} /> : <Upload size={16} />}
              >
                {contentProgressLabel[contentProgress]}
              </Button>

              {contentUploaded && (
                <span className="text-xs text-emerald-600">
                  内容已确认保存
                </span>
              )}
            </div>
          </div>
        )}

        {/* ===== Step 3: Images ===== */}
        {step === 3 && (
          <div className="flex flex-col gap-5">
            <div>
              <h2 className="text-lg font-bold text-text-primary">图片上传</h2>
              <p className="mt-1 text-sm text-text-secondary">
                上传帖子配图（可选），支持拖拽
              </p>
            </div>

            <ImageUpload
              images={images}
              onChange={setImages}
              maxImages={9}
            />

            {images.some((img) => img.file) && (
              <Button
                variant="secondary"
                loading={isUploadingImages}
                onClick={handleUploadImages}
                icon={<Upload size={16} />}
              >
                上传 {images.filter((img) => img.file).length} 张图片
              </Button>
            )}

            {imageUrls.length > 0 && !images.some((img) => img.file) && (
              <p className="text-xs text-emerald-600">
                {imageUrls.length} 张图片已上传
              </p>
            )}
          </div>
        )}

        {/* ===== Step 4: Metadata ===== */}
        {step === 4 && (
          <form id="metadata-form" onSubmit={handleSubmit(handlePublish)} className="flex flex-col gap-5">
            <div>
              <h2 className="text-lg font-bold text-text-primary">发布信息</h2>
              <p className="mt-1 text-sm text-text-secondary">
                填写帖子元数据后发布
              </p>
            </div>

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

            {/* Publish button */}
            <Button
              type="submit"
              variant="primary"
              size="lg"
              loading={isPublishing}
              className="mt-2 self-end"
            >
              发布帖子
            </Button>
          </form>
        )}
      </Card>

      {/* --- Bottom Navigation --- */}
      <div className="mt-6 flex items-center justify-between">
        <Button
          variant="ghost"
          size="md"
          disabled={step === 1}
          onClick={goBack}
          icon={<ChevronLeft size={18} />}
        >
          上一步
        </Button>

        <span className="text-sm text-text-tertiary">
          {step} / {STEPS.length}
        </span>

        {step < 4 ? (
          <Button
            variant="primary"
            size="md"
            disabled={!canGoNext()}
            onClick={handleNext}
            icon={<ChevronRight size={18} />}
          >
            下一步
          </Button>
        ) : (
          // Step 4 — bottom button also submits the metadata form
          <Button
            variant="primary"
            size="md"
            form="metadata-form"
            type="submit"
            loading={isPublishing}
          >
            发布帖子
          </Button>
        )}
      </div>
    </div>
  );
}
