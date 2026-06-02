import { useEffect } from "react";
import { CalendarDays, ClipboardList, Thermometer, X } from "lucide-react";
import type { Assessment } from "../types";
import { displayRisk, formatDate } from "../utils";
import { CarePrepGuide } from "./CarePrepGuide";

interface AssessmentReportDrawerProps {
  assessment: Assessment | null;
  onClose: () => void;
}

export function AssessmentReportDrawer({ assessment, onClose }: AssessmentReportDrawerProps) {
  useEffect(() => {
    if (!assessment) return;
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [assessment, onClose]);

  if (!assessment) return null;

  return (
    <div className="drawer-overlay" role="presentation" onMouseDown={onClose}>
      <aside className="assessment-report-drawer" role="dialog" aria-modal="true" aria-label={`Assessment ASM-${assessment.id} report`} onMouseDown={(event) => event.stopPropagation()}>
        <header className="drawer-header">
          <div>
            <p className="eyebrow">Assessment report</p>
            <h2>ASM-{assessment.id} care-preparation record</h2>
          </div>
          <button className="icon-button" type="button" title="Close report" onClick={onClose}><X size={18} /></button>
        </header>

        <section className="drawer-intake-grid">
          <div><ClipboardList size={17} /><small>Symptoms</small><strong>{assessment.mainSymptom}</strong></div>
          <div><CalendarDays size={17} /><small>Recorded</small><strong>{formatDate(assessment.createdAt)}</strong></div>
          <div><small>Severity</small><strong>{assessment.severity}/10</strong></div>
          <div><small>Duration</small><strong>{assessment.durationDays} day{assessment.durationDays === 1 ? "" : "s"}</strong></div>
          <div><Thermometer size={17} /><small>Temperature</small><strong>{assessment.temperatureAvailable && assessment.temperatureF ? `${assessment.temperatureF} F` : "Not available"}</strong></div>
          <div><small>Risk</small><strong>{displayRisk(assessment.riskLevel)} | {assessment.riskScore}</strong></div>
          <div className="drawer-span"><small>Chronic-condition context</small><strong>{assessment.chronicCondition || "None provided"}</strong></div>
        </section>

        <CarePrepGuide
          insight={assessment}
          riskLevel={assessment.riskLevel}
          riskScore={assessment.riskScore}
          reasons={assessment.reasons}
          suggestions={assessment.suggestions}
        />

        <section className="care-section">
          <div><ClipboardList size={18} /><strong>Follow-up answers</strong></div>
          <ul>
            {assessment.followUpQuestions.map((question, index) => (
              <li key={`${question}-${index}`}><strong>{question}</strong><span>{assessment.followUpAnswers[index] || "No answer recorded"}</span></li>
            ))}
          </ul>
        </section>
      </aside>
    </div>
  );
}
