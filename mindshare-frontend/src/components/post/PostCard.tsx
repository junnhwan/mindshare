import { Link } from "react-router-dom";
import { Avatar } from "../ui/Avatar";
import { Tag } from "../ui/Tag";
import { PostActions } from "./PostActions";
import { formatRelativeTime } from "../../lib/format";
import type { FeedItem } from "../../types/post";

interface PostCardProps {
  post: FeedItem;
  liked?: boolean;
  faved?: boolean;
  likeCount?: number;
  favCount?: number;
}

export function PostCard({
  post,
  liked = false,
  faved = false,
  likeCount = 0,
  favCount = 0,
}: PostCardProps) {
  const coverImage = post.imgUrls && post.imgUrls.length > 0 ? post.imgUrls[0] : null;

  return (
    <Link
      to={`/post/${post.id}`}
      className="glass group block rounded-xl transition-all duration-300 hover:shadow-glass-hover hover:-translate-y-0.5"
    >
      {/* Cover image */}
      {coverImage && (
        <div className="relative aspect-[16/9] overflow-hidden rounded-t-xl">
          <img
            src={coverImage}
            alt={post.title}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
          {post.isTop && (
            <span className="absolute left-3 top-3 rounded-full bg-amber-400/90 px-2.5 py-0.5 text-[10px] font-semibold text-white backdrop-blur-sm">
              置顶
            </span>
          )}
        </div>
      )}

      <div className={`${coverImage ? "p-5" : "p-5"}`}>
        {/* Title */}
        <h3 className="text-lg font-semibold text-text-primary leading-snug line-clamp-2 group-hover:text-azure-500 transition-colors">
          {post.isTop && !coverImage && (
            <span className="mr-1.5 inline-block rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-400">
              置顶
            </span>
          )}
          {post.title}
        </h3>

        {/* Description */}
        {post.description && (
          <p className="mt-2 text-sm text-text-secondary line-clamp-2">
            {post.description}
          </p>
        )}

        {/* Tags */}
        {post.tags && post.tags.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1.5">
            {post.tags.slice(0, 5).map((tag) => (
              <Tag key={tag}>{tag}</Tag>
            ))}
          </div>
        )}

        {/* Footer: Author + Actions */}
        <div className="mt-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Avatar
              src={post.authorAvatar}
              alt={post.authorNickname}
              size="xs"
            />
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-text-primary">
                {post.authorNickname}
              </span>
              <span className="text-xs text-text-tertiary">
                {formatRelativeTime(post.publishTime)}
              </span>
            </div>
          </div>

          {/* Actions (stop link propagation) */}
          <div onClick={(e) => e.preventDefault()}>
            <PostActions
              postId={post.id}
              liked={liked}
              faved={faved}
              likeCount={likeCount}
              favCount={favCount}
              size="sm"
            />
          </div>
        </div>
      </div>
    </Link>
  );
}
