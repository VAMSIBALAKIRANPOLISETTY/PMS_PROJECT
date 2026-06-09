import { useEffect, useRef, useState } from "react";
import type { DragEvent, FormEvent, KeyboardEvent } from "react";
import axios from "axios";
import { Activity, ArrowRight, CheckCircle2, Sparkles, Stethoscope, Trash2, X } from "lucide-react";
import { api, authHeaders } from "../../api";
import { CarePrepGuide } from "../../components/CarePrepGuide";
import type { Assessment, Notify, TimelineRecord } from "../../types";
import { SymptomDrawer } from "./SymptomDrawer";

interface AssessmentFormProps {
  token: string;
  onCreated: () => Promise<void>;
  notify: Notify;
}

type TemperatureMode = "" | "available" | "unavailable";
type FollowUpChoice = "" | "Yes" | "No" | "Not sure";

interface FollowUpAnswer {
  choice: FollowUpChoice;
  note: string;
}

const emptyForm = {
  symptoms: [] as string[],
  severity: null as number | null,
  durationDays: "",
  temperatureMode: "" as TemperatureMode,
  temperatureF: "",
  chronicCondition: "",
};

export function AssessmentForm({ token, onCreated, notify }: AssessmentFormProps) {
  const primaryFollowActionRef = useRef<HTMLButtonElement | null>(null);
  const [draft, setDraft] = useState<Assessment | null>(null);
  const [result, setResult] = useState<Assessment | null>(null);
  const [message, setMessage] = useState("");
  const [followStep, setFollowStep] = useState(0);
  const [followAnswers, setFollowAnswers] = useState<FollowUpAnswer[]>([]);
  const [connectedRecords, setConnectedRecords] = useState<TimelineRecord[]>([]);
  const [includeConnected, setIncludeConnected] = useState(false);
  const [form, setForm] = useState(emptyForm);

  useEffect(() => {
    api.get("/assessments/pending", { headers: authHeaders(token) })
      .then((response) => {
        if (response.data?.id) {
          setDraft(response.data);
          setFollowAnswers(makeEmptyAnswers(response.data.followUpQuestions.length));
        }
      })
      .catch(() => undefined);
  }, [token]);

  function addSymptom(symptom: string) {
    if (!symptom) return;
    setForm((current) => {
      if (current.symptoms.includes(symptom) || current.symptoms.length >= 5) return current;
      return { ...current, symptoms: [...current.symptoms, symptom] };
    });
  }

  function removeSymptom(symptom: string) {
    setForm((current) => ({ ...current, symptoms: current.symptoms.filter((item) => item !== symptom) }));
  }

  function dropSymptom(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    addSymptom(event.dataTransfer.getData("text/plain"));
  }

  async function runAssessment(event: FormEvent) {
    event.preventDefault();
    setMessage("");
    if (draft) {
      setMessage("Finish or discard your current assessment draft before starting another.");
      return;
    }
    if (form.symptoms.length === 0 || form.severity === null || form.durationDays === "" || form.temperatureMode === "" || form.chronicCondition === "") {
      setMessage("Complete each assessment field before preparing follow-up questions.");
      return;
    }
    try {
      const response = await api.post("/assessments", {
        symptoms: form.symptoms,
        severity: form.severity,
        durationDays: Number(form.durationDays),
        temperatureAvailable: form.temperatureMode === "available",
        temperatureF: form.temperatureMode === "available" && form.temperatureF !== "" ? Number(form.temperatureF) : null,
        chronicCondition: form.chronicCondition,
        includeConnectedHealth: includeConnected,
        connectedHealthRecordIds: connectedRecords.map((record) => record.id),
      }, { headers: authHeaders(token) });
      setDraft(response.data);
      setResult(null);
      setFollowStep(0);
      setFollowAnswers(makeEmptyAnswers(response.data.followUpQuestions.length));
      notify("Intake saved. Answer the follow-up questions to prepare your care guide.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Assessment intake failed." : "Assessment intake failed.");
      notify("Assessment intake failed.", "danger");
    }
  }

  async function submitFollowUps() {
    if (!draft || followAnswers.some((answer) => answer.choice === "")) {
      setMessage("Choose an answer for every follow-up question before preparing your care guide.");
      return;
    }
    setMessage("");
    try {
      const response = await api.post(`/assessments/${draft.id}/follow-ups`, {
        answers: followAnswers.map(serializeAnswer),
      }, { headers: authHeaders(token) });
      setDraft(null);
      setResult(response.data);
      await onCreated();
      notify("Your care-preparation guide is ready.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Follow-up submission failed." : "Follow-up submission failed.");
      notify("Follow-up submission failed.", "danger");
    }
  }

  async function discardDraft() {
    if (!draft) return;
    try {
      await api.delete(`/assessments/${draft.id}/draft`, { headers: authHeaders(token) });
      setDraft(null);
      setFollowAnswers([]);
      setFollowStep(0);
      setMessage("");
      notify("Assessment draft discarded.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Draft could not be discarded." : "Draft could not be discarded.");
      notify("Draft could not be discarded.", "danger");
    }
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

  const currentQuestion = draft?.followUpQuestions[followStep];
  const currentAnswer = followAnswers[followStep] ?? { choice: "", note: "" };
  const answeredCount = followAnswers.filter((answer) => answer.choice !== "").length;

  return (
    <div className="assessment-workspace" data-section="assessment">
      <div className="assessment-layout">
        <div className="assessment-intake-grid">
          <section className="panel assessment-form-panel">
            <div className="section-title"><div><p className="eyebrow">Guided assessment</p><h2>Record what you are feeling</h2></div><Stethoscope size={24} /></div>
            <form className="form-grid" onSubmit={runAssessment}>
              <div className="symptom-dropbox" onDragOver={(event) => event.preventDefault()} onDrop={dropSymptom}>
                <div><strong>Main symptoms</strong><span>{form.symptoms.length}/5 selected. Click or drag symptoms from the drawer.</span></div>
                <div className="symptom-chip-row">
                  {form.symptoms.map((symptom) => (
                    <button type="button" key={symptom} onClick={() => removeSymptom(symptom)}>{symptom}<X size={14} /></button>
                  ))}
                  {form.symptoms.length === 0 && <span className="empty-drop-hint">Drop symptoms here</span>}
                </div>
              </div>
              <fieldset className="severity-field">
                <legend>Severity</legend>
                <span>Choose how strongly the symptoms are affecting you right now.</span>
                <div className="severity-scale">
                  {Array.from({ length: 10 }, (_, index) => index + 1).map((value) => (
                    <button type="button" className={form.severity === value ? "active" : ""} key={value} onClick={() => setForm({ ...form, severity: value })}>{value}</button>
                  ))}
                </div>
              </fieldset>
              <label>Duration days<input type="number" placeholder="Example: 2" min="0" max="365" value={form.durationDays} onChange={(event) => setForm({ ...form, durationDays: event.target.value })} required /></label>
              <div className="temperature-card">
                <strong>Temperature</strong>
                <div className="segmented">
                  <button type="button" className={form.temperatureMode === "available" ? "active" : ""} onClick={() => setForm({ ...form, temperatureMode: "available" })}>Enter now</button>
                  <button type="button" className={form.temperatureMode === "unavailable" ? "active" : ""} onClick={() => setForm({ ...form, temperatureMode: "unavailable", temperatureF: "" })}>Not available</button>
                </div>
                {form.temperatureMode === "available" ? (
                  <label>Temperature F<input type="number" placeholder="Example: 98.6" min="90" max="110" step="0.1" value={form.temperatureF} onChange={(event) => setForm({ ...form, temperatureF: event.target.value })} required /></label>
                ) : form.temperatureMode === "unavailable" ? (
                  <p>Choose this if you cannot take a temperature reading right now.</p>
                ) : (
                  <p>Select whether you can take a temperature reading right now.</p>
                )}
              </div>
              <label>Known chronic condition<select value={form.chronicCondition} onChange={(event) => setForm({ ...form, chronicCondition: event.target.value })} required><option value="">Select an option</option><option>None</option><option>Diabetes</option><option>Blood pressure</option><option>Asthma</option><option>Heart disease</option></select></label>
              {draft && <div className="form-message neutral">A saved draft is waiting below. Finish its questions or discard it before starting another.</div>}
              {message && <div className="form-message">{message}</div>}
              <div className="assessment-action-row">
                <button className="primary-button" disabled={!!draft}>Prepare care guide<ArrowRight size={18} /></button>
                <button className="ghost-button" type="button" disabled={!!draft} onClick={() => void useConnectedHealthData()}>
                  <Activity size={17} /> Use connected health data
                </button>
              </div>
              {includeConnected && <div className="success-row"><CheckCircle2 size={18} />Connected data will be included in this assessment.</div>}
            </form>
          </section>

          <SymptomDrawer selected={form.symptoms} onSelect={addSymptom} />
        </div>

          <section className="panel result-panel">
            <p className="eyebrow">Your assessment</p>
            {draft ? (
            <div className="draft-workspace">
              {draft.urgentWarning && <div className="urgent-warning"><strong>Urgent safety guidance</strong><p>{draft.urgentWarning}</p></div>}
              <div className="followup-box">
                {draft.connectedHealthSummary && <div className="summary-box compact-summary">{draft.connectedHealthSummary}</div>}
                <div className="section-title">
                  <div><p className="eyebrow">Follow-up questions</p><h2>{answeredCount}/{draft.followUpQuestions.length} answered</h2></div>
                  <button type="button" className="ghost-button danger-text" onClick={discardDraft}><Trash2 size={17} />Discard draft</button>
                </div>
                {currentQuestion && (
                  <div className="followup-card" onKeyDown={handleFollowUpKeyDown}>
                    <span>Question {followStep + 1} of {draft.followUpQuestions.length}</span>
                    <strong>{currentQuestion}</strong>
                    <div className="followup-choices">
                      {(["Yes", "No", "Not sure"] as FollowUpChoice[]).map((choice) => (
                        <button type="button" className={currentAnswer.choice === choice ? "active" : ""} key={choice} onClick={() => updateAnswer(followStep, choice, currentAnswer.note)}>{choice}</button>
                      ))}
                    </div>
                    <label>Optional note<textarea placeholder="Add context for this answer if helpful" value={currentAnswer.note} onChange={(event) => updateAnswer(followStep, currentAnswer.choice, event.target.value)} /></label>
                    <div className="profile-setup-actions">
                      <button type="button" className="ghost-button" disabled={followStep === 0} onClick={() => setFollowStep(Math.max(0, followStep - 1))}>Back</button>
                      {followStep < draft.followUpQuestions.length - 1 ? (
                        <button ref={primaryFollowActionRef} type="button" className="primary-button" disabled={!currentAnswer.choice} onClick={() => setFollowStep(followStep + 1)}>Next<ArrowRight size={18} /></button>
                      ) : (
                        <button ref={primaryFollowActionRef} type="button" className="primary-button" disabled={followAnswers.some((answer) => !answer.choice)} onClick={submitFollowUps}>Prepare guide<ArrowRight size={18} /></button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : result ? (
            <>
              {result.connectedHealthSummary && <div className="summary-box compact-summary">{result.connectedHealthSummary}</div>}
              <CarePrepGuide
                insight={result}
                riskLevel={result.riskLevel}
                riskScore={result.riskScore}
                reasons={result.reasons}
                suggestions={result.suggestions}
              />
            </>
          ) : (
            <div className="empty-state"><Sparkles size={32} /><h2>Your care guide will appear here</h2><p>Complete the intake and answer the follow-up questions to receive a clear summary and practical next steps.</p></div>
          )}
        </section>
      </div>
    </div>
  );

  function updateAnswer(index: number, choice: FollowUpChoice, note: string) {
    setFollowAnswers((current) => current.map((answer, answerIndex) => answerIndex === index ? { choice, note } : answer));
    if (choice) {
      window.setTimeout(() => primaryFollowActionRef.current?.focus(), 0);
    }
  }

  function handleFollowUpKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key !== "Enter") return;
    const target = event.target as HTMLElement;
    if (target.tagName === "TEXTAREA") return;
    event.preventDefault();
    if (!draft || !currentAnswer.choice) return;
    if (followStep < draft.followUpQuestions.length - 1) {
      setFollowStep(followStep + 1);
    } else if (!followAnswers.some((answer) => !answer.choice)) {
      void submitFollowUps();
    }
  }
}

function makeEmptyAnswers(count: number): FollowUpAnswer[] {
  return Array.from({ length: count }, () => ({ choice: "", note: "" }));
}

function serializeAnswer(answer: FollowUpAnswer) {
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
