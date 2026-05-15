import { CartesianGrid, Line, LineChart as ReLineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { RiskPill } from "../../components/RiskPill";
import type { Assessment } from "../../types";
import { formatDate, trendFromAssessments } from "../../utils";

export function History({ assessments }: { assessments: Assessment[] }) {
  return (
    <div className="page-grid" data-section="history">
      <section className="panel wide">
        <div className="section-title"><div><p className="eyebrow">History</p><h2>Assessment timeline</h2></div></div>
        <div className="timeline">
          {assessments.length === 0 && <div className="empty-row">No history found for this account.</div>}
          {assessments.map((item) => (
            <div key={item.id} className="timeline-item"><span /><div><strong>{formatDate(item.createdAt)} | {item.mainSymptom}</strong><p>{item.reasons[0]}</p></div><RiskPill value={item.riskLevel} /></div>
          ))}
        </div>
      </section>
      <section className="panel">
        <p className="eyebrow">Risk trend</p>
        <ResponsiveContainer width="100%" height={240}>
          <ReLineChart data={trendFromAssessments(assessments)}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
            <XAxis dataKey="day" tickLine={false} axisLine={false} />
            <YAxis tickLine={false} axisLine={false} />
            <Tooltip />
            <Line type="monotone" dataKey="risk" stroke="var(--accent-2)" strokeWidth={3} />
          </ReLineChart>
        </ResponsiveContainer>
      </section>
    </div>
  );
}
