import { Suspense, lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import { RequireAuth, GuestOnly } from "../auth/RequireAuth";
import { AppLayout } from "../components/layout/AppLayout";
import { AuthLayout } from "../components/layout/AuthLayout";
import { Spinner } from "../components/ui/Spinner";

// Eagerly loaded (critical path)
import { LoginPage } from "../pages/auth/LoginPage";
import { RegisterPage } from "../pages/auth/RegisterPage";
import { ForgotPasswordPage } from "../pages/auth/ForgotPasswordPage";
import { FeedPage } from "../pages/feed/FeedPage";
import { NotFoundPage } from "../pages/error/NotFoundPage";

// Lazy-loaded (loaded on demand)
const SearchPage = lazy(() => import("../pages/search/SearchPage").then(m => ({ default: m.SearchPage })));
const PostDetailPage = lazy(() => import("../pages/post/PostDetailPage").then(m => ({ default: m.PostDetailPage })));
const PostCreatePage = lazy(() => import("../pages/post/PostCreatePage").then(m => ({ default: m.PostCreatePage })));
const PostEditPage = lazy(() => import("../pages/post/PostEditPage").then(m => ({ default: m.PostEditPage })));
const MyPostsPage = lazy(() => import("../pages/feed/MyPostsPage").then(m => ({ default: m.MyPostsPage })));
const ProfilePage = lazy(() => import("../pages/profile/ProfilePage").then(m => ({ default: m.ProfilePage })));
const ProfileEditPage = lazy(() => import("../pages/profile/ProfileEditPage").then(m => ({ default: m.ProfileEditPage })));
const FollowingPage = lazy(() => import("../pages/relation/FollowingPage").then(m => ({ default: m.FollowingPage })));
const FollowersPage = lazy(() => import("../pages/relation/FollowersPage").then(m => ({ default: m.FollowersPage })));

function Lazy({ children }: { children: React.ReactNode }) {
  return (
    <Suspense
      fallback={
        <div className="flex items-center justify-center py-20">
          <Spinner size="lg" />
        </div>
      }
    >
      {children}
    </Suspense>
  );
}

export const router = createBrowserRouter([
  // Auth routes (guest only)
  {
    path: "/login",
    element: (
      <GuestOnly>
        <AuthLayout>
          <LoginPage />
        </AuthLayout>
      </GuestOnly>
    ),
  },
  {
    path: "/register",
    element: (
      <GuestOnly>
        <AuthLayout>
          <RegisterPage />
        </AuthLayout>
      </GuestOnly>
    ),
  },
  {
    path: "/forgot-password",
    element: (
      <GuestOnly>
        <AuthLayout>
          <ForgotPasswordPage />
        </AuthLayout>
      </GuestOnly>
    ),
  },

  // App routes
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <FeedPage /> },
      {
        path: "search",
        element: <Lazy><SearchPage /></Lazy>,
      },
      {
        path: "post/:id",
        element: <Lazy><PostDetailPage /></Lazy>,
      },
      {
        path: "post/create",
        element: (
          <RequireAuth>
            <Lazy><PostCreatePage /></Lazy>
          </RequireAuth>
        ),
      },
      {
        path: "post/:id/edit",
        element: (
          <RequireAuth>
            <Lazy><PostEditPage /></Lazy>
          </RequireAuth>
        ),
      },
      {
        path: "my-posts",
        element: (
          <RequireAuth>
            <Lazy><MyPostsPage /></Lazy>
          </RequireAuth>
        ),
      },
      {
        path: "profile",
        element: (
          <RequireAuth>
            <Lazy><ProfilePage /></Lazy>
          </RequireAuth>
        ),
      },
      {
        path: "profile/:userId",
        element: <Lazy><ProfilePage /></Lazy>,
      },
      {
        path: "profile/edit",
        element: (
          <RequireAuth>
            <Lazy><ProfileEditPage /></Lazy>
          </RequireAuth>
        ),
      },
      {
        path: "following",
        element: (
          <RequireAuth>
            <Lazy><FollowingPage /></Lazy>
          </RequireAuth>
        ),
      },
      {
        path: "followers",
        element: (
          <RequireAuth>
            <Lazy><FollowersPage /></Lazy>
          </RequireAuth>
        ),
      },
    ],
  },

  // 404
  { path: "*", element: <NotFoundPage /> },
]);
