import { useInfiniteQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { Search, AlertCircle } from "lucide-react";
import { search } from "../../api/search";
import { SearchBar } from "../../components/search/SearchBar";
import { PostCard } from "../../components/post/PostCard";
import { PostCardSkeleton } from "../../components/post/PostCardSkeleton";
import { Spinner } from "../../components/ui/Spinner";
import { useIntersectionObserver } from "../../hooks/useIntersectionObserver";
import { PAGE_SIZE } from "../../lib/constants";

export function SearchPage() {
  const [searchParams] = useSearchParams();
  const q = searchParams.get("q")?.trim() ?? "";

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
    error,
  } = useInfiniteQuery({
    queryKey: ["search", q],
    queryFn: ({ pageParam }) =>
      search({ q, size: PAGE_SIZE, after: pageParam }),
    getNextPageParam: (lastPage) => {
      if (!lastPage.hasMore || !lastPage.nextAfter) return undefined;
      return lastPage.nextAfter;
    },
    initialPageParam: undefined as string | undefined,
    enabled: q.length > 0,
    staleTime: 30_000,
  });

  const [sentinelRef, isIntersecting] = useIntersectionObserver({
    rootMargin: "200px",
  });

  // Trigger next page when the sentinel scrolls into view
  if (isIntersecting && hasNextPage && !isFetchingNextPage) {
    fetchNextPage();
  }

  const allPosts = data?.pages.flatMap((page) => page.items) ?? [];

  // --- State: no query entered yet ---
  if (!q) {
    return (
      <div>
        <SearchBar autoFocus className="mb-8" />
        <div className="flex flex-col items-center justify-center py-20">
          <div className="glass mb-6 flex h-20 w-20 items-center justify-center rounded-2xl">
            <Search size={32} className="text-azure-500" />
          </div>
          <h2 className="text-lg font-semibold text-text-primary">
            搜索知识帖子
          </h2>
          <p className="mt-2 text-sm text-text-secondary">
            输入关键词，搜索你感兴趣的思维碎片与知识总结
          </p>
        </div>
      </div>
    );
  }

  // --- State: loading first page ---
  if (isLoading) {
    return (
      <div>
        <SearchBar className="mb-8" />
        <p className="mb-4 text-sm text-text-secondary">
          正在搜索 &quot;{q}&quot;...
        </p>
        <div className="flex flex-col gap-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <PostCardSkeleton key={i} />
          ))}
        </div>
      </div>
    );
  }

  // --- State: error ---
  if (isError) {
    return (
      <div>
        <SearchBar className="mb-8" />
        <div className="flex flex-col items-center justify-center py-20">
          <div className="glass mb-6 flex h-20 w-20 items-center justify-center rounded-2xl">
            <AlertCircle size={32} className="text-red-400" />
          </div>
          <h2 className="text-lg font-semibold text-text-primary">搜索失败</h2>
          <p className="mt-2 text-sm text-text-secondary">
            {(error as Error)?.message || "无法完成搜索，请稍后重试"}
          </p>
        </div>
      </div>
    );
  }

  // --- State: no results ---
  if (allPosts.length === 0) {
    return (
      <div>
        <SearchBar className="mb-8" />
        <p className="mb-4 text-sm text-text-secondary">
          搜索 &quot;{q}&quot; 的结果
        </p>
        <div className="flex flex-col items-center justify-center py-20">
          <div className="glass mb-6 flex h-20 w-20 items-center justify-center rounded-2xl">
            <Search size={32} className="text-text-tertiary" />
          </div>
          <h2 className="text-lg font-semibold text-text-primary">
            未找到相关结果
          </h2>
          <p className="mt-2 text-sm text-text-secondary">
            请尝试使用不同的关键词搜索
          </p>
        </div>
      </div>
    );
  }

  // --- State: results ---
  return (
    <div>
      <SearchBar className="mb-8" />

      <p className="mb-4 text-sm text-text-secondary">
        搜索 &quot;{q}&quot; 的结果
      </p>

      <div className="flex flex-col gap-4">
        {allPosts.map((post) => (
          <PostCard key={post.id} post={post} />
        ))}
      </div>

      {/* Sentinel for infinite scroll */}
      <div
        ref={sentinelRef}
        className="flex items-center justify-center py-8"
      >
        {isFetchingNextPage && <Spinner size="md" />}
        {!hasNextPage && allPosts.length >= PAGE_SIZE && (
          <p className="text-sm text-text-tertiary">已加载全部结果</p>
        )}
      </div>
    </div>
  );
}
