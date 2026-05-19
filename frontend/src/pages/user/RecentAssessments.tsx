import { RiskPill } from "../../components/RiskPill";
import type { Assessment } from "../../types";
import { formatDate } from "../../utils";

export function RecentAssessments({ assessments }: { assessments: Assessment[] }) {
  return (
    <section className="panel" data-section="recent-assessments">
      <div className="section-title"><div><p className="eyebrow">Records</p><h2>Recent assessments</h2></div></div>
      <div className="table-list">
        {assessments.length === 0 && <div className="empty-row">No assessment records yet.</div>}
        {assessments.map((item) => (
          <div className="table-row" key={item.id}>
            <div><strong>{item.mainSymptom}</strong><span>ASM-{item.id} | {formatDate(item.createdAt)}</span></div>
            <RiskPill value={item.riskLevel} />
          </div>
        ))}
      </div>
    </section>
  );
}
