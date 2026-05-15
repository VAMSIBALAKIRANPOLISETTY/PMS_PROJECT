import { HeartPulse, ShieldCheck, X } from "lucide-react";
import { adminNav, userNav } from "../data";
import type { Mode, Page } from "../types";

interface SidebarProps {
  mode: Mode;
  page: Page;
  setPage: (page: Page) => void;
  open: boolean;
  setOpen: (open: boolean) => void;
}

export function Sidebar({ mode, page, setPage, open, setOpen }: SidebarProps) {
  const nav = mode === "admin" ? adminNav : userNav;
  return (
    <aside className={`sidebar ${open ? "open" : ""}`}>
      <div className="brand-row">
        <div className="brand-mark"><HeartPulse size={24} /></div>
        <div><strong>PMS Health</strong><span>Assessment system</span></div>
        <button className="icon-button mobile-only close-menu" onClick={() => setOpen(false)}><X size={18} /></button>
      </div>
      <nav>
        {nav.map(([id, label, Icon]) => (
          <button key={id} className={page === id ? "active" : ""} onClick={() => { setPage(id); setOpen(false); }}>
            <Icon size={18} />{label}
          </button>
        ))}
      </nav>
      <div className="disclaimer-mini">
        <ShieldCheck size={18} />
        <p>Insights only. No diagnosis, prescription, or emergency response.</p>
      </div>
    </aside>
  );
}
