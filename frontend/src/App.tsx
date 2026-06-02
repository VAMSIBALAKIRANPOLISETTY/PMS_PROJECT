import { useEffect, useState } from "react";
import { api, authHeaders } from "./api";
import { Sidebar } from "./components/Sidebar";
import { Topbar } from "./components/Topbar";
import { emptyAnalytics } from "./data";
import { MainContent } from "./MainContent";
import { AuthPage } from "./pages/auth/AuthPage";
import type { AuthMode } from "./pages/auth/AuthPage";
import { LandingPage } from "./pages/auth/LandingPage";
import type { Analytics, Assessment, DesignId, Mode, Notify, Page, Question, Rule, User } from "./types";

export function App() {
  const [stage, setStage] = useState<"landing" | "auth" | "app">("landing");
  const [authMode, setAuthMode] = useState<AuthMode>("login");
  const [token, setToken] = useState(localStorage.getItem("pms-token") ?? "");
  const [user, setUser] = useState<User | null>(null);
  const [page, setPage] = useState<Page>("overview");
  const [design, setDesign] = useState<DesignId>((localStorage.getItem("pms-design") as DesignId) || "clinical");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [assessments, setAssessments] = useState<Assessment[]>([]);
  const [analytics, setAnalytics] = useState<Analytics>(emptyAnalytics);
  const [rules, setRules] = useState<Rule[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [toast, setToast] = useState<{ message: string; tone: "success" | "warning" | "danger" } | null>(null);

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
    setPage("overview");
    setStage("app");
    await refresh(nextToken, nextUser);
  }

  const notify: Notify = (message, tone = "success") => {
    setToast({ message, tone });
    window.setTimeout(() => setToast(null), 4200);
  };

  function updateUser(nextUser: User) {
    setUser(nextUser);
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

  const currentMode: Mode = user?.role === "ADMIN" ? "admin" : "user";

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
        <Topbar design={design} setDesign={setDesign} onMenu={() => setSidebarOpen(true)} user={user} onLogout={logout} />
        <MainContent mode={currentMode} page={page} setPage={setPage} user={user} token={token} assessments={assessments} analytics={analytics} rules={rules} questions={questions} refresh={refresh} updateUser={updateUser} notify={notify} />
      </main>
      {toast && <div className={`toast-notification ${toast.tone}`} role="status">{toast.message}</div>}
    </div>
  );
}
