import { useState } from "react";
import axios from "axios";
import { ArrowRight, ShieldCheck, X } from "lucide-react";
import { api, authHeaders } from "../../api";
import { RiskPill } from "../../components/RiskPill";
import { possibleSymptoms } from "../../data";
import type { Notify, Rule } from "../../types";

interface RulesProps {
  token: string;
  rules: Rule[];
  refresh: () => Promise<void>;
  notify: Notify;
}

const emptyRule = {
  conditionLabel: "",
  primarySymptom: "",
  secondarySymptom: "",
  minSeverity: "",
  minDurationDays: "",
  chronicConditionKeyword: "",
  score: "55",
  urgent: false,
  active: true,
  explanation: "",
};

export function Rules({ token, rules, refresh, notify }: RulesProps) {
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState(emptyRule);

  async function saveRule() {
    setSaving(true);
    setMessage("");
    try {
      await api.post("/admin/rules", {
        ...form,
        minSeverity: form.minSeverity ? Number(form.minSeverity) : null,
        minDurationDays: form.minDurationDays ? Number(form.minDurationDays) : null,
        score: Number(form.score),
      }, { headers: authHeaders(token) });
      setEditing(false);
      setForm(emptyRule);
      await refresh();
      notify("Safety rule added. Future matching assessments can only be raised in risk.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Safety rule could not be saved." : "Safety rule could not be saved.");
      notify("Safety rule could not be saved.", "danger");
    } finally {
      setSaving(false);
    }
  }

  async function toggleRule(rule: Rule) {
    try {
      await api.patch(`/admin/rules/${rule.id}/active`, { active: !rule.active }, { headers: authHeaders(token) });
      await refresh();
      notify(`Safety rule ${rule.active ? "paused" : "activated"}.`);
    } catch {
      notify("Safety rule status could not be updated.", "danger");
    }
  }

  return (
    <>
      <section className="panel wide" data-section="admin-rules">
        <div className="section-title"><div><p className="eyebrow">Clinical safeguards</p><h2>Safety rule review</h2></div><button className="primary-button" type="button" onClick={() => setEditing(true)}>Add rule<ArrowRight size={17} /></button></div>
        <p className="section-note">Active operational rules raise awareness risk when every configured condition matches. Protected urgent safeguards always remain active.</p>
        <div className="rule-grid">{rules.map((rule) => (
          <article className={`rule-card ${rule.active === false ? "inactive" : ""}`} key={rule.id}>
            <div className="rule-card-heading"><span>{rule.conditionLabel}</span><RiskPill value={rule.riskLevel} /></div>
            <p>{rule.explanation}</p>
            <small>{describeRule(rule)}</small>
            <button className="ghost-button compact-button" type="button" onClick={() => toggleRule(rule)}>{rule.active === false ? "Activate rule" : "Pause rule"}</button>
          </article>
        ))}</div>
      </section>

      {editing && (
        <div className="drawer-overlay" role="presentation" onMouseDown={() => setEditing(false)}>
          <aside className="admin-editor-drawer" role="dialog" aria-modal="true" aria-label="Add safety rule" onMouseDown={(event) => event.stopPropagation()}>
            <header className="drawer-header"><div><p className="eyebrow">Clinical safeguards</p><h2>Add safety rule</h2></div><button className="icon-button" type="button" title="Close rule form" onClick={() => setEditing(false)}><X size={18} /></button></header>
            <p className="section-note"><ShieldCheck size={17} /> New rules can only raise awareness risk. They cannot lower or replace built-in emergency guidance.</p>
            <div className="form-grid admin-editor-form">
              <label className="full">Rule name<input value={form.conditionLabel} placeholder="Example: Persistent fever with weakness" onChange={(event) => setForm({ ...form, conditionLabel: event.target.value })} /></label>
              <label>Primary symptom<select value={form.primarySymptom} onChange={(event) => setForm({ ...form, primarySymptom: event.target.value })}><option value="">Select symptom</option>{possibleSymptoms.map((symptom) => <option key={symptom}>{symptom}</option>)}</select></label>
              <label>Second symptom optional<select value={form.secondarySymptom} onChange={(event) => setForm({ ...form, secondarySymptom: event.target.value })}><option value="">No second symptom</option>{possibleSymptoms.map((symptom) => <option key={symptom}>{symptom}</option>)}</select></label>
              <label>Minimum severity<input type="number" min="1" max="10" placeholder="Optional" value={form.minSeverity} onChange={(event) => setForm({ ...form, minSeverity: event.target.value })} /></label>
              <label>Minimum duration days<input type="number" min="0" max="365" placeholder="Optional" value={form.minDurationDays} onChange={(event) => setForm({ ...form, minDurationDays: event.target.value })} /></label>
              <label>Chronic-condition keyword<input placeholder="Optional" value={form.chronicConditionKeyword} onChange={(event) => setForm({ ...form, chronicConditionKeyword: event.target.value })} /></label>
              <label>Minimum score floor<input type="number" min="0" max="100" value={form.score} onChange={(event) => setForm({ ...form, score: event.target.value })} /></label>
              <label className="full">Explanation<textarea placeholder="Explain why this combination needs closer review" value={form.explanation} onChange={(event) => setForm({ ...form, explanation: event.target.value })} /></label>
              <label className="agreement-check full"><input type="checkbox" checked={form.urgent} onChange={(event) => setForm({ ...form, urgent: event.target.checked })} />Show standard urgent guidance when this rule matches.</label>
              <label className="agreement-check full"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} />Activate this rule for future assessments.</label>
            </div>
            {message && <div className="form-message">{message}</div>}
            <button className="primary-button full" type="button" disabled={saving || !form.conditionLabel.trim() || !form.primarySymptom || !form.explanation.trim()} onClick={saveRule}>{saving ? "Saving..." : "Save safety rule"}</button>
          </aside>
        </div>
      )}
    </>
  );
}

function describeRule(rule: Rule) {
  const parts = [rule.primarySymptom, rule.secondarySymptom].filter(Boolean);
  if (rule.minSeverity) parts.push(`severity ${rule.minSeverity}+`);
  if (rule.minDurationDays != null) parts.push(`${rule.minDurationDays}+ days`);
  if (rule.chronicConditionKeyword) parts.push(`history: ${rule.chronicConditionKeyword}`);
  parts.push(`score floor ${rule.score}`);
  if (rule.urgent) parts.push("urgent guidance");
  return parts.join(" | ");
}
