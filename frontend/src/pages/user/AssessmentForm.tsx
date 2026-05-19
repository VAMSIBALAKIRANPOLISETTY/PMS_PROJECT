import { useState } from "react";
import type { FormEvent } from "react";
import axios from "axios";
import { ArrowRight, CheckCircle2, Sparkles, Stethoscope } from "lucide-react";
import { api, authHeaders } from "../../api";
import { RiskPill } from "../../components/RiskPill";
import type { Assessment } from "../../types";
import { clampNumber, displayRisk } from "../../utils";
import { SymptomDrawer } from "./SymptomDrawer";

interface AssessmentFormProps {
  token: string;
  onCreated: () => Promise<void>;
}

export function AssessmentForm({ token, onCreated }: AssessmentFormProps) {
  const [result, setResult] = useState<Assessment | null>(null);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({
    mainSymptom: "Fever",
    severity: 5,
    durationDays: 1,
    temperatureF: 98.6,
    oxygenLevel: 98,
    heartRate: 76,
    chronicCondition: "None",
  });

  function updateNumber(field: keyof typeof form, value: string, min: number, max: number, fallback: number) {
    setForm((current) => ({ ...current, [field]: clampNumber(value, min, max, fallback) }));
  }

  async function runAssessment(event: FormEvent) {
    event.preventDefault();
    setMessage("");
    try {
      const response = await api.post("/assessments", form, { headers: authHeaders(token) });
      setResult(response.data);
      await onCreated();
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Assessment failed." : "Assessment failed.");
    }
  }

  return (
    <div className="assessment-workspace" data-section="assessment">
      <div className="split-layout">
        <section className="panel">
          <div className="section-title"><div><p className="eyebrow">User flow</p><h2>Health assessment</h2></div><Stethoscope size={24} /></div>
          <form className="form-grid" onSubmit={runAssessment}>
            <label>Main symptom<input value={form.mainSymptom} minLength={2} maxLength={120} onChange={(event) => setForm({ ...form, mainSymptom: event.target.value })} required /></label>
            <label>Severity<input type="range" min="1" max="10" value={form.severity} onChange={(event) => updateNumber("severity", event.target.value, 1, 10, 5)} /><span className="range-value">{form.severity}/10</span></label>
            <label>Duration days<input type="number" min="0" max="365" value={form.durationDays} onChange={(event) => updateNumber("durationDays", event.target.value, 0, 365, 0)} /></label>
            <label>Temperature F<input type="number" min="90" max="110" step="0.1" value={form.temperatureF} onChange={(event) => updateNumber("temperatureF", event.target.value, 90, 110, 98.6)} /></label>
            <label>Oxygen level<input type="number" min="50" max="100" value={form.oxygenLevel} onChange={(event) => updateNumber("oxygenLevel", event.target.value, 50, 100, 98)} /></label>
            <label>Heart rate<input type="number" min="35" max="220" value={form.heartRate} onChange={(event) => updateNumber("heartRate", event.target.value, 35, 220, 76)} /></label>
            <label>Chronic condition<select value={form.chronicCondition} onChange={(event) => setForm({ ...form, chronicCondition: event.target.value })}><option>None</option><option>Diabetes</option><option>Blood pressure</option><option>Asthma</option><option>Heart disease</option></select></label>
            {message && <div className="form-message">{message}</div>}
            <button className="primary-button full">Generate safe insight<ArrowRight size={18} /></button>
          </form>
        </section>
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
                <h2>Follow-up questions</h2>
                {result.followUpQuestions.map((question, index) => <label key={question}>Q{index + 1}. {question}<input placeholder="Your answer" /></label>)}
              </div>
            </>
          ) : (
            <div className="empty-state"><Sparkles size={32} /><h2>Result appears here</h2><p>Submit the form to receive backend-generated risk, reasons, suggestions, and follow-up questions.</p></div>
          )}
        </section>
      </div>
      <SymptomDrawer selected={form.mainSymptom} onSelect={(mainSymptom) => setForm({ ...form, mainSymptom })} />
    </div>
  );
}
