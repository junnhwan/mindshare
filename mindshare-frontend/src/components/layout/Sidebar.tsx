import { NavLink } from "react-router-dom";
import { Home, Search, FileText, User, Users } from "lucide-react";
import { cn } from "../../lib/cn";
import { useAuth } from "../../auth/AuthProvider";

const navItems = [
  { to: "/", icon: Home, label: "首页", public: true },
  { to: "/search", icon: Search, label: "搜索", public: true },
  { to: "/my-posts", icon: FileText, label: "我的帖子", public: false },
  { to: "/profile", icon: User, label: "个人", public: false },
  { to: "/following", icon: Users, label: "关注", public: false },
];

export function Sidebar() {
  const { isAuthenticated } = useAuth();

  const visibleItems = navItems.filter((item) => item.public || isAuthenticated);

  return (
    <aside className="hidden w-56 shrink-0 lg:block">
      <nav className="sticky top-[72px] space-y-1">
        {visibleItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm font-medium transition-all duration-200",
                isActive
                  ? "bg-white/70 text-azure-500 shadow-sm"
                  : "text-text-secondary hover:bg-white/40 hover:text-text-primary"
              )
            }
          >
            <item.icon size={20} />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
