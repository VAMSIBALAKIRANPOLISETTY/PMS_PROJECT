import { useState } from "react";
import axios from "axios";
import { ArrowRight, CheckCircle2, Upload } from "lucide-react";
import { api, authHeaders } from "../../api";
import { CarePrepGuide } from "../../components/CarePrepGuide";
import type { Notify, ReportFollowUpResponse, ReportInsight } from "../../types";

interface ReportsProps {
  token: string;
  notify: Notify;
}

export function Reports({ token, notify }: ReportsProps) {
  const [fileName, setFileName] = useState("");
  const [reportText, setReportText] = useState("");
  const [step, setStep] = useState(0);
  const [questions, setQuestions] = useState<string[]>([]);
  const [answers, setAnswers] = useState<string[]>([]);
  const [insight, setInsight] = useState<ReportInsight | null>(null);
  const [message, setMessage] = useState("");

  async function handleFile(file?: File) {
    if (!file) return;
    setFileName(file.name);
    setStep(0);
    setReportText("");
    setQuestions([]);
    setAnswers([]);
    setInsight(null);
    setMessage("");
    try {
      const response = await api.post<ReportFollowUpResponse>("/reports/follow-ups", { reportName: file.name }, { headers: authHeaders(token) });
      setQuestions(response.data.followUpQuestions);
      setAnswers(Array(response.data.followUpQuestions.length).fill(""));
      notify("Report added successfully. Backend follow-up cards are ready.");
    } catch (error) {
      const fallback = axios.isAxiosError(error) ? error.response?.data?.message ?? "Could not prepare report follow-ups." : "Could not prepare report follow-ups.";
      setMessage(fallback);
      notify("Report follow-up setup failed.", "danger");
    }
  }

  async function submitReportFollowUps() {
    if (!fileName) return;
    setMessage("");
    const cleanedAnswers = answers.map((answer) => answer.trim()).filter(Boolean);
    if (cleanedAnswers.length === 0) {
      setMessage("Answer at least one report follow-up before generating the care-prep guide.");
      return;
    }
    try {
      const response = await api.post<ReportInsight>("/reports/insight", {
        reportName: fileName,
        reportText,
        answers: cleanedAnswers,
      }, { headers: authHeaders(token) });
      setInsight(response.data);
      notify("Report follow-up answers submitted. Care-prep guide is ready.");
    } catch (error) {
      const fallback = axios.isAxiosError(error) ? error.response?.data?.message ?? "Report insight failed." : "Report insight failed.";
      setMessage(fallback);
      notify("Report insight failed.", "danger");
    }
  }

  const currentQuestion = questions[step];

  return (
    <div className="split-layout" data-section="reports">
      <section className="panel upload-panel">
        <div className="upload-zone">
          <Upload size={38} />
          <h2>Upload text-based PDF report</h2>
          <p>After a report is added, the system asks follow-up cards before refreshing the insight summary.</p>
          <input type="file" accept="application/pdf" onChange={(event) => void handleFile(event.target.files?.[0])} />
          {fileName && <div className="success-row"><CheckCircle2 size={18} />{fileName} added</div>}
          {fileName && (
            <label className="report-text-box">
              Report notes or copied text
              <textarea
                placeholder="Paste key report text, abnormal values, or notes from the report."
                value={reportText}
                onChange={(event) => setReportText(event.target.value)}
              />
            </label>
          )}
        </div>
      </section>
      <section className="panel">
        <p className="eyebrow">Report follow-ups</p>
        <h2>{questions.length > 0 ? `Question ${step + 1} of ${questions.length}` : "Backend-generated report questions"}</h2>
        {message && <div className="form-message">{message}</div>}
        {fileName && currentQuestion ? (
          <div className="followup-card">
            <strong>{currentQuestion}</strong>
            <textarea
              placeholder="Type your answer"
              value={answers[step]}
              onChange={(event) => setAnswers((current) => current.map((answer, index) => index === step ? event.target.value : answer))}
            />
            <div className="profile-setup-actions">
              <button className="ghost-button" disabled={step === 0} onClick={() => setStep(Math.max(0, step - 1))}>Back</button>
              {step < questions.length - 1 ? (
                <button className="primary-button" onClick={() => setStep(step + 1)}>Next<ArrowRight size={18} /></button>
              ) : (
                <button className="primary-button" onClick={submitReportFollowUps}>Submit answers<ArrowRight size={18} /></button>
              )}
            </div>
          </div>
        ) : (
          <p className="summary-box">Upload a report to start the follow-up question flow.</p>
        )}
        {insight && <CarePrepGuide title="Report care-preparation guide" insight={insight} />}
        <div className="disclaimer-box">This application does not provide medical diagnosis, treatment, or prescription.</div>
      </section>
    </div>
  );
}
