import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { getDetail } from "../../api/knowpost";
import { PostActions } from "../../components/post/PostActions";
import { Avatar } from "../../components/ui/Avatar";
import { Tag } from "../../components/ui/Tag";
import { Badge } from "../../components/ui/Badge";
import { Spinner } from "../../components/ui/Spinner";
import { formatDateTime } from "../../lib/format";
import { useState } from "react";

const VISIBILITY_LABELS: Record<string, string> = {
  public: "公开",
  followers: "粉丝可见",
  school: "校友可见",
  private: "仅自己",
  unlisted: "不公开到列表",
};

export function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [lightboxIndex, setLightboxIndex] = useState(0);

  const { data: post, isLoading, isError } = useQuery({
    queryKey: ["post", id],
    queryFn: () => getDetail(id!),
    enabled: !!id,
    staleTime: 60_000,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !post) {
    return (
      <div className="py-20 text-center">
        <p className="text-lg font-semibold text-text-primary">帖子不存在</p>
        <Link to="/" className="mt-4 inline-block text-azure-500 hover:text-azure-600">
          返回首页
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      {/* Back link */}
      <Link
        to="/"
        className="mb-6 inline-block text-sm text-text-secondary hover:text-azure-500 transition-colors"
      >
        ← 返回首页
      </Link>

      {/* Hero section */}
      <div className="glass rounded-2xl p-6 md:p-8">
        {/* Title */}
        <h1 className="text-2xl font-bold text-text-primary md:text-3xl">
          {post.title}
        </h1>

        {/* Meta */}
        <div className="mt-4 flex flex-wrap items-center gap-4">
          <Link
            to={`/profile/${post.creatorId}`}
            className="flex items-center gap-2 hover:opacity-80 transition-opacity"
          >
            <Avatar src={post.authorAvatar} alt={post.authorNickname} size="md" />
            <div>
              <p className="text-sm font-medium text-text-primary">
                {post.authorNickname}
              </p>
              {post.publishTime && (
                <p className="text-xs text-text-tertiary">
                  {formatDateTime(post.publishTime)}
                </p>
              )}
            </div>
          </Link>

          {post.visible && post.visible !== "public" && (
            <Badge variant="amber">
              {VISIBILITY_LABELS[post.visible] || post.visible}
            </Badge>
          )}
          {post.isTop && <Badge variant="amber">置顶</Badge>}
        </div>

        {/* Tags */}
        {post.tags && post.tags.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-1.5">
            {post.tags.map((tag) => (
              <Tag key={tag} variant="azure" size="md">
                {tag}
              </Tag>
            ))}
          </div>
        )}

        {/* Description */}
        {post.description && (
          <p className="mt-4 text-text-secondary leading-relaxed">
            {post.description}
          </p>
        )}

        {/* Images */}
        {post.imgUrls && post.imgUrls.length > 0 && (
          <div className="mt-6 grid grid-cols-2 gap-2 md:grid-cols-3">
            {post.imgUrls.map((url, i) => (
              <button
                key={i}
                onClick={() => {
                  setLightboxIndex(i);
                  setLightboxOpen(true);
                }}
                className="aspect-square overflow-hidden rounded-xl"
              >
                <img
                  src={url}
                  alt={`${post.title} - 图片 ${i + 1}`}
                  className="h-full w-full object-cover transition-transform hover:scale-105"
                />
              </button>
            ))}
          </div>
        )}

        {/* Content */}
        {post.contentUrl && (
          <div className="mt-8 border-t border-white/70 pt-6">
            <div className="prose prose-sm max-w-none text-text-primary">
              <p className="text-text-secondary">
                内容已上传至{" "}
                <a
                  href={post.contentUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-azure-500 hover:underline"
                >
                  查看原文
                </a>
              </p>
            </div>
          </div>
        )}

        {/* Post Actions */}
        <div className="mt-6 border-t border-white/70 pt-4">
          <PostActions
            postId={post.id}
            likeCount={0}
            favCount={0}
          />
        </div>
      </div>

      {/* Lightbox */}
      {lightboxOpen && post.imgUrls && (
        <div
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-sm"
          onClick={() => setLightboxOpen(false)}
        >
          <button
            className="absolute right-4 top-4 text-white/80 hover:text-white"
            onClick={() => setLightboxOpen(false)}
          >
            ✕
          </button>
          <img
            src={post.imgUrls[lightboxIndex]}
            alt=""
            className="max-h-[90vh] max-w-[90vw] rounded-xl object-contain"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );
}
