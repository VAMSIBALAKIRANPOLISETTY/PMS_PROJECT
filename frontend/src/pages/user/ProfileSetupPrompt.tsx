import { useMemo, useState } from "react";
import axios from "axios";
import { ArrowRight, CheckCircle2, Save } from "lucide-react";
import { api, authHeaders } from "../../api";
import type { Notify, User } from "../../types";

type HistoryKey = "allergies" | "chronicConditions" | "medications" | "familyHistory" | "mentalHealthHistory" | "sleepQuality" | "lifestyle";

interface HistoryQuestion {
  key: HistoryKey;
  label: string;
  options: readonly string[];
  detailsFor: readonly string[];
  requiredDetailsFor?: readonly string[];
  detailPlaceholder: string;
}

const historyQuestions: readonly HistoryQuestion[] = [
  { key: "allergies", label: "Do you have any allergies?", options: ["No known allergies", "Medication allergy", "Food allergy", "Environmental allergy", "Other"], detailsFor: ["Medication allergy", "Food allergy", "Environmental allergy", "Other"], detailPlaceholder: "Add allergy details if helpful" },
  { key: "chronicConditions", label: "Do you have any chronic health conditions?", options: ["None", "Asthma", "Diabetes", "Hypertension", "Heart disease", "Thyroid condition", "Other"], detailsFor: ["Asthma", "Diabetes", "Hypertension", "Heart disease", "Thyroid condition", "Other"], detailPlaceholder: "Add condition details if helpful" },
  { key: "medications", label: "Are you taking any regular medicines?", options: ["None", "Yes - add details", "Prefer not to say"], detailsFor: ["Yes - add details"], requiredDetailsFor: ["Yes - add details"], detailPlaceholder: "Add medicine names or notes" },
  { key: "familyHistory", label: "Is there important family health history you want to note?", options: ["None known", "Diabetes", "Heart disease", "Hypertension", "Other", "Prefer not to say"], detailsFor: ["Other"], detailPlaceholder: "Add family-history details" },
  { key: "mentalHealthHistory", label: "Is there mental health history you want considered?", options: ["None", "Anxiety", "Depression", "Other", "Prefer not to say"], detailsFor: ["Other"], detailPlaceholder: "Add details you want to include" },
  { key: "sleepQuality", label: "How is your sleep lately?", options: ["Restful", "Sometimes disrupted", "Frequently disrupted", "Insomnia concerns", "Prefer not to say"], detailsFor: [], detailPlaceholder: "" },
  { key: "lifestyle", label: "How would you describe your lifestyle?", options: ["Active", "Moderately active", "Mostly seated", "Tobacco use", "Other", "Prefer not to say"], detailsFor: ["Tobacco use", "Other"], detailPlaceholder: "Add lifestyle details if helpful" },
] as const;

type HistoryAnswer = { choice: string; detail: string };
type HistoryAnswers = Record<HistoryKey, HistoryAnswer>;

interface ProfileSetupPromptProps {
  user: User;
  token: string;
  updateUser: (user: User) => void;
  notify: Notify;
}

export function ProfileSetupPrompt({ user, token, updateUser, notify }: ProfileSetupPromptProps) {
  const [step, setStep] = useState(0);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [answers, setAnswers] = useState<HistoryAnswers>(() => initialAnswers(user));

  const question = historyQuestions[step];
  const currentAnswer = answers[question.key];
  const answeredCount = useMemo(() => historyQuestions.filter((item) => validAnswer(item, answers[item.key])).length, [answers]);
  const progress = Math.round((answeredCount / historyQuestions.length) * 100);
  const currentValid = validAnswer(question, currentAnswer);

  async function saveProfile() {
    if (answeredCount !== historyQuestions.length) {
      setMessage("Answer every health-history question before saving your setup.");
      return;
    }
    setSaving(true);
    setMessage("");
    try {
      const response = await api.put("/auth/profile", {
        fullName: user.fullName,
        age: user.age,
        heightCm: user.heightCm,
        weightKg: user.weightKg,
        sex: user.sex,
        allergies: serializeAnswer(answers.allergies),
        chronicConditions: serializeAnswer(answers.chronicConditions),
        medications: serializeAnswer(answers.medications),
        familyHistory: serializeAnswer(answers.familyHistory),
        mentalHealthHistory: serializeAnswer(answers.mentalHealthHistory),
        sleepQuality: serializeAnswer(answers.sleepQuality),
        lifestyle: serializeAnswer(answers.lifestyle),
      }, { headers: authHeaders(token) });
      updateUser(response.data);
      notify("Profile setup saved. Your assessments will use your reviewed health history.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Could not save profile." : "Could not save profile.");
      notify("Profile setup could not be saved.", "danger");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="profile-setup-card" data-section="profile-setup">
      <div className="section-title">
        <div><p className="eyebrow">Profile setup</p><h2>Finish your health history</h2></div>
        <span className="setup-percent">{progress}%</span>
      </div>
      <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>
      <div className="question-card">
        <span>Question {step + 1} of {historyQuestions.length}</span>
        <h3>{question.label}</h3>
        <select value={currentAnswer.choice} onChange={(event) => updateAnswer(question.key, event.target.value, "")}>
          <option value="">Select an answer</option>
          {question.options.map((option) => <option value={option} key={option}>{option}</option>)}
        </select>
        {question.detailsFor.includes(currentAnswer.choice) && (
          <input value={currentAnswer.detail} placeholder={question.detailPlaceholder} onChange={(event) => updateAnswer(question.key, currentAnswer.choice, event.target.value)} />
        )}
      </div>
      {message && <div className="form-message">{message}</div>}
      <div className="profile-setup-actions">
        <button className="ghost-button" type="button" onClick={() => setStep(Math.max(0, step - 1))} disabled={step === 0}>Back</button>
        {step < historyQuestions.length - 1 ? (
          <button className="primary-button" type="button" disabled={!currentValid} onClick={() => setStep(step + 1)}>Next question<ArrowRight size={18} /></button>
        ) : (
          <button className="primary-button" type="button" onClick={saveProfile} disabled={saving || answeredCount !== historyQuestions.length}>{saving ? "Saving..." : "Save setup"}<Save size={18} /></button>
        )}
      </div>
      {progress === 100 && <div className="setup-complete"><CheckCircle2 size={18} />All health-history questions have reviewed answers.</div>}
    </section>
  );

  function updateAnswer(key: HistoryKey, choice: string, detail: string) {
    setAnswers((current) => ({ ...current, [key]: { choice, detail } }));
  }
}

function initialAnswers(user: User): HistoryAnswers {
  return {
    allergies: parseAnswer(user.allergies, historyQuestions[0].options),
    chronicConditions: parseAnswer(user.chronicConditions, historyQuestions[1].options),
    medications: parseAnswer(user.medications, historyQuestions[2].options),
    familyHistory: parseAnswer(user.familyHistory, historyQuestions[3].options),
    mentalHealthHistory: parseAnswer(user.mentalHealthHistory, historyQuestions[4].options),
    sleepQuality: parseAnswer(user.sleepQuality, historyQuestions[5].options),
    lifestyle: parseAnswer(user.lifestyle, historyQuestions[6].options),
  };
}

function parseAnswer(value: string | undefined, options: readonly string[]): HistoryAnswer {
  if (!value) return { choice: "", detail: "" };
  const exact = options.find((option) => option === value);
  if (exact) return { choice: exact, detail: "" };
  const prefixed = options.find((option) => value.startsWith(`${option}: `));
  if (prefixed) return { choice: prefixed, detail: value.slice(prefixed.length + 2) };
  return options.includes("Other") ? { choice: "Other", detail: value } : { choice: "", detail: "" };
}

function validAnswer(question: HistoryQuestion, answer: HistoryAnswer) {
  if (!answer.choice) return false;
  return !question.requiredDetailsFor?.includes(answer.choice) || Boolean(answer.detail.trim());
}

function serializeAnswer(answer: HistoryAnswer) {
  return answer.detail.trim() ? `${answer.choice}: ${answer.detail.trim()}` : answer.choice;
}
