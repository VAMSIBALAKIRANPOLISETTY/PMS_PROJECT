import { useState } from "react";
import type { FormEvent } from "react";
import axios from "axios";
import { ArrowLeft, ArrowRight, ShieldCheck } from "lucide-react";
import { api } from "../../api";
import type { User } from "../../types";
import { feetInchesToCm } from "../../utils";

export type AuthMode = "login" | "signup" | "staff-login";
type HeightUnit = "cm" | "imperial";

interface AuthPageProps {
  initialMode: AuthMode;
  onSuccess: (token: string, user: User) => void;
  onBack: () => void;
}

export function AuthPage({ initialMode, onSuccess, onBack }: AuthPageProps) {
  const [authMode, setAuthMode] = useState(initialMode);
  const [message, setMessage] = useState("");
  const [signupStep, setSignupStep] = useState(1);
  const [heightUnit, setHeightUnit] = useState<HeightUnit>("cm");
  const [form, setForm] = useState({
    identifier: "",
    email: "",
    username: "",
    fullName: "",
    password: "",
    age: "",
    heightCm: "",
    heightFeet: "",
    heightInches: "",
    weightKg: "",
    sex: "",
    privacyNoticeAccepted: false,
    termsAccepted: false,
  });

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage("");
    if (authMode === "signup" && signupStep < 3) {
      setSignupStep((current) => current + 1);
      return;
    }
    try {
      const response = authMode === "signup"
        ? await api.post("/auth/register", {
          email: form.email,
          username: form.username,
          fullName: form.fullName,
          password: form.password,
          age: Number(form.age),
          heightCm: heightUnit === "cm" ? Number(form.heightCm) : feetInchesToCm(form.heightFeet, form.heightInches),
          weightKg: Number(form.weightKg),
          sex: form.sex,
          privacyNoticeAccepted: form.privacyNoticeAccepted,
          termsAccepted: form.termsAccepted,
        })
        : await api.post(authMode === "staff-login" ? "/auth/staff-login" : "/auth/login", {
          identifier: form.identifier,
          password: form.password,
        });
      onSuccess(response.data.token, response.data.user);
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Authentication failed." : "Authentication failed.");
    }
  }

  return (
     <div className="auth-page" data-section="auth">
      {/* <div className="auth-backdrop" aria-hidden="true">
        <div className="auth-device-card">
          <HeartPulse size={28} />
          <strong>Safe health workspace</strong>
          <span>Profile, assessment, report, and follow-up history in one flow.</span>
        </div>
        <div className="auth-mini-grid">
          <span><ShieldCheck size={17} /> Awareness only</span>
          <span><ClipboardCheck size={17} /> Guided setup</span>
          <span>4-7 follow-ups</span>
        </div>
      </div> */}
      <section className={`panel auth-panel ${authMode === "signup" ? "signup-panel" : ""}`}>
        <div>
          <p className="eyebrow">{authMode === "signup" ? "Create account" : authMode === "staff-login" ? "Staff access" : "Welcome back"}</p>
          <h2>{authMode === "signup" ? "Create your personal health workspace" : authMode === "staff-login" ? "Login to the clinical operations workspace" : "Login to your health workspace"}</h2>
        </div>
        {authMode === "signup" && (
          <div className="signup-progress" aria-label={`Signup step ${signupStep} of 3`}>
            {["Account details", "Privacy and consent", "Terms and create account"].map((label, index) => (
              <div className={signupStep >= index + 1 ? "active" : ""} key={label}><span>{index + 1}</span><strong>{label}</strong></div>
            ))}
          </div>
        )}
        <form className="form-grid auth-form" onSubmit={submit}>
          {authMode !== "signup" ? (
            <label>Email or username<input placeholder="Enter your email or username" value={form.identifier} onChange={(event) => setForm({ ...form, identifier: event.target.value })} required /></label>
          ) : signupStep === 1 ? (
            <>
              <label>Email<input type="email" placeholder="name@example.com" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required /></label>
              <label>Username<input placeholder="Choose a username" value={form.username} minLength={3} onChange={(event) => setForm({ ...form, username: event.target.value })} required /></label>
              <label>Full name<input placeholder="Enter your full name" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required /></label>
              <label>Age<input type="number" placeholder="18 or older" min="18" max="120" value={form.age} onChange={(event) => setForm({ ...form, age: event.target.value })} required /></label>
              <div className="height-field">
                <div className="field-heading"><span>Height</span><div className="segmented compact"><button type="button" className={heightUnit === "cm" ? "active" : ""} onClick={() => setHeightUnit("cm")}>cm</button><button type="button" className={heightUnit === "imperial" ? "active" : ""} onClick={() => setHeightUnit("imperial")}>ft + in</button></div></div>
                {heightUnit === "cm" ? (
                  <input type="number" aria-label="Height cm" placeholder="Example: 170" min="30" max="260" step="0.1" value={form.heightCm} onChange={(event) => setForm({ ...form, heightCm: event.target.value })} required />
                ) : (
                  <div className="imperial-height-grid"><input type="number" aria-label="Height feet" placeholder="Feet" min="1" max="8" value={form.heightFeet} onChange={(event) => setForm({ ...form, heightFeet: event.target.value })} required /><input type="number" aria-label="Height inches" placeholder="Inches" min="0" max="11" value={form.heightInches} onChange={(event) => setForm({ ...form, heightInches: event.target.value })} required /></div>
                )}
              </div>
              <label>Weight kg<input type="number" placeholder="Example: 68" min="2" max="350" step="0.1" value={form.weightKg} onChange={(event) => setForm({ ...form, weightKg: event.target.value })} required /></label>
              <label>Sex<select value={form.sex} onChange={(event) => setForm({ ...form, sex: event.target.value })} required><option value="">Select an option</option><option>Female</option><option>Male</option><option>Intersex</option><option>Prefer not to say</option></select></label>
            </>
          ) : signupStep === 2 ? (
            <div className="signup-review">
              <ShieldCheck size={26} />
              <h3>Review how your information is used.</h3>
              <p>PMS Health stores your profile details, symptoms, health-history answers, assessment responses, and report notes in your private care-preparation workspace.</p>
              <p>This is not a hospital portal or HIPAA authorization form. PMS Health does not provide diagnosis, prescriptions, or emergency service.</p>
              <p>For privacy guidance, contact privacy@pmshealth.example.</p>
              <label className="agreement-check"><input type="checkbox" checked={form.privacyNoticeAccepted} onChange={(event) => setForm({ ...form, privacyNoticeAccepted: event.target.checked })} required />I reviewed and accept the PMS Health privacy notice.</label>
            </div>
          ) : (
            <div className="signup-review">
              <ShieldCheck size={26} />
              <h3>Accept the terms before creating your account.</h3>
              <p>Use PMS Health to prepare accurate notes for a medical conversation. Keep your password private and enter current information.</p>
              <p>For severe, sudden, or worsening symptoms, contact local emergency services or seek urgent professional care immediately.</p>
              <p>PMS Health is for lawful personal care preparation only. It does not replace a qualified medical professional.</p>
              <label className="agreement-check"><input type="checkbox" checked={form.termsAccepted} onChange={(event) => setForm({ ...form, termsAccepted: event.target.checked })} required />I reviewed and accept the PMS Health terms of use.</label>
            </div>
          )}
          {(authMode !== "signup" || signupStep === 1) && <label>Password<input type="password" placeholder="Enter at least 8 characters" minLength={8} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /></label>}
          {message && <div className="form-message">{message}</div>}
          {authMode === "signup" && signupStep > 1 && <button type="button" className="ghost-button" onClick={() => setSignupStep((current) => current - 1)}><ArrowLeft size={18} />Back</button>}
          <button className="primary-button full">{authMode === "signup" ? signupStep < 3 ? "Continue" : "Create account" : authMode === "staff-login" ? "Staff login" : "Login"}<ArrowRight size={18} /></button>
        </form>
        {authMode === "staff-login" ? (
          <button className="ghost-button" onClick={() => setAuthMode("login")}>Return to patient login</button>
        ) : (
          <button className="ghost-button" onClick={() => { setAuthMode(authMode === "login" ? "signup" : "login"); setSignupStep(1); }}>
            {authMode === "login" ? "Need an account? Sign up" : "Already have an account? Login"}
          </button>
        )}
        {authMode === "login" && <button className="auth-secondary-link" onClick={() => setAuthMode("staff-login")}>Staff login</button>}
        <button className="ghost-button" onClick={onBack}>Back</button>
      </section>
    </div>
  );
}
