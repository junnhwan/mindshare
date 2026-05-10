import { useState } from "react";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getMine, deleteKnowpost, toggleTop } from "../../api/knowpost";
import { PostCard } from "../../components/post/PostCard";
import { PostCardSkeleton } from "../../components/post/PostCardSkeleton";
import { Tabs } from "../../components/ui/Tabs";
import { Button } from "../../components/ui/Button";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { useIntersectionObserver } from "../../hooks/useIntersectionObserver";
import { FileText, Edit, Trash2, Pin } from "lucide-react";
import { PAGE_SIZE } from "../../lib/constants";
import toast from "react-hot-toast";

export function MyPostsPage() {
  const [status, setStatus] = useState("published");
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError,
  } = useInfiniteQuery({
    queryKey: ["myPosts", status],
    queryFn: ({ pageParam = 1 }) =>
      getMine(pageParam as number, PAGE_SIZE, status),
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

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteKnowpost(id),
    onSuccess: () => {
      toast.success("已删除");
      queryClient.invalidateQueries({ queryKey: ["myPosts"] });
      setDeleteId(null);
    },
    onError: () => toast.error("删除失败"),
  });

  const topMutation = useMutation({
    mutationFn: (id: string) => toggleTop(id),
    onSuccess: () => {
      toast.success("操作成功");
      queryClient.invalidateQueries({ queryKey: ["myPosts"] });
    },
    onError: () => toast.error("操作失败"),
  });

  const allPosts = data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-bold text-text-primary">我的帖子</h2>
        <Link to="/post/create">
          <Button size="sm">发布帖子</Button>
        </Link>
      </div>

      <Tabs
        tabs={[
          { key: "published", label: "已发布" },
          { key: "draft", label: "草稿" },
        ]}
        activeKey={status}
        onChange={setStatus}
        className="mb-6"
      />

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <PostCardSkeleton key={i} />
          ))}
        </div>
      )}

      {isError && (
        <EmptyState
          icon={<FileText size={48} />}
          title="加载失败"
          description="无法获取帖子列表"
        />
      )}

      {!isLoading && !isError && allPosts.length === 0 && (
        <EmptyState
          icon={<FileText size={48} />}
          title={status === "published" ? "暂无已发布帖子" : "暂无草稿"}
          description={status === "published" ? "开始分享你的知识吧" : "草稿会在这里保存"}
          action={
            status === "draft"
              ? { label: "发布帖子", onClick: () => window.location.href = "/post/create" }
              : undefined
          }
        />
      )}

      {!isLoading && !isError && allPosts.length > 0 && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {allPosts.map((post) => (
            <div key={post.id} className="relative group/post">
              <PostCard post={post} />

              {/* Admin actions overlay */}
              <div className="absolute right-3 top-3 flex gap-1 opacity-0 transition-opacity group-hover/post:opacity-100">
                <Link
                  to={`/post/${post.id}/edit`}
                  onClick={(e) => e.stopPropagation()}
                >
                  <Button variant="secondary" size="sm" icon={<Edit size={14} />}>
                    编辑
                  </Button>
                </Link>
                <Button
                  variant="secondary"
                  size="sm"
                  icon={<Pin size={14} />}
                  onClick={(e) => {
                    e.stopPropagation();
                    topMutation.mutate(String(post.id));
                  }}
                  loading={topMutation.isPending}
                >
                  置顶
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  icon={<Trash2 size={14} />}
                  onClick={(e) => {
                    e.stopPropagation();
                    setDeleteId(String(post.id));
                  }}
                >
                  删除
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div ref={sentinelRef} className="flex items-center justify-center py-8">
        {isFetchingNextPage && <Spinner size="md" />}
      </div>

      <ConfirmDialog
        open={!!deleteId}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId && deleteMutation.mutate(deleteId)}
        title="确认删除"
        message="删除后将无法恢复，确定要删除这篇帖子吗？"
        confirmLabel="删除"
        loading={deleteMutation.isPending}
      />
    </div>
  );
}
