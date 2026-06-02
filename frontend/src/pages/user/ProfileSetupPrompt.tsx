import { useMemo, useState } from "react";
import axios from "axios";
import { ArrowRight, CheckCircle2, Save } from "lucide-react";
import { api, authHeaders } from "../../api";
import type { Notify, User } from "../../types";

interface ProfileSetupPromptProps {
  user: User;
  token: string;
  updateUser: (user: User) => void;
  notify: Notify;
}

const historyQuestions = [
  { key: "allergies", label: "Do you have any allergies?", placeholder: "Example: No known allergies, dust, peanuts" },
  { key: "chronicConditions", label: "Do you have any chronic health conditions?", placeholder: "Example: Asthma, diabetes, blood pressure, none" },
  { key: "medications", label: "Are you taking any regular medicines?", placeholder: "Example: None, inhaler, BP medicine" },
  { key: "familyHistory", label: "Any important family health history?", placeholder: "Example: Diabetes in family, heart disease, none" },
  { key: "mentalHealthHistory", label: "Any psychiatric or mental health history we should consider?", placeholder: "Example: Anxiety treatment, depression history, none" },
  { key: "sleepQuality", label: "How is your sleep lately?", placeholder: "Example: 7 hours, disturbed sleep, insomnia" },
  { key: "lifestyle", label: "How would you describe your lifestyle?", placeholder: "Example: Active, desk work, smoker, occasional exercise" },
] as const;

type HistoryKey = (typeof historyQuestions)[number]["key"];

export function ProfileSetupPrompt({ user, token, updateUser, notify }: ProfileSetupPromptProps) {
  const [step, setStep] = useState(0);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [answers, setAnswers] = useState<Record<HistoryKey, string>>({
    allergies: user.allergies ?? "",
    chronicConditions: user.chronicConditions ?? "",
    medications: user.medications ?? "",
    familyHistory: user.familyHistory ?? "",
    mentalHealthHistory: user.mentalHealthHistory ?? "",
    sleepQuality: user.sleepQuality ?? "",
    lifestyle: user.lifestyle ?? "",
  });

  const question = historyQuestions[step];
  const answeredCount = useMemo(() => historyQuestions.filter((item) => answers[item.key]?.trim()).length, [answers]);
  const progress = Math.round((answeredCount / historyQuestions.length) * 100);

  async function saveProfile() {
    setSaving(true);
    setMessage("");
    try {
      const response = await api.put("/auth/profile", {
        fullName: user.fullName,
        age: user.age ?? 24,
        heightCm: user.heightCm ?? 170,
        weightKg: user.weightKg ?? 68,
        sex: user.sex ?? "Prefer not to say",
        ...answers,
      }, { headers: authHeaders(token) });
      updateUser(response.data);
      notify("Profile setup saved. Your assessments will use richer health history now.");
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
        <div>
          <p className="eyebrow">Profile setup</p>
          <h2>Finish your health history</h2>
        </div>
        <span className="setup-percent">{Math.max(user.profileCompletion ?? 0, progress)}%</span>
      </div>
      <div className="progress-track"><span style={{ width: `${Math.max(user.profileCompletion ?? 0, progress)}%` }} /></div>
      <div className="question-card">
        <span>Question {step + 1} of {historyQuestions.length}</span>
        <h3>{question.label}</h3>
        <textarea
          value={answers[question.key]}
          placeholder={question.placeholder}
          onChange={(event) => setAnswers((current) => ({ ...current, [question.key]: event.target.value }))}
        />
      </div>
      {message && <div className="form-message">{message}</div>}
      <div className="profile-setup-actions">
        <button className="ghost-button" type="button" onClick={() => setStep(Math.max(0, step - 1))} disabled={step === 0}>Back</button>
        {step < historyQuestions.length - 1 ? (
          <button className="primary-button" type="button" onClick={() => setStep(step + 1)}>Next question<ArrowRight size={18} /></button>
        ) : (
          <button className="primary-button" type="button" onClick={saveProfile} disabled={saving}>{saving ? "Saving..." : "Save setup"}<Save size={18} /></button>
        )}
      </div>
      {progress === 100 && <div className="setup-complete"><CheckCircle2 size={18} />All health history cards have answers.</div>}
    </section>
  );
}
