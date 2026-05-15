import { useEffect, useMemo, useState } from "react";
import { api, authHeaders } from "./api";
import { Sidebar } from "./components/Sidebar";
import { Topbar } from "./components/Topbar";
import { emptyAnalytics } from "./data";
import { MainContent } from "./MainContent";
import { AuthPage } from "./pages/auth/AuthPage";
import { LandingPage } from "./pages/auth/LandingPage";
import type { Analytics, Assessment, DesignId, Mode, Page, Question, Rule, User } from "./types";

export function App() {
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
