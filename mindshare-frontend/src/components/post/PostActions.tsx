import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Heart, Bookmark } from "lucide-react";
import { like, unlike, fav, unfav } from "../../api/counter";
import { formatCount } from "../../lib/format";
import { useAuth } from "../../auth/AuthProvider";
import toast from "react-hot-toast";

interface PostActionsProps {
  postId: number;
  liked?: boolean;
  faved?: boolean;
  likeCount?: number;
  favCount?: number;
  size?: "sm" | "md";
}

export function PostActions({
  postId,
  liked = false,
  faved = false,
  likeCount = 0,
  favCount = 0,
  size = "md",
}: PostActionsProps) {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  const iconSize = size === "sm" ? 16 : 18;
  const textSize = size === "sm" ? "text-xs" : "text-sm";

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["counts", "knowpost", postId] });
    queryClient.invalidateQueries({ queryKey: ["feed"] });
  };

  const likeMutation = useMutation({
    mutationFn: () =>
      liked ? unlike({ entityType: "knowpost", entityId: postId }) : like({ entityType: "knowpost", entityId: postId }),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ["counts", "knowpost", postId] });
      const prev = queryClient.getQueryData<{ like: number; fav: number }>(["counts", "knowpost", postId]);
      if (prev) {
        queryClient.setQueryData(["counts", "knowpost", postId], {
          ...prev,
          like: liked ? prev.like - 1 : prev.like + 1,
        });
      }
      return { prev };
    },
    onError: (_err, _vars, context) => {
      if (context?.prev) {
        queryClient.setQueryData(["counts", "knowpost", postId], context.prev);
      }
      toast.error("操作失败，请重试");
    },
    onSettled: invalidate,
  });

  const favMutation = useMutation({
    mutationFn: () =>
      faved ? unfav({ entityType: "knowpost", entityId: postId }) : fav({ entityType: "knowpost", entityId: postId }),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ["counts", "knowpost", postId] });
      const prev = queryClient.getQueryData<{ like: number; fav: number }>(["counts", "knowpost", postId]);
      if (prev) {
        queryClient.setQueryData(["counts", "knowpost", postId], {
          ...prev,
          fav: faved ? prev.fav - 1 : prev.fav + 1,
        });
      }
      return { prev };
    },
    onError: (_err, _vars, context) => {
      if (context?.prev) {
        queryClient.setQueryData(["counts", "knowpost", postId], context.prev);
      }
      toast.error("操作失败，请重试");
    },
    onSettled: invalidate,
  });

  const handleClick = (mutation: typeof likeMutation) => {
    if (!isAuthenticated) {
      toast.error("请先登录");
      return;
    }
    mutation.mutate();
  };

  // Calculate effective state (query data overrides props if available)
  const countsData = queryClient.getQueryData<{ like: number; fav: number }>(["counts", "knowpost", postId]);
  const effectiveLikeCount = countsData?.like ?? likeCount;
  const effectiveFavCount = countsData?.fav ?? favCount;

  // Determine visual state based on mutation + query data
  const isLiked = likeMutation.isPending
    ? !liked
    : liked;
  const isFaved = favMutation.isPending
    ? !faved
    : faved;

  return (
    <div className={`flex items-center gap-4 ${textSize}`}>
      <button
        onClick={() => handleClick(likeMutation)}
        disabled={likeMutation.isPending}
        className={`flex items-center gap-1.5 transition-colors ${
          isLiked
            ? "text-error"
            : "text-text-tertiary hover:text-error"
        }`}
      >
        <Heart
          size={iconSize}
          fill={isLiked ? "currentColor" : "none"}
          className="transition-transform active:scale-125"
        />
        {effectiveLikeCount > 0 && (
          <span className="font-medium">{formatCount(effectiveLikeCount)}</span>
        )}
      </button>

      <button
        onClick={() => handleClick(favMutation)}
        disabled={favMutation.isPending}
        className={`flex items-center gap-1.5 transition-colors ${
          isFaved
            ? "text-amber-400"
            : "text-text-tertiary hover:text-amber-400"
        }`}
      >
        <Bookmark
          size={iconSize}
          fill={isFaved ? "currentColor" : "none"}
          className="transition-transform active:scale-125"
        />
        {effectiveFavCount > 0 && (
          <span className="font-medium">{formatCount(effectiveFavCount)}</span>
        )}
      </button>
    </div>
  );
}
