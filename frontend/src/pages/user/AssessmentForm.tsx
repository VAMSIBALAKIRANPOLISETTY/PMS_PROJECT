import { useState } from "react";
import type { DragEvent, FormEvent } from "react";
import axios from "axios";
import { ArrowRight, CheckCircle2, Sparkles, Stethoscope, X } from "lucide-react";
import { api, authHeaders } from "../../api";
import { RiskPill } from "../../components/RiskPill";
import type { Assessment, Notify } from "../../types";
import { clampNumber, displayRisk } from "../../utils";
import { SymptomDrawer } from "./SymptomDrawer";

interface AssessmentFormProps {
  token: string;
  onCreated: () => Promise<void>;
  notify: Notify;
}

export function AssessmentForm({ token, onCreated, notify }: AssessmentFormProps) {
  const [result, setResult] = useState<Assessment | null>(null);
  const [message, setMessage] = useState("");
  const [followStep, setFollowStep] = useState(0);
  const [followAnswers, setFollowAnswers] = useState<string[]>([]);
  const [form, setForm] = useState({
    symptoms: ["Fever"],
    severity: 5,
    durationDays: 1,
    temperatureMode: "available" as "available" | "unavailable",
    temperatureF: 98.6,
    chronicCondition: "None",
  });

  function updateNumber(field: "severity" | "durationDays" | "temperatureF", value: string, min: number, max: number, fallback: number) {
    setForm((current) => ({ ...current, [field]: clampNumber(value, min, max, fallback) }));
  }

  function addSymptom(symptom: string) {
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
    if (form.symptoms.length === 0) {
      setMessage("Add at least one symptom before generating an assessment.");
      return;
    }
    try {
      const response = await api.post("/assessments", {
        symptoms: form.symptoms,
        severity: form.severity,
        durationDays: form.durationDays,
        temperatureAvailable: form.temperatureMode === "available",
        temperatureF: form.temperatureMode === "available" ? form.temperatureF : null,
        chronicCondition: form.chronicCondition,
      }, { headers: authHeaders(token) });
      setResult(response.data);
      setFollowStep(0);
      setFollowAnswers(Array(response.data.followUpQuestions.length).fill(""));
      await onCreated();
      notify("Assessment generated successfully. Follow-up questions are ready.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Assessment failed." : "Assessment failed.");
      notify("Assessment generation failed.", "danger");
    }
  }

  async function submitFollowUps() {
    if (!result) return;
    setMessage("");
    try {
      const response = await api.post(`/assessments/${result.id}/follow-ups`, {
        answers: followAnswers.map((answer) => answer.trim()).filter(Boolean),
      }, { headers: authHeaders(token) });
      setResult(response.data);
      await onCreated();
      notify("Follow-up answers submitted. Insights were refreshed.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Follow-up submission failed." : "Follow-up submission failed.");
      notify("Follow-up submission failed.", "danger");
    }
  }

  const currentQuestion = result?.followUpQuestions[followStep];
  const answeredCount = followAnswers.filter((answer) => answer.trim()).length;

  return (
    <div className="assessment-workspace" data-section="assessment">
      <div className="assessment-layout">
        <section className="panel assessment-form-panel">
          <div className="section-title"><div><p className="eyebrow">User flow</p><h2>Health assessment</h2></div><Stethoscope size={24} /></div>
          <form className="form-grid" onSubmit={runAssessment}>
            <div className="symptom-dropbox" onDragOver={(event) => event.preventDefault()} onDrop={dropSymptom}>
              <div><strong>Main symptoms</strong><span>{form.symptoms.length}/5 selected. Click or drag symptoms from the drawer.</span></div>
              <div className="symptom-chip-row">
                {form.symptoms.map((symptom) => (
                  <button type="button" key={symptom} onClick={() => removeSymptom(symptom)}>{symptom}<X size={14} /></button>
                ))}
                {form.symptoms.length === 0 && <span className="empty-drop-hint">Drop symptom here</span>}
              </div>
            </div>
            <label>Severity<input type="range" min="1" max="10" value={form.severity} onChange={(event) => updateNumber("severity", event.target.value, 1, 10, 5)} /><span className="range-value">{form.severity}/10</span></label>
            <label>Duration days<input type="number" min="0" max="365" value={form.durationDays} onChange={(event) => updateNumber("durationDays", event.target.value, 0, 365, 0)} /></label>
            <div className="temperature-card">
              <strong>Temperature</strong>
              <div className="segmented">
                <button type="button" className={form.temperatureMode === "available" ? "active" : ""} onClick={() => setForm({ ...form, temperatureMode: "available" })}>Enter now</button>
                <button type="button" className={form.temperatureMode === "unavailable" ? "active" : ""} onClick={() => setForm({ ...form, temperatureMode: "unavailable" })}>Not available</button>
              </div>
              {form.temperatureMode === "available" ? (
                <label>Temperature F<input type="number" min="90" max="110" step="0.1" value={form.temperatureF} onChange={(event) => updateNumber("temperatureF", event.target.value, 90, 110, 98.6)} /></label>
              ) : (
                <p>Use this when the user cannot check temperature at the moment.</p>
              )}
            </div>
            <label>Known chronic condition<select value={form.chronicCondition} onChange={(event) => setForm({ ...form, chronicCondition: event.target.value })}><option>None</option><option>Diabetes</option><option>Blood pressure</option><option>Asthma</option><option>Heart disease</option></select></label>
            {message && <div className="form-message">{message}</div>}
            <button className="primary-button full">Generate safe insight<ArrowRight size={18} /></button>
          </form>
        </section>

        <SymptomDrawer selected={form.symptoms} onSelect={addSymptom} />

        <section className="panel result-panel">
          <p className="eyebrow">Final output</p>
          {result ? (
            <>
              <div className="score-ring"><strong>{result.riskScore}</strong><span>Risk score</span></div>
              <RiskPill value={result.riskLevel} />
              <h2>{displayRisk(result.riskLevel)} risk health awareness result</h2>
              <p>This is a non-diagnostic explanation based on entered data. Consult a qualified medical professional for diagnosis, treatment, or urgent symptoms.</p>
              <ul className="reason-list">{result.reasons.map((reason) => <li key={reason}><CheckCircle2 size={17} />{reason}</li>)}</ul>
              <div className="followup-box">
                <div className="section-title"><div><p className="eyebrow">Follow-up questions</p><h2>{answeredCount}/{result.followUpQuestions.length} answered</h2></div></div>
                {currentQuestion && (
                  <div className="followup-card">
                    <span>Question {followStep + 1} of {result.followUpQuestions.length}</span>
                    <strong>{currentQuestion}</strong>
                    <textarea
                      placeholder="Type your answer"
                      value={followAnswers[followStep] ?? ""}
                      onChange={(event) => setFollowAnswers((current) => current.map((answer, index) => index === followStep ? event.target.value : answer))}
                    />
                    <div className="profile-setup-actions">
                      <button type="button" className="ghost-button" disabled={followStep === 0} onClick={() => setFollowStep(Math.max(0, followStep - 1))}>Back</button>
                      {followStep < result.followUpQuestions.length - 1 ? (
                        <button type="button" className="primary-button" onClick={() => setFollowStep(followStep + 1)}>Next<ArrowRight size={18} /></button>
                      ) : (
                        <button type="button" className="primary-button" onClick={submitFollowUps}>Submit answers<ArrowRight size={18} /></button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="empty-state"><Sparkles size={32} /><h2>Result appears here</h2><p>Submit the form to receive backend-generated risk, reasons, suggestions, and follow-up questions.</p></div>
          )}
        </section>
      </div>
    </div>
  );
}
