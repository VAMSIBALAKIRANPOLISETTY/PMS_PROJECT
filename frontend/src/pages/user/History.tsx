import { useState } from "react";
import { Download } from "lucide-react";
import { CartesianGrid, Line, LineChart as ReLineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, authHeaders } from "../../api";
import { AssessmentReportDrawer } from "../../components/AssessmentReportDrawer";
import { ProfileAvatar } from "../../components/ProfileAvatar";
import { RiskPill } from "../../components/RiskPill";
import type { Assessment } from "../../types";
import { formatDate, trendFromAssessments } from "../../utils";

export function History({ assessments, token }: { assessments: Assessment[]; token: string }) {
  const [selected, setSelected] = useState<Assessment | null>(null);

  async function download(url: string, fileName: string) {
    const response = await api.get<Blob>(url, {
      headers: authHeaders(token),
      responseType: "blob",
    });
    const objectUrl = URL.createObjectURL(response.data);
    const anchor = document.createElement("a");
    anchor.href = objectUrl;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(objectUrl);
  }

  return (
    <>
    <div className="page-grid" data-section="history">
      <section className="panel wide">
        <div className="section-title">
          <div>
            <p className="eyebrow">History</p>
            <h2>Assessment history</h2>
          </div>
          <div className="section-actions">
            <button className="ghost-button compact-button" type="button" onClick={() => void download("/assessments/history/export", "pms-assessment-history.pdf")}>
              <Download size={16} /> Export full history
            </button>
          </div>
        </div>
        <div className="timeline">
          {assessments.length === 0 && <div className="empty-row">No history found for this account.</div>}
          {assessments.map((item) => (
            <div key={item.id} className="timeline-record">
              <button type="button" className="timeline-item assessment-open-button timeline-record-main" onClick={() => setSelected(item)}>
                <ProfileAvatar name={item.patient} photo={item.patientProfilePhotoDataUrl} size="sm" />
                <div>
                  <strong>{formatDate(item.createdAt)} | {item.sourceType === "REPORT" ? item.reportName || "Report assessment" : item.mainSymptom}</strong>
                  <p>{item.sourceType === "REPORT" ? "Saved report-based care guide" : item.reasons[0]}</p>
                </div>
                <div className="timeline-meta">
                  <span>{item.sourceType === "REPORT" ? "Report assessment" : "Guided assessment"}</span>
                  <RiskPill value={item.riskLevel} />
                </div>
              </button>
              <div className="timeline-record-actions">
                <button
                  className="ghost-button compact-button"
                  type="button"
                  onClick={() => void download(`/assessments/${item.id}/export`, `pms-assessment-${item.id}.pdf`)}
                >
                  <Download size={16} /> Export PDF
                </button>
              </div>
            </div>
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
    <AssessmentReportDrawer assessment={selected} onClose={() => setSelected(null)} token={token} />
    </>
  );
}
