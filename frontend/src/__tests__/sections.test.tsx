import "@testing-library/jest-dom/vitest";
import { cleanup, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { DesignPicker } from "../components/DesignPicker";
import { Sidebar } from "../components/Sidebar";
import { Topbar } from "../components/Topbar";
import { AdminOverview } from "../pages/admin/AdminOverview";
import { AssessmentTable } from "../pages/admin/AssessmentTable";
import { Datasets } from "../pages/admin/Datasets";
import { Questions } from "../pages/admin/Questions";
import { Rules } from "../pages/admin/Rules";
import { AuthPage } from "../pages/auth/AuthPage";
import { LandingPage } from "../pages/auth/LandingPage";
import { AssessmentForm } from "../pages/user/AssessmentForm";
import { History } from "../pages/user/History";
import { Profile } from "../pages/user/Profile";
import { RecentAssessments } from "../pages/user/RecentAssessments";
import { Reports } from "../pages/user/Reports";
import { SymptomDrawer } from "../pages/user/SymptomDrawer";
import { UserOverview } from "../pages/user/UserOverview";
import type { Analytics, Assessment, Question, Rule, User } from "../types";

vi.mock("recharts", () => {
  const Chart = ({ children }: { children?: ReactNode }) => <div data-testid="chart">{children}</div>;
  const Primitive = ({ children }: { children?: ReactNode }) => <div>{children}</div>;
  return {
    Area: Primitive,
    AreaChart: Chart,
    Bar: Primitive,
    BarChart: Chart,
    CartesianGrid: Primitive,
    Cell: Primitive,
    Line: Primitive,
    LineChart: Chart,
    Pie: Primitive,
    PieChart: Chart,
    ResponsiveContainer: Chart,
    Tooltip: Primitive,
    XAxis: Primitive,
    YAxis: Primitive,
  };
});

const user: User = {
  id: 1,
  email: "user@example.com",
  username: "anaya",
  fullName: "Anaya Rao",
  role: "USER",
};

const admin: User = {
  ...user,
  id: 2,
  email: "admin@example.com",
  username: "admin",
  fullName: "Admin User",
  role: "ADMIN",
};

const assessment: Assessment = {
  id: 10,
  userId: 1,
  patient: "Anaya Rao",
  mainSymptom: "Fever",
  severity: 6,
  durationDays: 4,
  temperatureF: 100.4,
  oxygenLevel: 97,
  heartRate: 88,
  chronicCondition: "None",
  riskScore: 52,
  riskLevel: "MEDIUM",
  reasons: ["Fever lasting several days"],
  suggestions: ["Monitor symptoms"],
  followUpQuestions: ["Any chills?"],
  createdAt: "2026-05-15T10:00:00",
};

const analytics: Analytics = {
  totalUsers: 3,
  totalAssessments: 4,
  highRiskCount: 1,
  mediumRiskCount: 2,
  lowRiskCount: 1,
  commonSymptoms: [{ symptom: "Fever", count: 2 }],
};

const rule: Rule = {
  id: 1,
  conditionLabel: "High fever",
  riskLevel: "HIGH",
  score: 30,
  explanation: "Temperature is above safe demo threshold.",
};

const question: Question = {
  id: 1,
  symptomKey: "fever",
  prompt: "How long have you had fever?",
  inputType: "text",
  active: true,
};

afterEach(() => {
  cleanup();
});

describe("section rendering", () => {
  it("renders landing section", () => {
    render(<LandingPage onAuth={vi.fn()} />);
    expect(screen.getByText("PMS Health")).toBeInTheDocument();
    expect(screen.getByText("Create account")).toBeInTheDocument();
  });

  it("renders auth section", () => {
    render(<AuthPage initialMode="login" onSuccess={vi.fn()} onBack={vi.fn()} />);
    expect(screen.getByText("Login to your health workspace")).toBeInTheDocument();
  });

  it("renders layout controls", () => {
    render(<Topbar mode="admin" setMode={vi.fn()} design="clinical" setDesign={vi.fn()} onMenu={vi.fn()} user={admin} onLogout={vi.fn()} />);
    expect(screen.getByText("Admin Web Dashboard")).toBeInTheDocument();
    render(<Sidebar mode="user" page="overview" setPage={vi.fn()} open={false} setOpen={vi.fn()} />);
    expect(screen.getByText("Assessment")).toBeInTheDocument();
  });

  it("renders user overview and design picker", () => {
    render(<UserOverview user={user} assessments={[assessment]} setPage={vi.fn()} design="clinical" setDesign={vi.fn()} />);
    expect(screen.getByText(/latest assessment is medium risk/i)).toBeInTheDocument();
    expect(screen.getByText("Select a design direction")).toBeInTheDocument();
  });

  it("renders assessment and symptom drawer sections", () => {
    render(<AssessmentForm token="token" onCreated={vi.fn()} />);
    expect(screen.getByText("Health assessment")).toBeInTheDocument();
    expect(screen.getByText("Possible symptoms")).toBeInTheDocument();
    render(<SymptomDrawer selected="Fever" onSelect={vi.fn()} />);
    expect(screen.getAllByText("Fever").length).toBeGreaterThan(0);
  });

  it("renders reports, history, profile, and recent assessments", () => {
    render(<Reports />);
    expect(screen.getByText("Upload text-based PDF report")).toBeInTheDocument();
    render(<History assessments={[assessment]} />);
    expect(screen.getByText("Assessment timeline")).toBeInTheDocument();
    render(<Profile user={user} />);
    expect(screen.getByText("Patient profile")).toBeInTheDocument();
    render(<RecentAssessments assessments={[assessment]} />);
    expect(screen.getByText("Recent assessments")).toBeInTheDocument();
  });

  it("renders admin overview and admin sections", () => {
    render(<AdminOverview analytics={analytics} assessments={[assessment]} setPage={vi.fn()} />);
    expect(screen.getByText("Common symptoms")).toBeInTheDocument();
    render(<AssessmentTable assessments={[assessment]} />);
    expect(screen.getAllByText("Monitoring").length).toBeGreaterThan(0);
    render(<Rules rules={[rule]} />);
    expect(screen.getByText("Rule engine logic")).toBeInTheDocument();
    render(<Questions questions={[question]} />);
    expect(screen.getByText("Question bank")).toBeInTheDocument();
    render(<Datasets />);
    expect(screen.getByText("Testing dataset plan")).toBeInTheDocument();
  });

  it("renders design picker directly", () => {
    render(<DesignPicker design="paper" setDesign={vi.fn()} />);
    expect(screen.getByText("Paper Console")).toBeInTheDocument();
  });
});
