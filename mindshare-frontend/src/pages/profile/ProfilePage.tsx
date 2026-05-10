import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useAuthStore } from "../../auth/AuthProvider";
import { getProfile } from "../../api/profile";
import { getRelationCounter } from "../../api/relation";
import { getMine } from "../../api/knowpost";
import { Avatar } from "../../components/ui/Avatar";
import { Card } from "../../components/ui/Card";
import { Tabs } from "../../components/ui/Tabs";
import { Button } from "../../components/ui/Button";
import { Tag } from "../../components/ui/Tag";
import { PostCard } from "../../components/post/PostCard";
import { PostCardSkeleton } from "../../components/post/PostCardSkeleton";
import { Spinner } from "../../components/ui/Spinner";
import { Skeleton } from "../../components/ui/Skeleton";
import { EmptyState } from "../../components/ui/EmptyState";
import { FollowButton } from "../../components/user/FollowButton";
import { useIntersectionObserver } from "../../hooks/useIntersectionObserver";
import { PAGE_SIZE } from "../../lib/constants";
import { FileText } from "lucide-react";

function parseTagsJson(tagsJson?: string | null): string[] {
  if (!tagsJson) return [];
  try {
    const parsed = JSON.parse(tagsJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function ProfilePage() {
  const [searchParams] = useSearchParams();
  const currentUser = useAuthStore((s) => s.user);
  const profileUserId = searchParams.get("userId");
  const viewingUserId = profileUserId ? Number(profileUserId) : currentUser?.id;
  const isOwnProfile = !profileUserId || Number(profileUserId) === currentUser?.id;

  // Own profile from API
  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
    enabled: isOwnProfile,
    staleTime: 60_000,
  });

  // Relation counts
  const { data: counter } = useQuery({
    queryKey: ["relationCounter", viewingUserId],
    queryFn: () => getRelationCounter(viewingUserId!),
    enabled: !!viewingUserId,
    staleTime: 60_000,
  });

  // Posts (own profile only)
  const [activeTab, setActiveTab] = useState("posts");

  const {
    data: postsData,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading: postsLoading,
  } = useInfiniteQuery({
    queryKey: ["myPosts", "published"],
    queryFn: ({ pageParam = 1 }) =>
      getMine(pageParam as number, PAGE_SIZE, "published"),
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.page + 1 : undefined,
    initialPageParam: 1,
    staleTime: 30_000,
    enabled: isOwnProfile,
  });

  const [sentinelRef, isIntersecting] = useIntersectionObserver({
    rootMargin: "200px",
  });

  if (isIntersecting && hasNextPage && !isFetchingNextPage) {
    fetchNextPage();
  }

  const allPosts = postsData?.pages.flatMap((page) => page.items) ?? [];

  // --- Profile header skeleton ---
  if (profileLoading && isOwnProfile) {
    return (
      <div className="space-y-6">
        <Card padding="lg">
          <div className="flex flex-col items-center gap-4 sm:flex-row sm:gap-6">
            <Skeleton variant="circular" width={96} height={96} />
            <div className="flex-1 space-y-3 text-center sm:text-left">
              <Skeleton className="mx-auto h-7 w-40 sm:mx-0" />
              <Skeleton className="mx-auto h-4 w-24 sm:mx-0" />
              <Skeleton className="mx-auto h-4 w-64 sm:mx-0" />
            </div>
            <Skeleton className="h-9 w-24 shrink-0 !rounded-md" />
          </div>
          <div className="mt-6 flex justify-center gap-8 border-t border-white/50 pt-4">
            <Skeleton className="h-10 w-16" />
            <Skeleton className="h-10 w-16" />
          </div>
        </Card>
      </div>
    );
  }

  // --- Resolve display data ---
  const displayUser = profile ?? currentUser;
  const displayName = displayUser?.nickname ?? "未设置昵称";
  const displayZgId = displayUser?.zgId ?? "";
  const displayBio = displayUser?.bio ?? "";
  const displaySchool = displayUser?.school ?? "";
  const displayAvatar = displayUser?.avatar ?? "";
  const displayTags = parseTagsJson(displayUser?.tagsJson);

  return (
    <div className="space-y-6">
      {/* Profile Header */}
      <Card padding="lg">
        <div className="flex flex-col items-center gap-4 sm:flex-row sm:gap-6">
          <Avatar
            src={displayAvatar}
            alt={displayName}
            size="xl"
          />

          <div className="flex-1 text-center sm:text-left">
            <h1 className="text-2xl font-bold text-text-primary">
              {displayName}
            </h1>
            {displayZgId && (
              <p className="mt-1 text-sm text-text-tertiary">@{displayZgId}</p>
            )}
            {displayBio && (
              <p className="mt-2 text-sm text-text-secondary max-w-md">
                {displayBio}
              </p>
            )}
            {displaySchool && (
              <p className="mt-1 text-xs text-text-tertiary">
                {displaySchool}
              </p>
            )}
            {displayTags.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-1.5 justify-center sm:justify-start">
                {displayTags.map((tag) => (
                  <Tag key={tag}>{tag}</Tag>
                ))}
              </div>
            )}
          </div>

          <div className="shrink-0">
            {isOwnProfile ? (
              <Link to="/profile/edit">
                <Button variant="secondary" size="sm">
                  编辑资料
                </Button>
              </Link>
            ) : viewingUserId ? (
              <FollowButton userId={viewingUserId} />
            ) : null}
          </div>
        </div>

        {/* Stats row */}
        <div className="mt-6 flex justify-center gap-8 border-t border-white/50 pt-4">
          <Link
            to={`/following${!isOwnProfile ? `?userId=${viewingUserId}` : ""}`}
            className="text-center transition-colors hover:text-azure-500"
          >
            <span className="block text-xl font-bold text-text-primary">
              {counter?.following ?? 0}
            </span>
            <span className="text-xs text-text-tertiary">关注</span>
          </Link>
          <Link
            to={`/followers${!isOwnProfile ? `?userId=${viewingUserId}` : ""}`}
            className="text-center transition-colors hover:text-azure-500"
          >
            <span className="block text-xl font-bold text-text-primary">
              {counter?.followers ?? 0}
            </span>
            <span className="text-xs text-text-tertiary">粉丝</span>
          </Link>
        </div>
      </Card>

      {/* Posts section (own profile only) */}
      {isOwnProfile && (
        <>
          <Tabs
            tabs={[{ key: "posts", label: "我的帖子" }]}
            activeKey={activeTab}
            onChange={setActiveTab}
          />

          {postsLoading ? (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {Array.from({ length: 4 }).map((_, i) => (
                <PostCardSkeleton key={i} />
              ))}
            </div>
          ) : allPosts.length === 0 ? (
            <EmptyState
              icon={<FileText size={48} />}
              title="暂无帖子"
              description="你还没有发布过帖子"
            />
          ) : (
            <>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {allPosts.map((post) => (
                  <PostCard key={post.id} post={post} />
                ))}
              </div>

              <div
                ref={sentinelRef}
                className="flex items-center justify-center py-8"
              >
                {isFetchingNextPage && <Spinner size="md" />}
                {!hasNextPage && allPosts.length >= PAGE_SIZE && (
                  <p className="text-sm text-text-tertiary">
                    已加载全部帖子
                  </p>
                )}
              </div>
            </>
          )}
        </>
      )}

      {/* Other user's profile: limited view */}
      {!isOwnProfile && (
        <EmptyState
          icon={<FileText size={48} />}
          title="用户主页"
          description="更多功能开发中"
        />
      )}
    </div>
  );
}
