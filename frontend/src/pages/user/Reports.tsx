import { useState } from "react";
import { ArrowRight, CheckCircle2, Upload } from "lucide-react";
import type { Notify } from "../../types";

interface ReportsProps {
  notify: Notify;
}

const reportFollowUps = [
  "What main symptom or concern made you upload this report?",
  "Did a doctor already review this report with you?",
  "Are any values marked high, low, critical, or abnormal?",
  "Do you currently have fever, pain, breathing difficulty, dizziness, or weakness?",
  "Are you taking medicines related to this report?",
  "Do you have a chronic condition connected to these results?",
];

export function Reports({ notify }: ReportsProps) {
  const [fileName, setFileName] = useState("");
  const [step, setStep] = useState(0);
  const [answers, setAnswers] = useState<string[]>(Array(reportFollowUps.length).fill(""));
  const [summary, setSummary] = useState("");

  function handleFile(file?: File) {
    if (!file) return;
    setFileName(file.name);
    setStep(0);
    setAnswers(Array(reportFollowUps.length).fill(""));
    setSummary("");
    notify("Report added successfully. Please answer the follow-up cards.");
  }

  function submitReportFollowUps() {
    const answered = answers.filter((answer) => answer.trim());
    setSummary(`Report follow-up insights were refreshed from ${answered.length} answers. Review abnormal values, symptoms, current medicines, and chronic conditions with a qualified medical professional.`);
    notify("Report follow-up answers submitted. New report insights are ready.");
  }

  return (
    <div className="split-layout" data-section="reports">
      <section className="panel upload-panel">
        <div className="upload-zone">
          <Upload size={38} />
          <h2>Upload text-based PDF report</h2>
          <p>After a report is added, the system asks follow-up cards before refreshing the insight summary.</p>
          <input type="file" accept="application/pdf" onChange={(event) => handleFile(event.target.files?.[0])} />
          {fileName && <div className="success-row"><CheckCircle2 size={18} />{fileName} added</div>}
        </div>
      </section>
      <section className="panel">
        <p className="eyebrow">Report follow-ups</p>
        <h2>Question {step + 1} of {reportFollowUps.length}</h2>
        {fileName ? (
          <div className="followup-card">
            <strong>{reportFollowUps[step]}</strong>
            <textarea
              placeholder="Type your answer"
              value={answers[step]}
              onChange={(event) => setAnswers((current) => current.map((answer, index) => index === step ? event.target.value : answer))}
            />
            <div className="profile-setup-actions">
              <button className="ghost-button" disabled={step === 0} onClick={() => setStep(Math.max(0, step - 1))}>Back</button>
              {step < reportFollowUps.length - 1 ? (
                <button className="primary-button" onClick={() => setStep(step + 1)}>Next<ArrowRight size={18} /></button>
              ) : (
                <button className="primary-button" onClick={submitReportFollowUps}>Submit answers<ArrowRight size={18} /></button>
              )}
            </div>
          </div>
        ) : (
          <p className="summary-box">Upload a report to start the follow-up question flow.</p>
        )}
        {summary && <p className="summary-box">{summary}</p>}
        <div className="disclaimer-box">This application does not provide medical diagnosis, treatment, or prescription.</div>
      </section>
    </div>
  );
}
