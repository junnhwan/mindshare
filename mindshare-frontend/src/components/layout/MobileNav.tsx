import { NavLink } from "react-router-dom";
import { Home, Search, PlusCircle, FileText, User } from "lucide-react";
import { cn } from "../../lib/cn";
import { useAuth } from "../../auth/AuthProvider";

const mobileNavItems = [
  { to: "/", icon: Home, label: "首页", public: true },
  { to: "/search", icon: Search, label: "搜索", public: true },
  { to: "/post/create", icon: PlusCircle, label: "发布", public: false },
  { to: "/my-posts", icon: FileText, label: "帖子", public: false },
  { to: "/profile", icon: User, label: "我的", public: false },
];

export function MobileNav() {
  const { isAuthenticated } = useAuth();

  const visibleItems = mobileNavItems.filter(
    (item) => item.public || isAuthenticated
  );

  return (
    <nav className="glass fixed bottom-0 left-0 right-0 z-50 border-t border-white/70 lg:hidden">
      <div className="flex items-center justify-around px-2 pb-safe">
        {visibleItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              cn(
                "flex flex-col items-center gap-0.5 py-2 px-3 text-[10px] font-medium transition-colors",
                isActive
                  ? "text-azure-500"
                  : "text-text-tertiary"
              )
            }
          >
            <item.icon
              size={22}
            />
            {item.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
