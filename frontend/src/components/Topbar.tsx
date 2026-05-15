import { Bell, ChevronDown, Menu, Sparkles } from "lucide-react";
import { designOptions } from "../data";
import type { DesignId, Mode, User } from "../types";

interface TopbarProps {
  mode: Mode;
  setMode: (mode: Mode) => void;
  design: DesignId;
  setDesign: (design: DesignId) => void;
  onMenu: () => void;
  user: User;
  onLogout: () => void;
}

export function Topbar({ mode, setMode, design, setDesign, onMenu, user, onLogout }: TopbarProps) {
  const canUseAdmin = user.role === "ADMIN";
  return (
    <header className="topbar">
      <button className="icon-button mobile-only" onClick={onMenu} title="Menu"><Menu size={20} /></button>
      <div>
        <p className="eyebrow">Logged in as {user.fullName}</p>
        <h1>{mode === "admin" ? "Admin Web Dashboard" : "User PWA Workspace"}</h1>
      </div>
      <div className="top-actions">
        <div className="segmented">
          <button className={mode === "user" ? "active" : ""} onClick={() => setMode("user")}>User</button>
          <button disabled={!canUseAdmin} className={mode === "admin" ? "active" : ""} onClick={() => canUseAdmin && setMode("admin")}>Admin</button>
        </div>
        <label className="select-shell">
          <Sparkles size={16} />
          <select value={design} onChange={(event) => setDesign(event.target.value as DesignId)}>
            {designOptions.map((option) => <option key={option.id} value={option.id}>{option.name}</option>)}
          </select>
          <ChevronDown size={16} />
        </label>
        <button className="icon-button" title="Notifications"><Bell size={19} /></button>
        <button className="ghost-button" onClick={onLogout}>Logout</button>
      </div>
    </header>
  );
}
