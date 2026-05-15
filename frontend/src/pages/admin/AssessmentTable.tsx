import { Filter } from "lucide-react";
import { RiskPill } from "../../components/RiskPill";
import type { Assessment } from "../../types";

export function AssessmentTable({ assessments }: { assessments: Assessment[] }) {
  return (
    <section className="panel wide" data-section="admin-assessments">
      <div className="section-title"><div><p className="eyebrow">Monitoring</p><h2>Assessment records</h2></div><button className="ghost-button"><Filter size={17} />Filters</button></div>
      <div className="data-table">
        <div className="data-head"><span>ID</span><span>Patient</span><span>Symptom</span><span>Risk</span><span>Score</span></div>
        {assessments.map((item) => <div className="data-row" key={item.id}><span>ASM-{item.id}</span><strong>{item.patient}</strong><span>{item.mainSymptom}</span><RiskPill value={item.riskLevel} /><span>{item.riskScore}</span></div>)}
      </div>
    </section>
  );
}
