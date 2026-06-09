import { useState } from "react";
import { AssessmentReportDrawer } from "../../components/AssessmentReportDrawer";
import { ProfileAvatar } from "../../components/ProfileAvatar";
import { RiskPill } from "../../components/RiskPill";
import type { Assessment } from "../../types";
import { formatDate } from "../../utils";

export function RecentAssessments({ assessments }: { assessments: Assessment[] }) {
  const [selected, setSelected] = useState<Assessment | null>(null);
  return (
    <>
    <section className="panel" data-section="recent-assessments">
      <div className="section-title"><div><p className="eyebrow">Records</p><h2>Recent assessments</h2></div></div>
      <div className="table-list">
        {assessments.length === 0 && <div className="empty-row">No assessment records yet.</div>}
        {assessments.map((item) => (
          <button type="button" className="table-row assessment-open-button" key={item.id} onClick={() => setSelected(item)}>
            <ProfileAvatar name={item.patient} photo={item.patientProfilePhotoDataUrl} size="sm" />
            <div><strong>{item.mainSymptom}</strong><span>ASM-{item.id} | {formatDate(item.createdAt)}</span></div>
            <RiskPill value={item.riskLevel} />
          </button>
        ))}
      </div>
    </section>
    <AssessmentReportDrawer assessment={selected} onClose={() => setSelected(null)} />
    </>
  );
}
