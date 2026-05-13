import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
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

const designOptions = [
  { id: "clinical", name: "Clinical Calm", note: "Clean care surfaces with teal, ink, blue, and coral emphasis." },
  { id: "paper", name: "Paper Console", note: "Editorial paper feel with charcoal text, soft blue, and warm accent." },
  { id: "vital", name: "Vital Signal", note: "Energetic product UI with green, plum, and amber status cues." },
];

const userNav = [
  ["overview", "Overview", LayoutDashboard],
  ["assessment", "Assessment", ClipboardList],
  ["reports", "Reports", FileText],
  ["history", "History", LineChart],
  ["profile", "Profile", UserRound],
];

const adminNav = [
  ["overview", "Overview", LayoutDashboard],
  ["assessments", "Assessments", ClipboardList],
  ["rules", "Rules", Settings2],
  ["questions", "Questions", MessageSquareText],
  ["datasets", "Datasets", Database],
];

const trendData = [
  { day: "Mon", risk: 28, temp: 98.2, sleep: 7.2, heart: 76 },
  { day: "Tue", risk: 34, temp: 99.1, sleep: 6.8, heart: 82 },
  { day: "Wed", risk: 31, temp: 98.8, sleep: 7.1, heart: 78 },
  { day: "Thu", risk: 48, temp: 100.4, sleep: 5.9, heart: 88 },
  { day: "Fri", risk: 42, temp: 99.7, sleep: 6.4, heart: 84 },
  { day: "Sat", risk: 24, temp: 98.4, sleep: 7.8, heart: 72 },
  { day: "Sun", risk: 20, temp: 98.1, sleep: 8.0, heart: 70 },
];

const symptomData = [
  { name: "Fever", count: 42 },
  { name: "Cough", count: 38 },
  { name: "Fatigue", count: 31 },
  { name: "Headache", count: 27 },
  { name: "Dizziness", count: 18 },
];

const riskData = [
  { name: "Low", value: 61, color: "#2f9e75" },
  { name: "Medium", value: 28, color: "#d8902f" },
  { name: "High", value: 11, color: "#d94c59" },
];

const assessments = [
  { id: "ASM-2041", patient: "Anaya Rao", symptom: "Fever and weakness", risk: "Medium", date: "13 May", score: 48, status: "Follow-up asked" },
  { id: "ASM-2038", patient: "Rohan Mehta", symptom: "Chest pain", risk: "High", date: "12 May", score: 86, status: "Doctor advice shown" },
  { id: "ASM-2032", patient: "Maya Iyer", symptom: "Mild headache", risk: "Low", date: "11 May", score: 21, status: "Saved to history" },
  { id: "ASM-2026", patient: "Kabir Shah", symptom: "Cough", risk: "Medium", date: "10 May", score: 54, status: "Report uploaded" },
];

const questions = [
  "How long have you had this symptom?",
  "Do you have breathing difficulty?",
  "Is the pain sudden or gradual?",
  "Do you have fever or chills?",
  "Any known chronic condition?",
];

const rules = [
  ["Chest pain + breathing difficulty", "High", "Red flag combination"],
  ["Fever more than 3 days + weakness", "Medium", "Duration and fatigue"],
  ["Mild headache + no red flags", "Low", "No severe indicator"],
  ["Diabetic user + dizziness", "Medium", "Ask glucose follow-up questions"],
];

function StatCard({ icon: Icon, label, value, delta, tone = "neutral" }) {
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

function RiskPill({ value }) {
  return <span className={`risk-pill ${value.toLowerCase()}`}>{value}</span>;
}

function Topbar({ mode, setMode, design, setDesign, onMenu }) {
  return (
    <header className="topbar">
      <button className="icon-button mobile-only" onClick={onMenu} title="Menu"><Menu size={20} /></button>
      <div>
        <p className="eyebrow">Hybrid AI health awareness prototype</p>
        <h1>{mode === "admin" ? "Admin Web Dashboard" : "User PWA Workspace"}</h1>
      </div>
      <div className="top-actions">
        <div className="segmented">
          <button className={mode === "user" ? "active" : ""} onClick={() => setMode("user")}>User</button>
          <button className={mode === "admin" ? "active" : ""} onClick={() => setMode("admin")}>Admin</button>
        </div>
        <label className="select-shell">
          <Sparkles size={16} />
          <select value={design} onChange={(event) => setDesign(event.target.value)}>
            {designOptions.map((option) => <option key={option.id} value={option.id}>{option.name}</option>)}
          </select>
          <ChevronDown size={16} />
        </label>
        <button className="icon-button" title="Notifications"><Bell size={19} /></button>
      </div>
    </header>
  );
}

function Sidebar({ mode, page, setPage, open, setOpen }) {
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

function DesignPicker({ design, setDesign }) {
  return (
    <section className="panel design-picker">
      <div className="section-title">
        <div><p className="eyebrow">UI options</p><h2>Select a design direction</h2></div>
      </div>
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

function LoginStrip({ mode }) {
  return (
    <section className="login-strip">
      <div>
        <p className="eyebrow">Demo accounts</p>
        <strong>{mode === "admin" ? "admin@example.com" : "user@example.com"}</strong>
        <span>Password: password123</span>
      </div>
      <button className="primary-button"><LogIn size={18} />Demo login</button>
    </section>
  );
}

function UserOverview({ setPage }) {
  return (
    <div className="page-grid">
      <div className="hero-panel">
        <div>
          <p className="eyebrow">Today</p>
          <h2>Good morning, Anaya. Your latest assessment is medium risk.</h2>
          <p>Fever duration and weakness increased the score. The system saved a follow-up question set and suggests medical consultation if symptoms persist.</p>
          <div className="hero-actions">
            <button className="primary-button" onClick={() => setPage("assessment")}>Start assessment<ArrowRight size={18} /></button>
            <button className="ghost-button" onClick={() => setPage("reports")}><Upload size={18} />Upload report</button>
          </div>
        </div>
        <div className="vital-card">
          <HeartPulse size={28} />
          <span>Risk score</span>
          <strong>48</strong>
          <RiskPill value="Medium" />
        </div>
      </div>
      <div className="stats-grid">
        <StatCard icon={ClipboardList} label="Assessments" value="12" delta="+2 this month" />
        <StatCard icon={Activity} label="Avg heart rate" value="78 bpm" delta="stable" />
        <StatCard icon={FileText} label="Reports stored" value="4" delta="1 new" />
        <StatCard icon={ShieldCheck} label="Consent status" value="Active" delta="accepted" />
      </div>
      <section className="panel wide">
        <div className="section-title">
          <div><p className="eyebrow">Tracking</p><h2>Vitals and risk trend</h2></div>
          <button className="icon-button" title="Filter"><Filter size={18} /></button>
        </div>
        <ResponsiveContainer width="100%" height={260}>
          <AreaChart data={trendData}>
            <defs>
              <linearGradient id="riskFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="var(--accent)" stopOpacity={0.28} />
                <stop offset="95%" stopColor="var(--accent)" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
            <XAxis dataKey="day" tickLine={false} axisLine={false} />
            <YAxis tickLine={false} axisLine={false} />
            <Tooltip />
            <Area type="monotone" dataKey="risk" stroke="var(--accent)" fill="url(#riskFill)" strokeWidth={3} />
            <Line type="monotone" dataKey="heart" stroke="var(--success)" strokeWidth={2} />
          </AreaChart>
        </ResponsiveContainer>
      </section>
      <RecentAssessments />
    </div>
  );
}

function AssessmentForm() {
  const [result, setResult] = useState(null);
  const [form, setForm] = useState({ symptom: "Fever and weakness", severity: 6, duration: "4 days", temperature: "100.4", oxygen: "97", chronic: "None" });
  const update = (field, value) => setForm((current) => ({ ...current, [field]: value }));
  const runAssessment = (event) => {
    event.preventDefault();
    const temp = Number(form.temperature);
    const score = temp >= 103 ? 82 : Number(form.severity) >= 7 ? 64 : form.duration.includes("4") ? 48 : 26;
    setResult({ risk: score >= 70 ? "High" : score >= 40 ? "Medium" : "Low", score, reasons: ["Symptom severity", "Duration", "Temperature value", "Red-flag screening"] });
  };

  return (
    <div className="split-layout">
      <section className="panel">
        <div className="section-title"><div><p className="eyebrow">User flow</p><h2>Health assessment</h2></div><Stethoscope size={24} /></div>
        <form className="form-grid" onSubmit={runAssessment}>
          <label>Main symptom<input value={form.symptom} onChange={(event) => update("symptom", event.target.value)} /></label>
          <label>Severity<input type="range" min="1" max="10" value={form.severity} onChange={(event) => update("severity", event.target.value)} /><span className="range-value">{form.severity}/10</span></label>
          <label>Duration<input value={form.duration} onChange={(event) => update("duration", event.target.value)} /></label>
          <label>Temperature<input value={form.temperature} onChange={(event) => update("temperature", event.target.value)} /></label>
          <label>Oxygen level<input value={form.oxygen} onChange={(event) => update("oxygen", event.target.value)} /></label>
          <label>Chronic condition<select value={form.chronic} onChange={(event) => update("chronic", event.target.value)}><option>None</option><option>Diabetes</option><option>Blood pressure</option><option>Asthma</option><option>Heart disease</option></select></label>
          <button className="primary-button full">Generate safe insight<ArrowRight size={18} /></button>
        </form>
      </section>
      <section className="panel result-panel">
        <p className="eyebrow">Final output preview</p>
        {result ? (
          <>
            <div className="score-ring"><strong>{result.score}</strong><span>Risk score</span></div>
            <RiskPill value={result.risk} />
            <h2>{result.risk} risk health awareness result</h2>
            <p>This is a non-diagnostic explanation based on entered data. Consult a qualified medical professional for diagnosis, treatment, or urgent symptoms.</p>
            <ul className="reason-list">{result.reasons.map((reason) => <li key={reason}><CheckCircle2 size={17} />{reason}</li>)}</ul>
          </>
        ) : (
          <div className="empty-state"><Sparkles size={32} /><h2>Result appears here</h2><p>Submit the form to preview risk level, reasons, suggestions, and disclaimer.</p></div>
        )}
      </section>
    </div>
  );
}

function Reports() {
  const [summary, setSummary] = useState("");
  return (
    <div className="split-layout">
      <section className="panel upload-panel">
        <div className="upload-zone">
          <Upload size={38} />
          <h2>Upload text-based PDF report</h2>
          <p>First version extracts readable text and creates a simple mock summary.</p>
          <input type="file" accept="application/pdf" onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) setSummary(`${file.name}: readable report text detected. Mock summary highlights CBC, sugar, and lipid references when present. No diagnosis is generated.`);
          }} />
        </div>
      </section>
      <section className="panel">
        <p className="eyebrow">Mock AI summary</p>
        <h2>Report simplification</h2>
        <p className="summary-box">{summary || "Upload a PDF to preview the raw text summary output. Scanned image PDFs are out of scope for version one."}</p>
        <div className="disclaimer-box">This application does not provide medical diagnosis, treatment, or prescription.</div>
      </section>
    </div>
  );
}

function RecentAssessments() {
  return (
    <section className="panel">
      <div className="section-title"><div><p className="eyebrow">Records</p><h2>Recent assessments</h2></div><button className="ghost-button">View all<ArrowRight size={16} /></button></div>
      <div className="table-list">
        {assessments.map((item) => (
          <div className="table-row" key={item.id}>
            <div><strong>{item.symptom}</strong><span>{item.id} | {item.date}</span></div>
            <RiskPill value={item.risk} />
          </div>
        ))}
      </div>
    </section>
  );
}

function History() {
  return (
    <div className="page-grid">
      <section className="panel wide">
        <div className="section-title"><div><p className="eyebrow">History</p><h2>Assessment timeline</h2></div></div>
        <div className="timeline">
          {assessments.map((item) => (
            <div key={item.id} className="timeline-item"><span /><div><strong>{item.date} | {item.symptom}</strong><p>{item.status}</p></div><RiskPill value={item.risk} /></div>
          ))}
        </div>
      </section>
      <section className="panel">
        <p className="eyebrow">Sleep trend</p>
        <ResponsiveContainer width="100%" height={240}>
          <ReLineChart data={trendData}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--line)" />
            <XAxis dataKey="day" tickLine={false} axisLine={false} />
            <YAxis tickLine={false} axisLine={false} />
            <Tooltip />
            <Line type="monotone" dataKey="sleep" stroke="var(--accent-2)" strokeWidth={3} />
          </ReLineChart>
        </ResponsiveContainer>
      </section>
    </div>
  );
}

function Profile() {
  return (
    <section className="panel profile-panel">
      <div className="avatar">AR</div>
      <div><p className="eyebrow">Patient profile</p><h2>Anaya Rao</h2><p>Age 24 | Female | 162 cm | 58 kg</p></div>
      <div className="profile-grid">
        {["No known allergies", "No chronic condition", "7h average sleep", "Moderate activity", "No smoking", "Occasional caffeine"].map((item) => <span key={item}>{item}</span>)}
      </div>
    </section>
  );
}

function AdminOverview({ setPage }) {
  return (
    <div className="page-grid">
      <LoginStrip mode="admin" />
      <div className="stats-grid">
        <StatCard icon={Users} label="Total users" value="248" delta="+18%" />
        <StatCard icon={ClipboardList} label="Assessments" value="1,426" delta="+11%" />
        <StatCard icon={AlertTriangle} label="High risk" value="38" delta="needs review" tone="danger" />
        <StatCard icon={FileText} label="PDF uploads" value="312" delta="+24%" />
      </div>
      <section className="panel wide">
        <div className="section-title">
          <div><p className="eyebrow">Analytics</p><h2>Common symptoms</h2></div>
          <div className="search-box"><Search size={17} /><input placeholder="Filter symptoms" /></div>
        </div>
        <ResponsiveContainer width="100%" height={270}>
          <BarChart data={symptomData}>
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
            <Pie data={riskData} innerRadius={62} outerRadius={92} paddingAngle={4} dataKey="value">
              {riskData.map((entry) => <Cell key={entry.name} fill={entry.color} />)}
            </Pie>
            <Tooltip />
          </RePieChart>
        </ResponsiveContainer>
        <button className="primary-button full" onClick={() => setPage("assessments")}>Review records<ArrowRight size={18} /></button>
      </section>
      <AssessmentTable />
    </div>
  );
}

function AssessmentTable() {
  return (
    <section className="panel wide">
      <div className="section-title"><div><p className="eyebrow">Monitoring</p><h2>Assessment records</h2></div><button className="ghost-button"><Filter size={17} />Filters</button></div>
      <div className="data-table">
        <div className="data-head"><span>ID</span><span>Patient</span><span>Symptom</span><span>Risk</span><span>Score</span></div>
        {assessments.map((item) => (
          <div className="data-row" key={item.id}><span>{item.id}</span><strong>{item.patient}</strong><span>{item.symptom}</span><RiskPill value={item.risk} /><span>{item.score}</span></div>
        ))}
      </div>
    </section>
  );
}

function Rules() {
  return (
    <section className="panel wide">
      <div className="section-title"><div><p className="eyebrow">Admin control</p><h2>Rule engine logic</h2></div><button className="primary-button">Add rule<ArrowRight size={17} /></button></div>
      <div className="rule-grid">
        {rules.map(([condition, risk, reason]) => <div className="rule-card" key={condition}><span>{condition}</span><RiskPill value={risk} /><p>{reason}</p></div>)}
      </div>
    </section>
  );
}

function Questions() {
  return (
    <section className="panel wide">
      <div className="section-title"><div><p className="eyebrow">Dynamic questioning</p><h2>Question bank</h2></div><button className="primary-button">Create question</button></div>
      <div className="question-list">
        {questions.map((question, index) => <div key={question}><span>Q{index + 1}</span><strong>{question}</strong><small>Active | symptom-triggered</small></div>)}
      </div>
    </section>
  );
}

function Datasets() {
  return (
    <section className="panel wide">
      <div className="section-title"><div><p className="eyebrow">Synthetic only</p><h2>Testing dataset plan</h2></div><Database size={24} /></div>
      <div className="dataset-grid">
        {["Synthetic patient records", "Symptom mapping test cases", "Text-based sample PDFs", "Synthea import placeholder"].map((item) => <div key={item}><CheckCircle2 size={20} /><strong>{item}</strong><p>No real patient data is used in this prototype.</p></div>)}
      </div>
    </section>
  );
}

function MainContent({ mode, page, setPage, design, setDesign }) {
  if (mode === "admin") {
    if (page === "assessments") return <AssessmentTable />;
    if (page === "rules") return <Rules />;
    if (page === "questions") return <Questions />;
    if (page === "datasets") return <Datasets />;
    return <AdminOverview setPage={setPage} />;
  }
  if (page === "assessment") return <AssessmentForm />;
  if (page === "reports") return <Reports />;
  if (page === "history") return <History />;
  if (page === "profile") return <Profile />;
  return <><LoginStrip mode="user" /><UserOverview setPage={setPage} /><DesignPicker design={design} setDesign={setDesign} /></>;
}

function App() {
  const [mode, setMode] = useState("user");
  const [page, setPage] = useState("overview");
  const [design, setDesign] = useState(localStorage.getItem("pms-design") || "clinical");
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    document.body.dataset.theme = design;
    localStorage.setItem("pms-design", design);
  }, [design]);

  function switchMode(nextMode) {
    setMode(nextMode);
    setPage("overview");
  }

  return (
    <div className="app-shell">
      <Sidebar mode={mode} page={page} setPage={setPage} open={sidebarOpen} setOpen={setSidebarOpen} />
      <main>
        <Topbar mode={mode} setMode={switchMode} design={design} setDesign={setDesign} onMenu={() => setSidebarOpen(true)} />
        <MainContent mode={mode} page={page} setPage={setPage} design={design} setDesign={setDesign} />
      </main>
    </div>
  );
}

createRoot(document.getElementById("app")).render(<App />);
