import React, { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { createRoot } from "react-dom/client";
import axios from "axios";
import {
  Activity,
  AlertTriangle,
  ArrowRight,
  Bell,
  CheckCircle2,
  ChevronDown,
  ClipboardList,
  Database,
  FileText,
  Filter,
  HeartPulse,
  LayoutDashboard,
  LineChart,
  LogIn,
  Menu,
  MessageSquareText,
  PieChart,
  Search,
  Settings2,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  Upload,
  UserRound,
  Users,
  X,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart as ReLineChart,
  Pie,
  PieChart as RePieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import "./styles.css";

type Role = "USER" | "ADMIN";
type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
type Mode = "user" | "admin";
type Page = "overview" | "assessment" | "reports" | "history" | "profile" | "assessments" | "rules" | "questions" | "datasets";
type DesignId = "clinical" | "paper" | "vital";

interface User {
  id: number;
  email: string;
  username: string;
  fullName: string;
  role: Role;
  age?: number;
  gender?: string;
  heightCm?: number;
  weightKg?: number;
  allergies?: string;
  chronicConditions?: string;
  lifestyle?: string;
}

interface Assessment {
  id: number;
  userId: number;
  patient: string;
  mainSymptom: string;
  severity: number;
  durationDays: number;
  temperatureF: number;
  oxygenLevel: number;
  heartRate: number;
  chronicCondition?: string;
  riskScore: number;
  riskLevel: RiskLevel;
  reasons: string[];
  suggestions: string[];
  followUpQuestions: string[];
  createdAt: string;
}

interface Analytics {
  totalUsers: number;
  totalAssessments: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
  commonSymptoms: { symptom: string; count: number }[];
}

interface Question {
  id: number;
  symptomKey: string;
  prompt: string;
  inputType: string;
  active: boolean;
}

interface Rule {
  id: number;
  conditionLabel: string;
  riskLevel: RiskLevel;
  score: number;
  explanation: string;
}

const api = axios.create({ baseURL: "/api" });

const designOptions: { id: DesignId; name: string; note: string }[] = [
  { id: "clinical", name: "Clinical Calm", note: "Clean care surfaces with teal, ink, blue, and coral emphasis." },
  { id: "paper", name: "Paper Console", note: "Editorial paper feel with charcoal text, soft blue, and warm accent." },
  { id: "vital", name: "Vital Signal", note: "Energetic product UI with green, plum, and amber status cues." },
];

const possibleSymptoms = [
  "Abdominal pain", "Acidity", "Allergic reaction", "Anxiety", "Back pain", "Body pain", "Breathing difficulty",
  "Chest pain", "Chills", "Constipation", "Cough", "Dehydration", "Diarrhea", "Dizziness", "Ear pain", "Eye redness",
  "Fatigue", "Fever", "Frequent urination", "Headache", "High blood pressure", "Joint pain", "Loss of appetite",
  "Low blood sugar", "Migraine", "Nausea", "Neck pain", "Palpitations", "Rash", "Runny nose", "Shortness of breath",
  "Sore throat", "Stomach cramps", "Sweating", "Swelling", "Throat pain", "Tooth pain", "Vomiting", "Weakness",
  "Wheezing", "Vision problem",
];

const userNav: [Page, string, typeof LayoutDashboard][] = [
  ["overview", "Overview", LayoutDashboard],
  ["assessment", "Assessment", ClipboardList],
  ["reports", "Reports", FileText],
  ["history", "History", LineChart],
  ["profile", "Profile", UserRound],
];

const adminNav: [Page, string, typeof LayoutDashboard][] = [
  ["overview", "Overview", LayoutDashboard],
  ["assessments", "Assessments", ClipboardList],
  ["rules", "Rules", Settings2],
  ["questions", "Questions", MessageSquareText],
  ["datasets", "Datasets", Database],
];

const emptyAnalytics: Analytics = {
  totalUsers: 0,
  totalAssessments: 0,
  highRiskCount: 0,
  mediumRiskCount: 0,
  lowRiskCount: 0,
  commonSymptoms: [],
};

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function displayRisk(level: RiskLevel) {
  return level.charAt(0) + level.slice(1).toLowerCase();
}

function clampNumber(value: string | number, min: number, max: number, fallback: number) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(max, Math.max(min, parsed));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short" }).format(new Date(value));
}

function StatCard({ icon: Icon, label, value, delta, tone = "neutral" }: {
  icon: typeof Activity;
  label: string;
  value: string | number;
  delta: string;
  tone?: string;
}) {
  return (
    <div className={`stat-card ${tone}`}>
      <div className="stat-head">
        <span className="icon-chip"><Icon size={19} /></span>
        <span>{delta}</span>
      </div>
      <strong>{value}</strong>
      <p>{label}</p>
    </div>
  );
}

function RiskPill({ value }: { value: RiskLevel }) {
  return <span className={`risk-pill ${displayRisk(value).toLowerCase()}`}>{displayRisk(value)}</span>;
}

function LandingPage({ onAuth }: { onAuth: (mode: "login" | "signup") => void }) {
  return (
    <div className="landing-page">
      <header className="landing-nav">
        <div className="brand-row">
          <div className="brand-mark"><HeartPulse size={24} /></div>
          <div><strong>PMS Health</strong><span>Assessment system</span></div>
        </div>
        <div className="hero-actions">
          <button className="ghost-button" onClick={() => onAuth("login")}><LogIn size={18} />Login</button>
          <button className="primary-button" onClick={() => onAuth("signup")}>Sign up<ArrowRight size={18} /></button>
        </div>
      </header>
      <section className="landing-hero">
        <div>
          <p className="eyebrow">Health awareness, not diagnosis</p>
          <h1>Understand symptoms, reports, and risk signals before your doctor visit.</h1>
          <p>
            A capstone-ready patient assessment system with structured health intake,
            rule-based risk scoring, follow-up questions, report summaries, and tracking.
          </p>
          <div className="hero-actions">
            <button className="primary-button" onClick={() => onAuth("signup")}>Create account<ArrowRight size={18} /></button>
            <button className="ghost-button" onClick={() => onAuth("login")}><LogIn size={18} />Login</button>
          </div>
        </div>
        <div className="landing-monitor">
          <div className="vital-card">
            <HeartPulse size={30} />
            <span>Safe prototype</span>
            <strong>AI + Rules</strong>
            <p>Low / Medium / High awareness output with reasons.</p>
          </div>
          <div className="mini-feature-grid">
            {["Dynamic questions", "PDF text summary", "Trend tracking", "Admin rules"].map((item) => (
              <span key={item}><CheckCircle2 size={16} />{item}</span>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

function AuthPage({ initialMode, onSuccess, onBack }: {
  initialMode: "login" | "signup";
  onSuccess: (token: string, user: User) => void;
  onBack: () => void;
}) {
  const [authMode, setAuthMode] = useState(initialMode);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({
    identifier: "user@example.com",
    email: "",
    username: "",
    fullName: "",
    password: "password123",
    role: "USER" as Role,
  });

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage("");
    try {
      const response = authMode === "login"
        ? await api.post("/auth/login", { identifier: form.identifier, password: form.password })
        : await api.post("/auth/register", {
          email: form.email,
          username: form.username,
          fullName: form.fullName,
          password: form.password,
          role: form.role,
        });
      onSuccess(response.data.token, response.data.user);
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Authentication failed." : "Authentication failed.");
    }
  }

  return (
    <div className="auth-page">
      <section className="panel auth-panel">
        <button className="ghost-button" onClick={onBack}>Back</button>
        <div>
          <p className="eyebrow">{authMode === "login" ? "Welcome back" : "Create account"}</p>
          <h2>{authMode === "login" ? "Login to your health workspace" : "Sign up for your own health history"}</h2>
        </div>
        <form className="form-grid auth-form" onSubmit={submit}>
          {authMode === "login" ? (
            <label>Email or username<input value={form.identifier} onChange={(event) => setForm({ ...form, identifier: event.target.value })} required /></label>
          ) : (
            <>
              <label>Email<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required /></label>
              <label>Username<input value={form.username} minLength={3} onChange={(event) => setForm({ ...form, username: event.target.value })} required /></label>
              <label>Full name<input value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required /></label>
              <label>Role<select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value as Role })}><option value="USER">User</option><option value="ADMIN">Admin</option></select></label>
            </>
          )}
          <label>Password<input type="password" minLength={8} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /></label>
          {message && <div className="form-message">{message}</div>}
          <button className="primary-button full">{authMode === "login" ? "Login" : "Create account"}<ArrowRight size={18} /></button>
        </form>
        <button className="ghost-button" onClick={() => setAuthMode(authMode === "login" ? "signup" : "login")}>
          {authMode === "login" ? "Need an account? Sign up" : "Already have an account? Login"}
        </button>
        <div className="disclaimer-box">Demo users: user@example.com / admin@example.com. Password: password123.</div>
      </section>
    </div>
  );
}

function Topbar({ mode, setMode, design, setDesign, onMenu, user, onLogout }: {
  mode: Mode;
  setMode: (mode: Mode) => void;
  design: DesignId;
  setDesign: (design: DesignId) => void;
  onMenu: () => void;
  user: User;
  onLogout: () => void;
}) {
  const canUseAdmin = user.role === "ADMIN";
  return (
    <header className="topbar">
      <button className="icon-button mobile-only" onClick={onMenu} title="Menu"><Menu size={20} /></button>
      <div>
        <p className="eyebrow">Logged in as {user.fullName}</p>
        <h1>{mode === "admin" ? "Admin Web Dashboard" : "User PWA Workspace"}</h1>
      </div>
      <div className="top-actions">
        <div className="segmented">
          <button className={mode === "user" ? "active" : ""} onClick={() => setMode("user")}>User</button>
          <button disabled={!canUseAdmin} className={mode === "admin" ? "active" : ""} onClick={() => canUseAdmin && setMode("admin")}>Admin</button>
        </div>
        <label className="select-shell">
          <Sparkles size={16} />
          <select value={design} onChange={(event) => setDesign(event.target.value as DesignId)}>
            {designOptions.map((option) => <option key={option.id} value={option.id}>{option.name}</option>)}
          </select>
          <ChevronDown size={16} />
        </label>
        <button className="icon-button" title="Notifications"><Bell size={19} /></button>
        <button className="ghost-button" onClick={onLogout}>Logout</button>
      </div>
    </header>
  );
}

function Sidebar({ mode, page, setPage, open, setOpen }: {
  mode: Mode;
  page: Page;
  setPage: (page: Page) => void;
  open: boolean;
  setOpen: (open: boolean) => void;
}) {
  const nav = mode === "admin" ? adminNav : userNav;
  return (
    <aside className={`sidebar ${open ? "open" : ""}`}>
      <div className="brand-row">
        <div className="brand-mark"><HeartPulse size={24} /></div>
        <div><strong>PMS Health</strong><span>Assessment system</span></div>
        <button className="icon-button mobile-only close-menu" onClick={() => setOpen(false)}><X size={18} /></button>
      </div>
      <nav>
        {nav.map(([id, label, Icon]) => (
          <button key={id} className={page === id ? "active" : ""} onClick={() => { setPage(id); setOpen(false); }}>
            <Icon size={18} />{label}
          </button>
        ))}
      </nav>
      <div className="disclaimer-mini">
        <ShieldCheck size={18} />
        <p>Insights only. No diagnosis, prescription, or emergency response.</p>
      </div>
    </aside>
  );
}

function DesignPicker({ design, setDesign }: { design: DesignId; setDesign: (design: DesignId) => void }) {
  return (
    <section className="panel design-picker">
      <div className="section-title"><div><p className="eyebrow">UI options</p><h2>Select a design direction</h2></div></div>
      <div className="design-grid">
        {designOptions.map((option) => (
          <button key={option.id} className={`design-option ${option.id} ${design === option.id ? "active" : ""}`} onClick={() => setDesign(option.id)}>
            <span className="swatches"><i /><i /><i /></span>
            <strong>{option.name}</strong>
            <small>{option.note}</small>
          </button>
        ))}
      </div>
    </section>
  );
}

function trendFromAssessments(assessments: Assessment[]) {
  const latest = [...assessments].slice(0, 7).reverse();
  if (latest.length === 0) return [{ day: "Now", risk: 0, temp: 98.6, heart: 0, sleep: 0 }];
  return latest.map((item) => ({
    day: formatDate(item.createdAt),
    risk: item.riskScore,
    temp: item.temperatureF,
    heart: item.heartRate,
    sleep: 7,
  }));
}

function UserOverview({ user, assessments, setPage, design, setDesign }: {
  user: User;
  assessments: Assessment[];
  setPage: (page: Page) => void;
  design: DesignId;
  setDesign: (design: DesignId) => void;
}) {
  const latest = assessments[0];
  const trendData = trendFromAssessments(assessments);
  return (
    <>
      <div className="page-grid">
        <div className="hero-panel">
          <div>
            <p className="eyebrow">Today</p>
            <h2>{latest ? `Good morning, ${user.fullName}. Your latest assessment is ${displayRisk(latest.riskLevel).toLowerCase()} risk.` : `Welcome, ${user.fullName}. Start your first health assessment.`}</h2>
            <p>{latest ? latest.reasons[0] : "Your assessments, follow-up questions, and trends will appear after you create records."}</p>
            <div className="hero-actions">
              <button className="primary-button" onClick={() => setPage("assessment")}>Start assessment<ArrowRight size={18} /></button>
              <button className="ghost-button" onClick={() => setPage("reports")}><Upload size={18} />Upload report</button>
            </div>
          </div>
          <div className="vital-card">
            <HeartPulse size={28} />
            <span>Latest risk score</span>
            <strong>{latest?.riskScore ?? 0}</strong>
            {latest ? <RiskPill value={latest.riskLevel} /> : <span>No records</span>}
          </div>
        </div>
        <div className="stats-grid">
          <StatCard icon={ClipboardList} label="Assessments" value={assessments.length} delta="your records" />
          <StatCard icon={Activity} label="Latest heart rate" value={latest ? `${latest.heartRate} bpm` : "0 bpm"} delta="from assessment" />
          <StatCard icon={FileText} label="Reports stored" value="0" delta="PDF module" />
          <StatCard icon={ShieldCheck} label="Consent status" value="Active" delta="awareness only" />
        </div>
        <section className="panel wide">
          <div className="section-title"><div><p className="eyebrow">Tracking</p><h2>Vitals and risk trend</h2></div><button className="icon-button" title="Filter"><Filter size={18} /></button></div>
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={trendData}>
              <defs><linearGradient id="riskFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="var(--accent)" stopOpacity={0.28} /><stop offset="95%" stopColor="var(--accent)" stopOpacity={0} /></linearGradient></defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
              <XAxis dataKey="day" tickLine={false} axisLine={false} />
              <YAxis tickLine={false} axisLine={false} />
              <Tooltip />
              <Area type="monotone" dataKey="risk" stroke="var(--accent)" fill="url(#riskFill)" strokeWidth={3} />
              <Line type="monotone" dataKey="heart" stroke="var(--success)" strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        </section>
        <RecentAssessments assessments={assessments} />
      </div>
      <DesignPicker design={design} setDesign={setDesign} />
    </>
  );
}

function SymptomDrawer({ selected, onSelect }: { selected: string; onSelect: (symptom: string) => void }) {
  const [query, setQuery] = useState("");
  const filtered = possibleSymptoms.filter((symptom) => query.length < 1 || symptom.toLowerCase().includes(query.toLowerCase()));
  return (
    <aside className="symptom-drawer">
      <div className="section-title"><div><p className="eyebrow">Symptom drawer</p><h2>Possible symptoms</h2></div></div>
      <div className="search-box drawer-search"><Search size={17} /><input placeholder="Type 1 or 2 letters" value={query} onChange={(event) => setQuery(event.target.value)} /></div>
      <div className="symptom-list">
        {filtered.map((symptom) => (
          <button key={symptom} className={selected === symptom ? "active" : ""} onClick={() => onSelect(symptom)}>{symptom}</button>
        ))}
      </div>
    </aside>
  );
}

function AssessmentForm({ token, onCreated }: { token: string; onCreated: () => Promise<void> }) {
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
    <div className="assessment-workspace">
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

function Reports() {
  return (
    <div className="split-layout">
      <section className="panel upload-panel">
        <div className="upload-zone">
          <Upload size={38} />
          <h2>Upload text-based PDF report</h2>
          <p>PDF upload UI is ready. The Spring Boot endpoint can be expanded for full text extraction in the next step.</p>
          <input type="file" accept="application/pdf" />
        </div>
      </section>
      <section className="panel">
        <p className="eyebrow">Mock AI summary</p>
        <h2>Report simplification</h2>
        <p className="summary-box">Upload support is intentionally safe in this branch. Scanned image PDFs remain out of scope.</p>
        <div className="disclaimer-box">This application does not provide medical diagnosis, treatment, or prescription.</div>
      </section>
    </div>
  );
}

function RecentAssessments({ assessments }: { assessments: Assessment[] }) {
  return (
    <section className="panel">
      <div className="section-title"><div><p className="eyebrow">Records</p><h2>Recent assessments</h2></div></div>
      <div className="table-list">
        {assessments.length === 0 && <div className="empty-row">No assessment records yet.</div>}
        {assessments.map((item) => (
          <div className="table-row" key={item.id}>
            <div><strong>{item.mainSymptom}</strong><span>ASM-{item.id} | {formatDate(item.createdAt)}</span></div>
            <RiskPill value={item.riskLevel} />
          </div>
        ))}
      </div>
    </section>
  );
}

function History({ assessments }: { assessments: Assessment[] }) {
  return (
    <div className="page-grid">
      <section className="panel wide">
        <div className="section-title"><div><p className="eyebrow">History</p><h2>Assessment timeline</h2></div></div>
        <div className="timeline">
          {assessments.length === 0 && <div className="empty-row">No history found for this account.</div>}
          {assessments.map((item) => (
            <div key={item.id} className="timeline-item"><span /><div><strong>{formatDate(item.createdAt)} | {item.mainSymptom}</strong><p>{item.reasons[0]}</p></div><RiskPill value={item.riskLevel} /></div>
          ))}
        </div>
      </section>
      <section className="panel">
        <p className="eyebrow">Risk trend</p>
        <ResponsiveContainer width="100%" height={240}>
          <ReLineChart data={trendFromAssessments(assessments)}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
            <XAxis dataKey="day" tickLine={false} axisLine={false} />
            <YAxis tickLine={false} axisLine={false} />
            <Tooltip />
            <Line type="monotone" dataKey="risk" stroke="var(--accent-2)" strokeWidth={3} />
          </ReLineChart>
        </ResponsiveContainer>
      </section>
    </div>
  );
}

function Profile({ user }: { user: User }) {
  const initials = user.fullName.split(" ").map((word) => word[0]).join("").slice(0, 2).toUpperCase();
  return (
    <section className="panel profile-panel">
      <div className="avatar">{initials}</div>
      <div><p className="eyebrow">Patient profile</p><h2>{user.fullName}</h2><p>Age {user.age ?? "not set"} | {user.gender ?? "not set"} | {user.heightCm ?? 0} cm | {user.weightKg ?? 0} kg</p></div>
      <div className="profile-grid">
        {[user.allergies ?? "No known allergies", user.chronicConditions ?? "No chronic condition", user.lifestyle ?? "Lifestyle not set", user.email, `Username: ${user.username}`, `Role: ${user.role}`].map((item) => <span key={item}>{item}</span>)}
      </div>
    </section>
  );
}

function AdminOverview({ analytics, assessments, setPage }: { analytics: Analytics; assessments: Assessment[]; setPage: (page: Page) => void }) {
  const riskData = [
    { name: "Low", value: analytics.lowRiskCount, color: "#2f9e75" },
    { name: "Medium", value: analytics.mediumRiskCount, color: "#d8902f" },
    { name: "High", value: analytics.highRiskCount, color: "#d94c59" },
  ];
  return (
    <div className="page-grid">
      <div className="stats-grid">
        <StatCard icon={Users} label="Total users" value={analytics.totalUsers} delta="registered" />
        <StatCard icon={ClipboardList} label="Assessments" value={analytics.totalAssessments} delta="database" />
        <StatCard icon={AlertTriangle} label="High risk" value={analytics.highRiskCount} delta="needs review" tone="danger" />
        <StatCard icon={FileText} label="PDF uploads" value="0" delta="next module" />
      </div>
      <section className="panel wide">
        <div className="section-title"><div><p className="eyebrow">Analytics</p><h2>Common symptoms</h2></div><div className="search-box"><Search size={17} /><input placeholder="Filter symptoms" /></div></div>
        <ResponsiveContainer width="100%" height={270}>
          <BarChart data={analytics.commonSymptoms.map((item) => ({ name: item.symptom, count: item.count }))}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
            <XAxis dataKey="name" tickLine={false} axisLine={false} />
            <YAxis tickLine={false} axisLine={false} />
            <Tooltip />
            <Bar dataKey="count" radius={[7, 7, 0, 0]} fill="var(--accent)" />
          </BarChart>
        </ResponsiveContainer>
      </section>
      <section className="panel">
        <div className="section-title"><div><p className="eyebrow">Risk mix</p><h2>Assessment split</h2></div><PieChart size={24} /></div>
        <ResponsiveContainer width="100%" height={240}>
          <RePieChart>
            <Pie data={riskData} innerRadius={62} outerRadius={92} paddingAngle={4} dataKey="value">{riskData.map((entry) => <Cell key={entry.name} fill={entry.color} />)}</Pie>
            <Tooltip />
          </RePieChart>
        </ResponsiveContainer>
        <button className="primary-button full" onClick={() => setPage("assessments")}>Review records<ArrowRight size={18} /></button>
      </section>
      <AssessmentTable assessments={assessments} />
    </div>
  );
}

function AssessmentTable({ assessments }: { assessments: Assessment[] }) {
  return (
    <section className="panel wide">
      <div className="section-title"><div><p className="eyebrow">Monitoring</p><h2>Assessment records</h2></div><button className="ghost-button"><Filter size={17} />Filters</button></div>
      <div className="data-table">
        <div className="data-head"><span>ID</span><span>Patient</span><span>Symptom</span><span>Risk</span><span>Score</span></div>
        {assessments.map((item) => <div className="data-row" key={item.id}><span>ASM-{item.id}</span><strong>{item.patient}</strong><span>{item.mainSymptom}</span><RiskPill value={item.riskLevel} /><span>{item.riskScore}</span></div>)}
      </div>
    </section>
  );
}

function Rules({ rules }: { rules: Rule[] }) {
  return (
    <section className="panel wide">
      <div className="section-title"><div><p className="eyebrow">Admin control</p><h2>Rule engine logic</h2></div><button className="primary-button">Add rule<ArrowRight size={17} /></button></div>
      <div className="rule-grid">{rules.map((rule) => <div className="rule-card" key={rule.id}><span>{rule.conditionLabel}</span><RiskPill value={rule.riskLevel} /><p>{rule.explanation}</p></div>)}</div>
    </section>
  );
}

function Questions({ questions }: { questions: Question[] }) {
  return (
    <section className="panel wide">
      <div className="section-title"><div><p className="eyebrow">Dynamic questioning</p><h2>Question bank</h2></div><button className="primary-button">Create question</button></div>
      <div className="question-list">{questions.map((question, index) => <div key={question.id}><span>Q{index + 1}</span><strong>{question.prompt}</strong><small>{question.active ? "Active" : "Inactive"} | {question.symptomKey}</small></div>)}</div>
    </section>
  );
}

function Datasets() {
  return (
    <section className="panel wide">
      <div className="section-title"><div><p className="eyebrow">Synthetic only</p><h2>Testing dataset plan</h2></div><Database size={24} /></div>
      <div className="dataset-grid">{["Seeded demo users", "Symptom mapping rules", "Validation test cases", "PostgreSQL-backed records"].map((item) => <div key={item}><CheckCircle2 size={20} /><strong>{item}</strong><p>No real patient data is used in this prototype.</p></div>)}</div>
    </section>
  );
}

function MainContent(props: {
  mode: Mode;
  page: Page;
  setPage: (page: Page) => void;
  user: User;
  token: string;
  assessments: Assessment[];
  analytics: Analytics;
  rules: Rule[];
  questions: Question[];
  refresh: () => Promise<void>;
  design: DesignId;
  setDesign: (design: DesignId) => void;
}) {
  if (props.mode === "admin") {
    if (props.page === "assessments") return <AssessmentTable assessments={props.assessments} />;
    if (props.page === "rules") return <Rules rules={props.rules} />;
    if (props.page === "questions") return <Questions questions={props.questions} />;
    if (props.page === "datasets") return <Datasets />;
    return <AdminOverview analytics={props.analytics} assessments={props.assessments} setPage={props.setPage} />;
  }
  if (props.page === "assessment") return <AssessmentForm token={props.token} onCreated={props.refresh} />;
  if (props.page === "reports") return <Reports />;
  if (props.page === "history") return <History assessments={props.assessments} />;
  if (props.page === "profile") return <Profile user={props.user} />;
  return <UserOverview user={props.user} assessments={props.assessments} setPage={props.setPage} design={props.design} setDesign={props.setDesign} />;
}

function App() {
  const [stage, setStage] = useState<"landing" | "auth" | "app">("landing");
  const [authMode, setAuthMode] = useState<"login" | "signup">("login");
  const [token, setToken] = useState(localStorage.getItem("pms-token") ?? "");
  const [user, setUser] = useState<User | null>(null);
  const [mode, setMode] = useState<Mode>("user");
  const [page, setPage] = useState<Page>("overview");
  const [design, setDesign] = useState<DesignId>((localStorage.getItem("pms-design") as DesignId) || "clinical");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [assessments, setAssessments] = useState<Assessment[]>([]);
  const [analytics, setAnalytics] = useState<Analytics>(emptyAnalytics);
  const [rules, setRules] = useState<Rule[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);

  useEffect(() => {
    document.body.dataset.theme = design;
    localStorage.setItem("pms-design", design);
  }, [design]);

  async function refresh(nextToken = token, nextUser = user) {
    if (!nextToken || !nextUser) return;
    const assessmentResponse = await api.get("/assessments", { headers: authHeaders(nextToken) });
    setAssessments(assessmentResponse.data);
    if (nextUser.role === "ADMIN") {
      const [analyticsResponse, rulesResponse, questionsResponse] = await Promise.all([
        api.get("/admin/analytics", { headers: authHeaders(nextToken) }),
        api.get("/admin/rules", { headers: authHeaders(nextToken) }),
        api.get("/admin/questions", { headers: authHeaders(nextToken) }),
      ]);
      setAnalytics(analyticsResponse.data);
      setRules(rulesResponse.data);
      setQuestions(questionsResponse.data);
    }
  }

  async function handleSuccess(nextToken: string, nextUser: User) {
    localStorage.setItem("pms-token", nextToken);
    setToken(nextToken);
    setUser(nextUser);
    setMode(nextUser.role === "ADMIN" ? "admin" : "user");
    setPage("overview");
    setStage("app");
    await refresh(nextToken, nextUser);
  }

  useEffect(() => {
    if (!token) return;
    api.get("/auth/me", { headers: authHeaders(token) })
      .then((response) => handleSuccess(token, response.data))
      .catch(() => localStorage.removeItem("pms-token"));
  }, []);

  function logout() {
    localStorage.removeItem("pms-token");
    setToken("");
    setUser(null);
    setAssessments([]);
    setStage("landing");
  }

  const currentMode = useMemo(() => user?.role === "ADMIN" ? mode : "user", [mode, user]);

  if (stage === "landing") {
    return <LandingPage onAuth={(nextMode) => { setAuthMode(nextMode); setStage("auth"); }} />;
  }
  if (stage === "auth" || !user) {
    return <AuthPage initialMode={authMode} onSuccess={handleSuccess} onBack={() => setStage("landing")} />;
  }

  return (
    <div className="app-shell">
      <Sidebar mode={currentMode} page={page} setPage={setPage} open={sidebarOpen} setOpen={setSidebarOpen} />
      <main>
        <Topbar mode={currentMode} setMode={(nextMode) => { setMode(nextMode); setPage("overview"); }} design={design} setDesign={setDesign} onMenu={() => setSidebarOpen(true)} user={user} onLogout={logout} />
        <MainContent mode={currentMode} page={page} setPage={setPage} user={user} token={token} assessments={assessments} analytics={analytics} rules={rules} questions={questions} refresh={refresh} design={design} setDesign={setDesign} />
      </main>
    </div>
  );
}

createRoot(document.getElementById("app")!).render(<App />);
