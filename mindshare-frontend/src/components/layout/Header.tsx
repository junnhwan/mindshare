import { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LogOut, User, Settings, FileText } from "lucide-react";
import { SearchBar } from "../search/SearchBar";
import { Avatar } from "../ui/Avatar";
import { useAuth, useAuthStore } from "../../auth/AuthProvider";
import { clearTokens, clearStorageTokens } from "../../api/client";
import { logout as logoutApi } from "../../api/auth";
import toast from "react-hot-toast";

export function Header() {
  const { isAuthenticated } = useAuth();
  const user = useAuthStore((s) => s.user);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu on click outside
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  const handleLogout = async () => {
    try {
      await logoutApi();
    } catch {
      // Ignore logout errors
    }
    clearAuth();
    clearTokens();
    clearStorageTokens();
    toast.success("已退出登录");
    navigate("/login");
  };

  return (
    <header className="glass sticky top-0 z-50 border-b border-white/70">
      <div className="mx-auto flex h-14 max-w-6xl items-center gap-4 px-4">
        {/* Logo */}
        <Link to="/" className="shrink-0 text-xl font-bold text-text-primary">
          MindShare
        </Link>

        {/* Search */}
        <SearchBar className="hidden flex-1 sm:block max-w-md" />

        {/* Right side */}
        <div className="ml-auto flex items-center gap-3">
          {isAuthenticated && user ? (
            <div ref={menuRef} className="relative">
              <button
                onClick={() => setMenuOpen(!menuOpen)}
                className="flex items-center gap-2 rounded-lg p-1 transition-colors hover:bg-white/50"
              >
                <Avatar src={user.avatar} alt={user.nickname} size="sm" />
              </button>

              {menuOpen && (
                <div className="absolute right-0 top-full mt-2 w-48 rounded-xl border border-white/70 bg-white/90 p-2 shadow-glass-elevated backdrop-blur-xl">
                  <div className="border-b border-white/50 px-3 py-2">
                    <p className="text-sm font-medium text-text-primary">
                      {user.nickname}
                    </p>
                    <p className="text-xs text-text-secondary">
                      @{user.zgId || user.id}
                    </p>
                  </div>

                  <button
                    onClick={() => {
                      setMenuOpen(false);
                      navigate("/profile");
                    }}
                    className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-text-primary hover:bg-cream-100 transition-colors"
                  >
                    <User size={16} />
                    个人主页
                  </button>
                  <button
                    onClick={() => {
                      setMenuOpen(false);
                      navigate("/my-posts");
                    }}
                    className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-text-primary hover:bg-cream-100 transition-colors"
                  >
                    <FileText size={16} />
                    我的帖子
                  </button>
                  <button
                    onClick={() => {
                      setMenuOpen(false);
                      navigate("/profile/edit");
                    }}
                    className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-text-primary hover:bg-cream-100 transition-colors"
                  >
                    <Settings size={16} />
                    编辑资料
                  </button>

                  <div className="border-t border-white/50 mt-1 pt-1">
                    <button
                      onClick={handleLogout}
                      className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-error hover:bg-red-50 transition-colors"
                    >
                      <LogOut size={16} />
                      退出登录
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <Link
              to="/login"
              className="rounded-lg bg-azure-500 px-4 py-1.5 text-sm font-medium text-white transition-colors hover:bg-azure-600"
            >
              登录
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
