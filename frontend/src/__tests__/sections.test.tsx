import "@testing-library/jest-dom/vitest";
import { cleanup, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CarePrepGuide } from "../components/CarePrepGuide";
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
  age: 24,
  sex: "Female",
  heightCm: 162,
  weightKg: 58,
  profileCompletion: 100,
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
  symptoms: ["Fever"],
  severity: 6,
  durationDays: 4,
  temperatureAvailable: true,
  temperatureF: 100.4,
  chronicCondition: "None",
  riskScore: 52,
  riskLevel: "MEDIUM",
  reasons: ["Fever lasting several days"],
  suggestions: ["Monitor symptoms"],
  followUpQuestions: ["Any chills?", "Any body pain?", "Any new severe symptom?", "Any chronic conditions?"],
  followUpAnswers: [],
  careSummary: "PMS reviewed fever and weakness as a care-preparation guide.",
  explanation: "The rule engine found moderate severity and fever duration that should be watched.",
  possibleDirections: ["Discuss fever pattern, exposure history, and weakness with a clinician."],
  urgentWarning: null,
  monitoringPlan: ["Track symptoms and temperature twice a day."],
  doctorPrepQuestions: ["What symptoms should I mention first?"],
  trustedSourceLinks: ["MedlinePlus evaluating health information: https://medlineplus.gov/evaluatinghealthinformation.html"],
  aiMode: "MOCK",
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
    expect(screen.getAllByText("Create account").length).toBeGreaterThan(0);
    expect(screen.getByText("Built for awareness, not medical decision-making.")).toBeInTheDocument();
    expect(screen.getByText("Questions about the PMS project?")).toBeInTheDocument();
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
    render(<UserOverview user={user} token="token" assessments={[assessment]} setPage={vi.fn()} design="clinical" setDesign={vi.fn()} updateUser={vi.fn()} notify={vi.fn()} />);
    expect(screen.getByText(/latest assessment is medium risk/i)).toBeInTheDocument();
    expect(screen.getByText("Select a design direction")).toBeInTheDocument();
  });

  it("renders assessment and symptom drawer sections", () => {
    render(<AssessmentForm token="token" onCreated={vi.fn()} notify={vi.fn()} />);
    expect(screen.getByText("Health assessment")).toBeInTheDocument();
    expect(screen.getByText("Possible symptoms")).toBeInTheDocument();
    render(<SymptomDrawer selected={["Fever"]} onSelect={vi.fn()} />);
    expect(screen.getAllByText("Fever").length).toBeGreaterThan(0);
  });

  it("renders reports, history, profile, and recent assessments", () => {
    render(<Reports token="token" notify={vi.fn()} />);
    expect(screen.getByText("Upload text-based PDF report")).toBeInTheDocument();
    render(<History assessments={[assessment]} />);
    expect(screen.getByText("Assessment timeline")).toBeInTheDocument();
    render(<Profile user={user} token="token" updateUser={vi.fn()} notify={vi.fn()} />);
    expect(screen.getByText("Patient profile")).toBeInTheDocument();
    render(<RecentAssessments assessments={[assessment]} />);
    expect(screen.getByText("Recent assessments")).toBeInTheDocument();
  });

  it("renders care-preparation guide sections", () => {
    render(<CarePrepGuide insight={assessment} riskLevel={assessment.riskLevel} riskScore={assessment.riskScore} reasons={assessment.reasons} suggestions={assessment.suggestions} />);
    expect(screen.getByText("Summary")).toBeInTheDocument();
    expect(screen.getByText("Why this matters")).toBeInTheDocument();
    expect(screen.getByText("Possible directions to discuss")).toBeInTheDocument();
    expect(screen.getByText("What to do next")).toBeInTheDocument();
    expect(screen.getByText("Doctor questions")).toBeInTheDocument();
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
