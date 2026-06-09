import { useEffect } from "react";
import { CalendarDays, ClipboardList, Download, FileText, Thermometer, X } from "lucide-react";
import { api, authHeaders } from "../api";
import type { Assessment } from "../types";
import { displayRisk, formatDate } from "../utils";
import { CarePrepGuide } from "./CarePrepGuide";
import { ProfileAvatar } from "./ProfileAvatar";

interface AssessmentReportDrawerProps {
  assessment: Assessment | null;
  onClose: () => void;
  token?: string;
}

export function AssessmentReportDrawer({ assessment, onClose, token }: AssessmentReportDrawerProps) {
  useEffect(() => {
    if (!assessment) return;
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [assessment, onClose]);

  if (!assessment) return null;

  async function exportPdf() {
    if (!assessment || !token) return;
    const response = await api.get<Blob>(`/assessments/${assessment.id}/export`, {
      headers: authHeaders(token),
      responseType: "blob",
    });
    const url = URL.createObjectURL(response.data);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `pms-assessment-${assessment.id}.pdf`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="drawer-overlay" role="presentation" onMouseDown={onClose}>
      <aside className="assessment-report-drawer" role="dialog" aria-modal="true" aria-label={`Assessment ASM-${assessment.id} report`} onMouseDown={(event) => event.stopPropagation()}>
        <header className="drawer-header">
          <ProfileAvatar name={assessment.patient} photo={assessment.patientProfilePhotoDataUrl} size="md" />
          <div>
            <p className="eyebrow">Assessment report</p>
            <h2>ASM-{assessment.id} care-preparation record</h2>
            <p>{assessment.patient}</p>
          </div>
          <button className="icon-button" type="button" title="Close report" onClick={onClose}><X size={18} /></button>
        </header>

        <section className="drawer-intake-grid">
          <div><ClipboardList size={17} /><small>Source</small><strong>{assessment.sourceType === "REPORT" ? "Report assessment" : "Symptom assessment"}</strong></div>
          <div><small>{assessment.sourceType === "REPORT" ? "Report" : "Symptoms"}</small><strong>{assessment.reportName || assessment.mainSymptom}</strong></div>
          <div><CalendarDays size={17} /><small>Recorded</small><strong>{formatDate(assessment.createdAt)}</strong></div>
          <div><small>Severity</small><strong>{assessment.severity}/10</strong></div>
          <div><small>Duration</small><strong>{assessment.durationDays} day{assessment.durationDays === 1 ? "" : "s"}</strong></div>
          <div><Thermometer size={17} /><small>Temperature</small><strong>{assessment.temperatureAvailable && assessment.temperatureF ? `${assessment.temperatureF} F` : "Not available"}</strong></div>
          <div><small>Risk</small><strong>{displayRisk(assessment.riskLevel)} | {assessment.riskScore}</strong></div>
          <div className="drawer-span"><small>Chronic-condition context</small><strong>{assessment.chronicCondition || "None provided"}</strong></div>
        </section>

        {assessment.connectedHealthSummary && (
          <section className="care-section">
            <div><ClipboardList size={18} /><strong>Connected health context</strong></div>
            <p>{assessment.connectedHealthSummary}</p>
          </section>
        )}

        {assessment.sourceType === "REPORT" && (
          <section className="care-section">
            <div><FileText size={18} /><strong>Extracted report values</strong></div>
            {assessment.reportProvider && <p>Source noted in report: {assessment.reportProvider}</p>}
            {assessment.extractedObservations && assessment.extractedObservations.length > 0 ? (
              <div className="observation-grid">
                {assessment.extractedObservations.map((observation, index) => (
                  <div className="observation-chip" key={`${observation.testName}-${index}`}>
                    <span>{observation.testName}</span>
                    <strong>{observation.valueText}{observation.unit ? ` ${observation.unit}` : ""}</strong>
                    {observation.referenceRange && <small>Range: {observation.referenceRange}</small>}
                    {observation.flag && <em>{observation.flag}</em>}
                  </div>
                ))}
              </div>
            ) : (
              <p>No structured values were extracted from this report.</p>
            )}
          </section>
        )}

        <CarePrepGuide
          insight={assessment}
          riskLevel={assessment.riskLevel}
          riskScore={assessment.riskScore}
          reasons={assessment.reasons}
          suggestions={assessment.suggestions}
          defaultExpanded
        />

        <section className="care-section">
          <div><ClipboardList size={18} /><strong>Follow-up answers</strong></div>
          <ul>
            {assessment.followUpQuestions.map((question, index) => (
              <li key={`${question}-${index}`}><strong>{question}</strong><span>{assessment.followUpAnswers[index] || "No answer recorded"}</span></li>
            ))}
          </ul>
        </section>

        {token && (
          <button className="ghost-button export-button" type="button" onClick={() => void exportPdf()}>
            <Download size={17} /> Export PDF
          </button>
        )}
      </aside>
    </div>
  );
}
