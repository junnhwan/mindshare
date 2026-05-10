import { Card } from "../ui/Card";
import { Skeleton } from "../ui/Skeleton";

export function PostCardSkeleton() {
  return (
    <Card className="overflow-hidden">
      {/* Cover image skeleton */}
      <Skeleton className="aspect-[16/9] w-full !rounded-none" variant="rectangular" />

      <div className="space-y-3 p-5">
        {/* Title */}
        <Skeleton className="h-6 w-3/4" />
        <Skeleton className="h-6 w-1/2" />

        {/* Description */}
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />

        {/* Tags */}
        <div className="flex gap-1.5 pt-1">
          <Skeleton className="h-5 w-12 !rounded-full" />
          <Skeleton className="h-5 w-16 !rounded-full" />
          <Skeleton className="h-5 w-10 !rounded-full" />
        </div>

        {/* Footer */}
        <div className="flex items-center gap-2 pt-2">
          <Skeleton variant="circular" className="h-6 w-6" />
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-4 w-12" />
        </div>
      </div>
    </Card>
  );
}
