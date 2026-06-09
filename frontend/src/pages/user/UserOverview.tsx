import { ArrowRight, ClipboardList, FileText, Filter, HeartPulse, ShieldCheck, Upload, UserRoundCheck } from "lucide-react";
import { Area, AreaChart, CartesianGrid, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { RiskPill } from "../../components/RiskPill";
import { StatCard } from "../../components/StatCard";
import type { Assessment, Notify, Page, User } from "../../types";
import { displayRisk, trendFromAssessments } from "../../utils";
import { ProfileSetupPrompt } from "./ProfileSetupPrompt";
import { RecentAssessments } from "./RecentAssessments";

interface UserOverviewProps {
  user: User;
  token: string;
  assessments: Assessment[];
  setPage: (page: Page) => void;
  updateUser: (user: User) => void;
  notify: Notify;
}

export function UserOverview({ user, token, assessments, setPage, updateUser, notify }: UserOverviewProps) {
  const latest = assessments[0];
  const trendData = trendFromAssessments(assessments);
  const profileCompletion = user.profileCompletion ?? 0;
  const reportCount = assessments.filter((item) => item.sourceType === "REPORT").length;
  return (
    <div className="page-grid" data-section="user-overview">
        {!user.profileSetupComplete && <ProfileSetupPrompt user={user} token={token} updateUser={updateUser} notify={notify} />}
        <div className="hero-panel">
          <div>
            <p className="eyebrow">Today</p>
            <h2>{latest ? `${user.fullName}, your latest assessment shows ${displayRisk(latest.riskLevel).toLowerCase()} awareness.` : `Welcome, ${user.fullName}. Start your first assessment.`}</h2>
            <p>{latest ? latest.reasons[0] : "Start an assessment to prepare a clear summary for your next medical conversation."}</p>
            <div className="hero-actions">
              <button className="primary-button" onClick={() => setPage("assessment")}>Start assessment<ArrowRight size={18} /></button>
              <button className="ghost-button" onClick={() => setPage("reports")}><Upload size={18} />Upload report</button>
            </div>
          </div>
          <div className="vital-card">
            <HeartPulse size={28} />
            <span>Latest risk score</span>
            <strong>{latest?.riskScore ?? 0}</strong>
            {latest ? <RiskPill value={latest.riskLevel} /> : <span>No records</span>}
          </div>
        </div>
        <div className="stats-grid">
          <StatCard icon={ClipboardList} label="Assessments" value={assessments.length} delta="saved records" />
          <StatCard icon={UserRoundCheck} label="Profile completion" value={`${profileCompletion}%`} delta="care profile" />
          <StatCard icon={FileText} label="Report assessments" value={reportCount} delta="saved uploads" />
          <StatCard icon={ShieldCheck} label="Safety guidance" value="Active" delta="always on" />
        </div>
        <section className="panel wide">
          <div className="section-title"><div><p className="eyebrow">Tracking</p><h2>Risk and temperature trend</h2></div><button className="icon-button" title="Filter"><Filter size={18} /></button></div>
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={trendData}>
              <defs><linearGradient id="riskFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="var(--accent)" stopOpacity={0.28} /><stop offset="95%" stopColor="var(--accent)" stopOpacity={0} /></linearGradient></defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
              <XAxis dataKey="day" tickLine={false} axisLine={false} />
              <YAxis tickLine={false} axisLine={false} />
              <Tooltip />
              <Area type="monotone" dataKey="risk" stroke="var(--accent)" fill="url(#riskFill)" strokeWidth={3} />
              <Line type="monotone" dataKey="temp" stroke="var(--success)" strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        </section>
        <RecentAssessments assessments={assessments} />
    </div>
  );
}
