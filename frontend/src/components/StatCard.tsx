import type { LucideIcon } from "lucide-react";

interface StatCardProps {
  icon: LucideIcon;
  label: string;
  value: string | number;
  delta: string;
  tone?: string;
}

export function StatCard({ icon: Icon, label, value, delta, tone = "neutral" }: StatCardProps) {
  return (
    <div className={`stat-card ${tone}`}>
      <div className="stat-head">
        <span className="icon-chip"><Icon size={19} /></span>
        <span>{delta}</span>
      </div>
      <strong>{value}</strong>
      <p>{label}</p>
    </div>
  );
}
