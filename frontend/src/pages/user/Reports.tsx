import { useRef, useState, type KeyboardEvent } from "react";
import axios from "axios";
import { Activity, ArrowRight, CheckCircle2, Download, FileText, Upload } from "lucide-react";
import { api, authHeaders } from "../../api";
import { CarePrepGuide } from "../../components/CarePrepGuide";
import type { Assessment, Notify, TimelineRecord } from "../../types";

interface ReportsProps {
  token: string;
  notify: Notify;
  onCreated?: () => Promise<void>;
}

const CHOICES = ["Yes", "No", "Not sure"] as const;

type Choice = (typeof CHOICES)[number] | "";

interface ReportAnswer {
  choice: Choice;
  note: string;
}

export function Reports({ token, notify, onCreated }: ReportsProps) {
  const primaryActionRef = useRef<HTMLButtonElement | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [reportText, setReportText] = useState("");
  const [step, setStep] = useState(0);
  const [draft, setDraft] = useState<Assessment | null>(null);
  const [answers, setAnswers] = useState<ReportAnswer[]>([]);
  const [result, setResult] = useState<Assessment | null>(null);
  const [connectedRecords, setConnectedRecords] = useState<TimelineRecord[]>([]);
  const [includeConnected, setIncludeConnected] = useState(false);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  function resetDraft(nextFile: File | null) {
    setFile(nextFile);
    setStep(0);
    setDraft(null);
    setAnswers([]);
    setResult(null);
    setIncludeConnected(false);
    setMessage("");
  }

  async function useConnectedHealthData() {
    try {
      const response = await api.get<TimelineRecord[]>("/health-timeline", { headers: authHeaders(token) });
      const records = recentTimelineRecords(response.data);
      setConnectedRecords(records);
      setIncludeConnected(records.length > 0);
      setMessage(records.length > 0
        ? `Connected health data selected: ${records.length} recent record${records.length === 1 ? "" : "s"}.`
        : "No connected health records are available yet.");
    } catch (error) {
      setMessage(connectedHealthMessage(error));
      notify("Connected health lookup failed.", "danger");
    }
  }

  async function uploadReport() {
    if (!file && !reportText.trim()) {
      setMessage("Upload a readable report or paste the report text first.");
      return;
    }
    if (file?.type.startsWith("image/") && !reportText.trim()) {
      setMessage("Image upload is supported, but paste readable report text until OCR extraction is added.");
      return;
    }
    const body = new FormData();
    if (file) body.append("file", file);
    if (reportText.trim()) body.append("reportText", reportText.trim());
    body.append("includeConnectedHealth", String(includeConnected));
    connectedRecords.forEach((record) => body.append("connectedHealthRecordIds", String(record.id)));
    setBusy(true);
    setMessage("");
    try {
      const response = await api.post<Assessment>("/reports/upload", body, {
        headers: { ...authHeaders(token), "Content-Type": "multipart/form-data" },
      });
      setDraft(response.data);
      setAnswers(response.data.followUpQuestions.map(() => ({ choice: "", note: "" })));
      notify("Report saved. Answer the follow-up cards to prepare the care guide.");
    } catch (error) {
      const fallback = axios.isAxiosError(error) ? error.response?.data?.message ?? "Could not save the report." : "Could not save the report.";
      setMessage(fallback);
      notify("Report upload failed.", "danger");
    } finally {
      setBusy(false);
    }
  }

  async function submitReportFollowUps() {
    if (!draft) return;
    const normalizedAnswers = answers.map(formatAnswer);
    if (normalizedAnswers.some((answer) => !answer)) {
      setMessage("Choose Yes, No, or Not sure for every report question.");
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      const response = await api.post<Assessment>(
        `/reports/${draft.id}/follow-ups`,
        { answers: normalizedAnswers },
        { headers: authHeaders(token) },
      );
      setResult(response.data);
      await onCreated?.();
      notify("Report assessment saved to history.");
    } catch (error) {
      const fallback = axios.isAxiosError(error) ? error.response?.data?.message ?? "Report assessment failed." : "Report assessment failed.";
      setMessage(fallback);
      notify("Report assessment failed.", "danger");
    } finally {
      setBusy(false);
    }
  }

  async function exportPdf() {
    if (!result) return;
    const response = await api.get<Blob>(`/assessments/${result.id}/export`, {
      headers: authHeaders(token),
      responseType: "blob",
    });
    const url = URL.createObjectURL(response.data);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `pms-report-assessment-${result.id}.pdf`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  function chooseAnswer(choice: Choice) {
    setAnswers((current) => current.map((answer, index) => index === step ? { ...answer, choice } : answer));
    window.setTimeout(() => primaryActionRef.current?.focus(), 0);
  }

  function goNext() {
    if (!answers[step]?.choice) {
      setMessage("Choose an answer before moving to the next question.");
      return;
    }
    setMessage("");
    setStep((current) => Math.min(current + 1, answers.length - 1));
  }

  function handleQuestionKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key !== "Enter") return;
    const target = event.target as HTMLElement;
    if (target.tagName === "TEXTAREA") return;
    event.preventDefault();
    if (step < answers.length - 1) {
      goNext();
    } else {
      void submitReportFollowUps();
    }
  }

  const currentQuestion = draft?.followUpQuestions[step];
  const currentAnswer = answers[step] ?? { choice: "", note: "" };
  const sourceAssessment = result ?? draft;

  return (
    <div className="split-layout" data-section="reports">
      <section className="panel upload-panel">
        <div className="upload-zone">
          <Upload size={34} />
          <h2>Upload a report</h2>
          <p>Use a text-based PDF or paste report text. PMS saves the report assessment so it appears in history and staff review.</p>
          <input type="file" accept="application/pdf,text/plain,image/png,image/jpeg,image/webp" onChange={(event) => resetDraft(event.target.files?.[0] ?? null)} />
          {file && <div className="success-row"><CheckCircle2 size={18} />{file.name} selected</div>}
          <label className="report-text-box">
            Report text or notes
            <textarea
              placeholder="Paste lab values, abnormal flags, or provider notes if the PDF cannot be read."
              value={reportText}
              onChange={(event) => setReportText(event.target.value)}
            />
          </label>
          <button className="primary-button" type="button" disabled={busy} onClick={uploadReport}>
            Prepare report questions <ArrowRight size={18} />
          </button>
          <button className="ghost-button" type="button" onClick={() => void useConnectedHealthData()}>
            <Activity size={17} /> Use connected health data
          </button>
          {includeConnected && <div className="success-row"><CheckCircle2 size={18} />Connected data will be included in this report assessment.</div>}
        </div>
      </section>

      <section className="panel">
        <p className="eyebrow">Report assessment</p>
        <h2>{currentQuestion ? `Question ${step + 1} of ${draft?.followUpQuestions.length}` : "Prepare a report-based care guide"}</h2>
        {message && <div className="form-message">{message}</div>}

        {sourceAssessment?.extractedObservations && sourceAssessment.extractedObservations.length > 0 && (
          <div className="observation-grid" aria-label="Extracted report values">
            {sourceAssessment.extractedObservations.slice(0, 6).map((observation, index) => (
              <div className="observation-chip" key={`${observation.testName}-${index}`}>
                <span>{observation.testName}</span>
                <strong>{observation.valueText}{observation.unit ? ` ${observation.unit}` : ""}</strong>
                {observation.flag && <em>{observation.flag}</em>}
              </div>
            ))}
          </div>
        )}

        {sourceAssessment?.connectedHealthSummary && (
          <div className="summary-box compact-summary">{sourceAssessment.connectedHealthSummary}</div>
        )}

        {draft && currentQuestion && !result ? (
          <div className="followup-card" onKeyDown={handleQuestionKeyDown}>
            <strong>{currentQuestion}</strong>
            <div className="choice-grid" role="radiogroup" aria-label={currentQuestion}>
              {CHOICES.map((choice) => (
                <button
                  key={choice}
                  type="button"
                  role="radio"
                  aria-checked={currentAnswer.choice === choice}
                  className={`choice-card ${currentAnswer.choice === choice ? "selected" : ""}`}
                  onClick={() => chooseAnswer(choice)}
                >
                  {choice}
                </button>
              ))}
            </div>
            <textarea
              placeholder="Optional note for your doctor"
              value={currentAnswer.note}
              onChange={(event) => setAnswers((current) => current.map((answer, index) => index === step ? { ...answer, note: event.target.value } : answer))}
            />
            <div className="profile-setup-actions">
              <button className="ghost-button" type="button" disabled={step === 0 || busy} onClick={() => setStep(Math.max(0, step - 1))}>Back</button>
              {step < answers.length - 1 ? (
                <button ref={primaryActionRef} className="primary-button" type="button" disabled={!currentAnswer.choice || busy} onClick={goNext}>Next<ArrowRight size={18} /></button>
              ) : (
                <button ref={primaryActionRef} className="primary-button" type="button" disabled={!currentAnswer.choice || busy} onClick={submitReportFollowUps}>Save report assessment<ArrowRight size={18} /></button>
              )}
            </div>
          </div>
        ) : (
          <p className="summary-box">Upload a report or paste text to prepare a saved report assessment.</p>
        )}

        {result && (
          <>
            <CarePrepGuide
              title="Report care-preparation guide"
              insight={result}
              riskLevel={result.riskLevel}
              riskScore={result.riskScore}
              reasons={result.reasons}
              suggestions={result.suggestions}
            />
            <button className="ghost-button export-button" type="button" onClick={() => void exportPdf()}>
              <Download size={17} /> Export PDF
            </button>
          </>
        )}

        <div className="disclaimer-box"><FileText size={16} /> PMS explains report context for care preparation. It does not diagnose, prescribe, or replace medical care.</div>
      </section>
    </div>
  );
}

function formatAnswer(answer: ReportAnswer) {
  if (!answer.choice) return "";
  const note = answer.note.trim();
  return note ? `${answer.choice} | Note: ${note}` : answer.choice;
}

function recentTimelineRecords(records: TimelineRecord[]) {
  const cutoff = Date.now() - 7 * 24 * 60 * 60 * 1000;
  return records.filter((record) => !record.observedAt || new Date(record.observedAt).getTime() >= cutoff).slice(0, 20);
}

function connectedHealthMessage(error: unknown) {
  if (!axios.isAxiosError(error)) return "Connected health records could not be loaded.";
  if (typeof error.response?.data?.message === "string") return error.response.data.message;
  if (error.response?.status === 401 || error.response?.status === 403) return "Log in with a patient account before using connected health records.";
  if (error.response?.status) return `Connected health records could not be loaded. Server returned ${error.response.status}.`;
  if (error.request) return "Connected health records could not be loaded. Confirm the backend is running and the frontend proxy is connected.";
  return "Connected health records could not be loaded.";
}
