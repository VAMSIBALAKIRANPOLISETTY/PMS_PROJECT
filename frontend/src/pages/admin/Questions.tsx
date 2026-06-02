import { useState } from "react";
import axios from "axios";
import { MessageSquareText, X } from "lucide-react";
import { api, authHeaders } from "../../api";
import { possibleSymptoms } from "../../data";
import type { Notify, Question } from "../../types";

interface QuestionsProps {
  token: string;
  questions: Question[];
  refresh: () => Promise<void>;
  notify: Notify;
}

export function Questions({ token, questions, refresh, notify }: QuestionsProps) {
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({ symptomKey: "General", prompt: "", active: true });

  async function saveQuestion() {
    setSaving(true);
    setMessage("");
    try {
      await api.post("/admin/questions", form, { headers: authHeaders(token) });
      setEditing(false);
      setForm({ symptomKey: "General", prompt: "", active: true });
      await refresh();
      notify("Assessment question added for future matching drafts.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Question could not be saved." : "Question could not be saved.");
      notify("Question could not be saved.", "danger");
    } finally {
      setSaving(false);
    }
  }

  async function toggleQuestion(question: Question) {
    try {
      await api.patch(`/admin/questions/${question.id}/active`, { active: !question.active }, { headers: authHeaders(token) });
      await refresh();
      notify(`Assessment question ${question.active ? "paused" : "activated"}.`);
    } catch {
      notify("Question status could not be updated.", "danger");
    }
  }

  return (
    <>
      <section className="panel wide" data-section="admin-questions">
        <div className="section-title"><div><p className="eyebrow">Follow-up guidance</p><h2>Assessment question bank</h2></div><button className="primary-button" type="button" onClick={() => setEditing(true)}>Create question</button></div>
        <p className="section-note">Active questions are selected by symptom and can join future guided assessments. General questions are eligible for every draft.</p>
        <div className="question-list">{questions.map((question, index) => <div className={question.active ? "" : "inactive"} key={question.id}><span>Q{index + 1}</span><strong>{question.prompt}</strong><small>{question.active ? "Active" : "Paused"} | {question.symptomKey}</small><button className="ghost-button compact-button" type="button" onClick={() => toggleQuestion(question)}>{question.active ? "Pause" : "Activate"}</button></div>)}</div>
      </section>

      {editing && (
        <div className="drawer-overlay" role="presentation" onMouseDown={() => setEditing(false)}>
          <aside className="admin-editor-drawer" role="dialog" aria-modal="true" aria-label="Create assessment question" onMouseDown={(event) => event.stopPropagation()}>
            <header className="drawer-header"><div><p className="eyebrow">Follow-up guidance</p><h2>Create question</h2></div><button className="icon-button" type="button" title="Close question form" onClick={() => setEditing(false)}><X size={18} /></button></header>
            <p className="section-note"><MessageSquareText size={17} /> Write a question that patients can answer with Yes, No, or Not sure.</p>
            <div className="form-grid admin-editor-form">
              <label>Use for symptom<select value={form.symptomKey} onChange={(event) => setForm({ ...form, symptomKey: event.target.value })}><option>General</option>{possibleSymptoms.map((symptom) => <option key={symptom}>{symptom}</option>)}</select></label>
              <label className="full">Question<textarea placeholder="Example: Has this symptom become worse since yesterday?" value={form.prompt} onChange={(event) => setForm({ ...form, prompt: event.target.value })} /></label>
              <label className="agreement-check full"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} />Activate this question for future assessments.</label>
            </div>
            {message && <div className="form-message">{message}</div>}
            <button className="primary-button full" type="button" disabled={saving || !form.prompt.trim()} onClick={saveQuestion}>{saving ? "Saving..." : "Save assessment question"}</button>
          </aside>
        </div>
      )}
    </>
  );
}
