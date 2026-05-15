import { AlertTriangle, ArrowRight, ClipboardList, FileText, PieChart, Search, Users } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart as RePieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { StatCard } from "../../components/StatCard";
import type { Analytics, Assessment, Page } from "../../types";
import { AssessmentTable } from "./AssessmentTable";

interface AdminOverviewProps {
  analytics: Analytics;
  assessments: Assessment[];
  setPage: (page: Page) => void;
}

export function AdminOverview({ analytics, assessments, setPage }: AdminOverviewProps) {
  const riskData = [
    { name: "Low", value: analytics.lowRiskCount, color: "#2f9e75" },
    { name: "Medium", value: analytics.mediumRiskCount, color: "#d8902f" },
    { name: "High", value: analytics.highRiskCount, color: "#d94c59" },
  ];
  return (
    <div className="page-grid" data-section="admin-overview">
      <div className="stats-grid">
        <StatCard icon={Users} label="Total users" value={analytics.totalUsers} delta="registered" />
        <StatCard icon={ClipboardList} label="Assessments" value={analytics.totalAssessments} delta="database" />
        <StatCard icon={AlertTriangle} label="High risk" value={analytics.highRiskCount} delta="needs review" tone="danger" />
        <StatCard icon={FileText} label="PDF uploads" value="0" delta="next module" />
      </div>
      <section className="panel wide">
        <div className="section-title"><div><p className="eyebrow">Analytics</p><h2>Common symptoms</h2></div><div className="search-box"><Search size={17} /><input placeholder="Filter symptoms" /></div></div>
        <ResponsiveContainer width="100%" height={270}>
          <BarChart data={analytics.commonSymptoms.map((item) => ({ name: item.symptom, count: item.count }))}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
            <XAxis dataKey="name" tickLine={false} axisLine={false} />
            <YAxis tickLine={false} axisLine={false} />
            <Tooltip />
            <Bar dataKey="count" radius={[7, 7, 0, 0]} fill="var(--accent)" />
          </BarChart>
        </ResponsiveContainer>
      </section>
      <section className="panel">
        <div className="section-title"><div><p className="eyebrow">Risk mix</p><h2>Assessment split</h2></div><PieChart size={24} /></div>
        <ResponsiveContainer width="100%" height={240}>
          <RePieChart>
            <Pie data={riskData} innerRadius={62} outerRadius={92} paddingAngle={4} dataKey="value">{riskData.map((entry) => <Cell key={entry.name} fill={entry.color} />)}</Pie>
            <Tooltip />
          </RePieChart>
        </ResponsiveContainer>
        <button className="primary-button full" onClick={() => setPage("assessments")}>Review records<ArrowRight size={18} /></button>
      </section>
      <AssessmentTable assessments={assessments} />
    </div>
  );
}
