import { Bell, ChevronDown, Menu, Sparkles } from "lucide-react";
import { designOptions } from "../data";
import type { DesignId, User } from "../types";

interface TopbarProps {
  design: DesignId;
  setDesign: (design: DesignId) => void;
  onMenu: () => void;
  user: User;
  onLogout: () => void;
}

export function Topbar({ design, setDesign, onMenu, user, onLogout }: TopbarProps) {
  const isStaff = user.role === "ADMIN";
  return (
    <header className="topbar">
      <button className="icon-button mobile-only" onClick={onMenu} title="Menu"><Menu size={20} /></button>
      <div>
        <p className="eyebrow">{isStaff ? "Staff workspace" : "Patient workspace"} | {user.fullName}</p>
        <h1>{isStaff ? "Clinical operations" : "My health overview"}</h1>
      </div>
      <div className="top-actions">
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
