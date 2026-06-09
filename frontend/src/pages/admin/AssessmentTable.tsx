import { useState } from "react";
import { Filter } from "lucide-react";
import { AssessmentReportDrawer } from "../../components/AssessmentReportDrawer";
import { ProfileAvatar } from "../../components/ProfileAvatar";
import { RiskPill } from "../../components/RiskPill";
import type { Assessment } from "../../types";

export function AssessmentTable({ assessments, token }: { assessments: Assessment[]; token: string }) {
  const [selected, setSelected] = useState<Assessment | null>(null);
  return (
    <>
    <section className="panel wide" data-section="admin-assessments">
      <div className="section-title"><div><p className="eyebrow">Care review</p><h2>Assessment records</h2></div><button className="ghost-button"><Filter size={17} />Filters</button></div>
      <div className="data-table">
        <div className="data-head"><span>ID</span><span>Patient</span><span>Symptom</span><span>Risk</span><span>Score</span></div>
        {assessments.map((item) => (
          <button type="button" className="data-row assessment-open-button" key={item.id} onClick={() => setSelected(item)}>
            <span>ASM-{item.id}</span>
            <strong className="patient-cell"><ProfileAvatar name={item.patient} photo={item.patientProfilePhotoDataUrl} size="sm" />{item.patient}</strong>
            <span>{item.mainSymptom}</span>
            <RiskPill value={item.riskLevel} />
            <span>{item.riskScore}</span>
          </button>
        ))}
      </div>
    </section>
    <AssessmentReportDrawer assessment={selected} onClose={() => setSelected(null)} token={token} />
    </>
  );
}
