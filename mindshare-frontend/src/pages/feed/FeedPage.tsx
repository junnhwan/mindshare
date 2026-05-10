import { useInfiniteQuery } from "@tanstack/react-query";
import { getFeed } from "../../api/knowpost";
import { PostCard } from "../../components/post/PostCard";
import { PostCardSkeleton } from "../../components/post/PostCardSkeleton";
import { useIntersectionObserver } from "../../hooks/useIntersectionObserver";
import { Spinner } from "../../components/ui/Spinner";
import { FileText } from "lucide-react";
import { PAGE_SIZE } from "../../lib/constants";

export function FeedPage() {
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
  } = useInfiniteQuery({
    queryKey: ["feed"],
    queryFn: ({ pageParam = 1 }) => getFeed(pageParam as number, PAGE_SIZE),
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.page + 1 : undefined,
    initialPageParam: 1,
    staleTime: 30_000,
  });

  const [sentinelRef, isIntersecting] = useIntersectionObserver({
    rootMargin: "200px",
  });

  // Load more when sentinel is visible
  if (isIntersecting && hasNextPage && !isFetchingNextPage) {
    fetchNextPage();
  }

  const allPosts = data?.pages.flatMap((page) => page.items) ?? [];

  if (isLoading) {
    return (
      <div>
        <h2 className="mb-6 text-2xl font-bold text-text-primary">首页</h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <PostCardSkeleton key={i} />
          ))}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-20">
        <FileText size={48} className="text-text-tertiary" />
        <p className="mt-4 text-lg font-semibold text-text-primary">加载失败</p>
        <p className="mt-1 text-sm text-text-secondary">无法获取帖子列表，请稍后重试</p>
      </div>
    );
  }

  if (allPosts.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20">
        <FileText size={48} className="text-text-tertiary" />
        <p className="mt-4 text-lg font-semibold text-text-primary">暂无帖子</p>
        <p className="mt-1 text-sm text-text-secondary">还没有人发布帖子，成为第一个分享知识的人吧</p>
      </div>
    );
  }

  return (
    <div>
      <h2 className="mb-6 text-2xl font-bold text-text-primary">首页</h2>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {allPosts.map((post) => (
          <PostCard key={post.id} post={post} />
        ))}
      </div>

      {/* Sentinel for infinite scroll */}
      <div ref={sentinelRef} className="flex items-center justify-center py-8">
        {isFetchingNextPage && <Spinner size="md" />}
        {!hasNextPage && allPosts.length >= PAGE_SIZE && (
          <p className="text-sm text-text-tertiary">已加载全部帖子</p>
        )}
      </div>
    </div>
  );
}
