import { useCallback } from "react";
import { useDropzone } from "react-dropzone";
import { Upload, X } from "lucide-react";
import { cn } from "../../lib/cn";

interface ImageUploadProps {
  images: { url: string; file?: File }[];
  onChange: (images: { url: string; file?: File }[]) => void;
  maxImages?: number;
}

export function ImageUpload({
  images,
  onChange,
  maxImages = 9,
}: ImageUploadProps) {
  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      const remaining = maxImages - images.length;
      if (remaining <= 0) return;

      const filesToAdd = acceptedFiles.slice(0, remaining);
      const newImages = filesToAdd.map((file) => ({
        url: URL.createObjectURL(file),
        file,
      }));

      onChange([...images, ...newImages]);
    },
    [images, onChange, maxImages]
  );

  const removeImage = (index: number) => {
    const image = images[index];
    // Revoke blob URL to avoid memory leak
    if (image.file) {
      URL.revokeObjectURL(image.url);
    }
    onChange(images.filter((_, i) => i !== index));
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "image/*": [".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"],
    },
    maxFiles: maxImages - images.length,
    disabled: images.length >= maxImages,
  });

  return (
    <div className="w-full">
      {/* Thumbnail Grid */}
      {images.length > 0 && (
        <div className="mb-3 grid grid-cols-2 gap-3">
          {images.map((image, index) => (
            <div
              key={image.url}
              className="group relative aspect-square overflow-hidden rounded-xl border border-white/70 bg-white/40"
            >
              <img
                src={image.url}
                alt={`上传图片 ${index + 1}`}
                className="h-full w-full object-cover"
              />
              {/* Remove overlay */}
              <button
                type="button"
                onClick={() => removeImage(index)}
                className="absolute right-1.5 top-1.5 rounded-full bg-black/50 p-1 text-white opacity-0 backdrop-blur-sm transition-opacity group-hover:opacity-100 hover:bg-black/70"
                aria-label="移除图片"
              >
                <X size={14} />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Dropzone Area */}
      {images.length < maxImages && (
        <div
          {...getRootProps()}
          className={cn(
            "cursor-pointer rounded-xl border-2 border-dashed p-8 text-center transition-all duration-200",
            isDragActive
              ? "border-azure-500 bg-azure-100/30"
              : "border-white/50 bg-white/30 hover:border-azure-500/50 hover:bg-white/50",
            images.length === 0 && "py-12"
          )}
        >
          <input {...getInputProps()} />
          <div className="flex flex-col items-center gap-2">
            <div
              className={cn(
                "rounded-full p-3 transition-colors",
                isDragActive
                  ? "bg-azure-100 text-azure-500"
                  : "bg-white/50 text-text-tertiary"
              )}
            >
              <Upload size={24} />
            </div>
            <p className="text-sm text-text-secondary">
              {isDragActive ? "释放以上传图片" : "拖拽图片到此处或点击上传"}
            </p>
            <p className="text-xs text-text-tertiary">
              支持 PNG、JPG、GIF、WebP 格式
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
