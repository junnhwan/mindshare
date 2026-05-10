import { useSearchParams } from "react-router-dom";
import { useInfiniteQuery } from "@tanstack/react-query";
import { getFollowing } from "../../api/relation";
import { Avatar } from "../../components/ui/Avatar";
import { Card } from "../../components/ui/Card";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { FollowButton } from "../../components/user/FollowButton";
import { useIntersectionObserver } from "../../hooks/useIntersectionObserver";
import { PAGE_SIZE } from "../../lib/constants";
import { Users } from "lucide-react";

export function FollowingPage() {
  const [searchParams] = useSearchParams();
  const userIdParam = searchParams.get("userId");
  const userId = userIdParam ? Number(userIdParam) : undefined;

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
  } = useInfiniteQuery({
    queryKey: ["following", userId],
    queryFn: ({ pageParam = 1 }) =>
      getFollowing({ userId, page: pageParam as number, size: PAGE_SIZE }),
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.page + 1 : undefined,
    initialPageParam: 1,
    staleTime: 30_000,
  });

  const [sentinelRef, isIntersecting] = useIntersectionObserver({
    rootMargin: "200px",
  });

  if (isIntersecting && hasNextPage && !isFetchingNextPage) {
    fetchNextPage();
  }

  const users = data?.pages.flatMap((page) => page.items) ?? [];

  // --- Loading skeleton ---
  if (isLoading) {
    return (
      <div className="space-y-3">
        <h1 className="text-2xl font-bold text-text-primary">关注列表</h1>
        {Array.from({ length: 5 }).map((_, i) => (
          <Card key={i} padding="md">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 animate-pulse rounded-full bg-cream-200" />
              <div className="flex-1 space-y-2">
                <div className="h-4 w-24 animate-pulse rounded bg-cream-200" />
                <div className="h-3 w-48 animate-pulse rounded bg-cream-200" />
              </div>
              <div className="h-8 w-16 animate-pulse rounded-md bg-cream-200" />
            </div>
          </Card>
        ))}
      </div>
    );
  }

  // --- Error ---
  if (isError) {
    return (
      <EmptyState
        icon={<Users size={48} />}
        title="加载失败"
        description="无法获取关注列表，请稍后重试"
      />
    );
  }

  return (
    <div className="space-y-3">
      <h1 className="text-2xl font-bold text-text-primary">关注列表</h1>

      {users.length === 0 ? (
        <EmptyState
          icon={<Users size={48} />}
          title="暂无关注"
          description="你还没有关注任何人"
        />
      ) : (
        <>
          {users.map((user) => (
            <Card key={user.id} padding="md">
              <div className="flex items-center gap-4">
                <Avatar src={user.avatar} alt={user.nickname} size="lg" />

                <div className="min-w-0 flex-1">
                  <h3 className="truncate text-sm font-semibold text-text-primary">
                    {user.nickname}
                  </h3>
                  {user.bio ? (
                    <p className="mt-0.5 line-clamp-1 text-xs text-text-secondary">
                      {user.bio}
                    </p>
                  ) : user.zgId ? (
                    <p className="mt-0.5 text-xs text-text-tertiary">
                      @{user.zgId}
                    </p>
                  ) : null}
                </div>

                <FollowButton userId={user.id} size="sm" />
              </div>
            </Card>
          ))}

          {/* Infinite scroll sentinel */}
          <div
            ref={sentinelRef}
            className="flex items-center justify-center py-8"
          >
            {isFetchingNextPage && <Spinner size="md" />}
            {!hasNextPage && users.length >= PAGE_SIZE && (
              <p className="text-sm text-text-tertiary">已加载全部</p>
            )}
          </div>
        </>
      )}
    </div>
  );
}
