import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getRelationStatus, follow, unfollow } from "../../api/relation";
import { Button } from "../ui/Button";
import toast from "react-hot-toast";
import type { RelationStatus, RelationStatusResponse } from "../../types/relation";

interface FollowButtonProps {
  userId: number;
  size?: "sm" | "md" | "lg";
}

const LABELS: Record<RelationStatus, string> = {
  self: "编辑资料",
  following: "已关注",
  mutual: "互相关注",
  followedBy: "关注",
  none: "关注",
};

export function FollowButton({ userId, size = "md" }: FollowButtonProps) {
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["relationStatus", userId],
    queryFn: () => getRelationStatus(userId),
    staleTime: 30_000,
  });

  const status: RelationStatus = data?.status ?? "none";

  const followMutation = useMutation({
    mutationFn: () => follow(userId),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ["relationStatus", userId] });
      const prev = queryClient.getQueryData<RelationStatusResponse>([
        "relationStatus",
        userId,
      ]);
      queryClient.setQueryData<RelationStatusResponse>(
        ["relationStatus", userId],
        { status: status === "followedBy" ? "mutual" : "following" },
      );
      return { prev };
    },
    onError: (_err, _vars, context) => {
      if (context?.prev) {
        queryClient.setQueryData(["relationStatus", userId], context.prev);
      }
      toast.error("操作失败，请重试");
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["relationStatus", userId] });
      queryClient.invalidateQueries({ queryKey: ["relationCounter"] });
    },
  });

  const unfollowMutation = useMutation({
    mutationFn: () => unfollow(userId),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ["relationStatus", userId] });
      const prev = queryClient.getQueryData<RelationStatusResponse>([
        "relationStatus",
        userId,
      ]);
      queryClient.setQueryData<RelationStatusResponse>(
        ["relationStatus", userId],
        { status: status === "mutual" ? "followedBy" : "none" },
      );
      return { prev };
    },
    onError: (_err, _vars, context) => {
      if (context?.prev) {
        queryClient.setQueryData(["relationStatus", userId], context.prev);
      }
      toast.error("操作失败，请重试");
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["relationStatus", userId] });
      queryClient.invalidateQueries({ queryKey: ["relationCounter"] });
    },
  });

  const handleClick = () => {
    if (status === "following" || status === "mutual") {
      unfollowMutation.mutate();
    } else if (status === "none" || status === "followedBy") {
      followMutation.mutate();
    }
  };

  // Self: link to edit profile
  if (status === "self") {
    return (
      <Link to="/profile/edit">
        <Button variant="ghost" size={size}>
          {LABELS.self}
        </Button>
      </Link>
    );
  }

  if (isError) {
    return (
      <Button variant="primary" size={size} disabled>
        关注
      </Button>
    );
  }

  const isFollowing = status === "following" || status === "mutual";
  const isPending = followMutation.isPending || unfollowMutation.isPending;

  return (
    <Button
      variant={isFollowing ? "secondary" : "primary"}
      size={size}
      loading={isPending || isLoading}
      onClick={handleClick}
    >
      {LABELS[status]}
    </Button>
  );
}
