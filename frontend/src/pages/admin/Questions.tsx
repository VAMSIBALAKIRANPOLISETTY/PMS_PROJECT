import { useState } from "react";
import axios from "axios";
import { MessageSquareText, Sparkles, X } from "lucide-react";
import { api, authHeaders } from "../../api";
import { possibleSymptoms } from "../../data";
import type { Notify, Question, QuestionSuggestionResponse } from "../../types";

interface QuestionsProps {
  token: string;
  questions: Question[];
  refresh: () => Promise<void>;
  notify: Notify;
}

export function Questions({ token, questions, refresh, notify }: QuestionsProps) {
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [suggesting, setSuggesting] = useState(false);
  const [suggestionOpen, setSuggestionOpen] = useState(false);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({ symptomKey: "General", prompt: "", active: true });
  const [suggestionForm, setSuggestionForm] = useState({ symptomKey: "General", focus: "" });
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [selectedSuggestions, setSelectedSuggestions] = useState<string[]>([]);
  const [suggestionMode, setSuggestionMode] = useState("");
  const [suggestionMessage, setSuggestionMessage] = useState("");

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

  async function suggestQuestions() {
    setSuggesting(true);
    setSuggestionMessage("");
    try {
      const response = await api.post<QuestionSuggestionResponse>("/admin/questions/suggest", suggestionForm, { headers: authHeaders(token) });
      setSuggestions(response.data.suggestions);
      setSelectedSuggestions(response.data.suggestions);
      setSuggestionMode(response.data.aiMode);
      notify(`AI question drafts prepared with ${response.data.aiMode.toLowerCase()} support.`);
    } catch (error) {
      setSuggestionMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Question suggestions could not be prepared." : "Question suggestions could not be prepared.");
      notify("Question suggestions could not be prepared.", "danger");
    } finally {
      setSuggesting(false);
    }
  }

  async function saveSelectedSuggestions() {
    if (selectedSuggestions.length === 0) {
      setSuggestionMessage("Select at least one draft question before saving.");
      return;
    }
    setSaving(true);
    setSuggestionMessage("");
    try {
      for (const prompt of selectedSuggestions) {
        await api.post("/admin/questions", { symptomKey: suggestionForm.symptomKey, prompt, active: false }, { headers: authHeaders(token) });
      }
      setSuggestionOpen(false);
      setSuggestions([]);
      setSelectedSuggestions([]);
      await refresh();
      notify("Selected AI drafts saved as paused questions for staff review.");
    } catch (error) {
      setSuggestionMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Selected suggestions could not be saved." : "Selected suggestions could not be saved.");
      notify("Selected suggestions could not be saved.", "danger");
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <section className="panel wide" data-section="admin-questions">
        <div className="section-title"><div><p className="eyebrow">Follow-up guidance</p><h2>Assessment question bank</h2></div><div className="section-actions"><button className="ghost-button" type="button" onClick={() => setSuggestionOpen(true)}><Sparkles size={17} />Suggest with AI</button><button className="primary-button" type="button" onClick={() => setEditing(true)}>Create question</button></div></div>
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

      {suggestionOpen && (
        <div className="drawer-overlay" role="presentation" onMouseDown={() => setSuggestionOpen(false)}>
          <aside className="admin-editor-drawer" role="dialog" aria-modal="true" aria-label="Suggest assessment questions with AI" onMouseDown={(event) => event.stopPropagation()}>
            <header className="drawer-header"><div><p className="eyebrow">AI-assisted drafts</p><h2>Suggest questions</h2></div><button className="icon-button" type="button" title="Close AI suggestions" onClick={() => setSuggestionOpen(false)}><X size={18} /></button></header>
            <p className="section-note"><Sparkles size={17} /> Drafts are for staff review only. They are saved as paused questions until activated.</p>
            <div className="form-grid admin-editor-form">
              <label>Use for symptom<select value={suggestionForm.symptomKey} onChange={(event) => setSuggestionForm({ ...suggestionForm, symptomKey: event.target.value })}><option>General</option>{possibleSymptoms.map((symptom) => <option key={symptom}>{symptom}</option>)}</select></label>
              <label className="full">Optional focus<textarea placeholder="Example: duration, severity changes, breathing context, medicine context" value={suggestionForm.focus} onChange={(event) => setSuggestionForm({ ...suggestionForm, focus: event.target.value })} /></label>
            </div>
            <button className="ghost-button full" type="button" disabled={suggesting} onClick={suggestQuestions}>{suggesting ? "Preparing drafts..." : "Generate draft questions"}</button>
            {suggestionMode && <p className="summary-box">Draft source: {suggestionMode === "OLLAMA" ? "Gemma 4 31B" : suggestionMode === "OPENAI" ? "OpenAI fallback" : "Internal fallback"}</p>}
            {suggestions.length > 0 && (
              <div className="suggestion-list">
                {suggestions.map((suggestion) => (
                  <label className="agreement-check" key={suggestion}>
                    <input
                      type="checkbox"
                      checked={selectedSuggestions.includes(suggestion)}
                      onChange={(event) => setSelectedSuggestions((current) => event.target.checked ? [...current, suggestion] : current.filter((item) => item !== suggestion))}
                    />
                    <span>{suggestion}</span>
                  </label>
                ))}
              </div>
            )}
            {suggestionMessage && <div className="form-message">{suggestionMessage}</div>}
            <button className="primary-button full" type="button" disabled={saving || selectedSuggestions.length === 0} onClick={saveSelectedSuggestions}>{saving ? "Saving..." : "Save selected as paused questions"}</button>
          </aside>
        </div>
      )}
    </>
  );
}
