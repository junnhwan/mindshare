import { Outlet } from "react-router-dom";
import { Header } from "./Header";
import { Sidebar } from "./Sidebar";
import { MobileNav } from "./MobileNav";

export function AppLayout() {
  return (
    <div className="min-h-screen bg-cream-50 pb-16 lg:pb-0">
      {/* Background decorations */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -left-40 -top-40 h-96 w-96 rounded-full bg-azure-500/5 blur-3xl" />
        <div className="absolute -bottom-40 -right-40 h-96 w-96 rounded-full bg-amber-400/5 blur-3xl" />
      </div>

      <Header />

      <div className="mx-auto flex max-w-6xl gap-8 px-4 pt-6">
        <Sidebar />
        <main className="min-h-[calc(100vh-120px)] flex-1">
          <Outlet />
        </main>
      </div>

      <MobileNav />
    </div>
  );
}
