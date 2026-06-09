import "@testing-library/jest-dom/vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CarePrepGuide } from "../components/CarePrepGuide";
import { DesignPicker } from "../components/DesignPicker";
import { Sidebar } from "../components/Sidebar";
import { Topbar } from "../components/Topbar";
import { MainContent } from "../MainContent";
import { AdminOverview } from "../pages/admin/AdminOverview";
import { AdminProfile } from "../pages/admin/AdminProfile";
import { AssessmentTable } from "../pages/admin/AssessmentTable";
import { Questions } from "../pages/admin/Questions";
import { Rules } from "../pages/admin/Rules";
import { AuthPage } from "../pages/auth/AuthPage";
import { LandingPage } from "../pages/auth/LandingPage";
import { AssessmentForm } from "../pages/user/AssessmentForm";
import { AssessmentWorkspace } from "../pages/user/AssessmentWorkspace";
import { ConnectedHealth } from "../pages/user/ConnectedHealth";
import { History } from "../pages/user/History";
import { Profile } from "../pages/user/Profile";
import { ProfileSetupPrompt } from "../pages/user/ProfileSetupPrompt";
import { RecentAssessments } from "../pages/user/RecentAssessments";
import { Reports } from "../pages/user/Reports";
import { SymptomDrawer } from "../pages/user/SymptomDrawer";
import { UserOverview } from "../pages/user/UserOverview";
import type { Analytics, Assessment, Question, Rule, User } from "../types";
import { api } from "../api";
import { feetInchesToCm, formatHeight } from "../utils";

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
  profileSetupComplete: true,
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
  status: "COMPLETED",
  reasons: ["Fever lasting several days"],
  suggestions: ["Monitor symptoms"],
  followUpQuestions: ["Any chills?", "Any body pain?", "Any new severe symptom?", "Any chronic conditions?"],
  followUpAnswers: [],
  careSummary: "PMS reviewed fever and weakness as a care-preparation guide.",
  explanation: "The rule engine found moderate severity and fever duration that should be watched.",
  possibleDirections: ["Discuss fever pattern, exposure history, and weakness with a clinician."],
  urgentWarning: null,
  monitoringPlan: ["Track symptoms and temperature twice a day."],
  careTips: ["Bring a symptom timeline to the clinician conversation."],
  doctorPrepQuestions: ["What symptoms should I mention first?"],
  trustedSourceLinks: ["MedlinePlus evaluating health information: https://medlineplus.gov/evaluatinghealthinformation.html"],
  aiMode: "MOCK",
  connectedHealthSummary: "Recent connected health context: Resting heart rate 72 bpm from Apple Health.",
  patientProfilePhotoDataUrl: "data:image/png;base64,ZmFrZQ==",
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
  primarySymptom: "Fever",
  riskLevel: "HIGH",
  score: 30,
  active: true,
  explanation: "Temperature is above the configured review threshold.",
};

const question: Question = {
  id: 1,
  symptomKey: "fever",
  prompt: "Has your fever continued for more than three days?",
  inputType: "choice",
  active: true,
};

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("section rendering", () => {
  it("renders landing section without floating background cards", () => {
    const view = render(<LandingPage onAuth={vi.fn()} />);
    expect(screen.getByText("PMS Health")).toBeInTheDocument();
    expect(screen.getAllByText("Create account").length).toBeGreaterThan(0);
    expect(screen.getByText("Built for awareness, not medical decision-making.")).toBeInTheDocument();
    expect(screen.getByText("Use one private workspace for preparation.")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Privacy and consent" }));
    expect(screen.getByText("Understand how your information supports care preparation.")).toBeInTheDocument();
    expect(view.container.querySelector(".dynamic-ui-background")).not.toBeInTheDocument();
  });

  it("renders patient login with a separate staff login path", () => {
    render(<AuthPage initialMode="login" onSuccess={vi.fn()} onBack={vi.fn()} />);
    expect(screen.getByText("Sign in to your health workspace")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Staff login" }));
    expect(screen.getByText("Sign in to the clinical operations workspace")).toBeInTheDocument();
  });

  it("renders patient-only signup without a role selector", () => {
    render(<AuthPage initialMode="signup" onSuccess={vi.fn()} onBack={vi.fn()} />);
    expect(screen.getByText("Create your private health workspace")).toBeInTheDocument();
    expect(screen.queryByText("Role")).not.toBeInTheDocument();
    expect(screen.getByPlaceholderText("name@example.com")).toHaveValue("");
    expect(screen.getByPlaceholderText("18 or older")).toHaveValue(null);
    fireEvent.click(screen.getByRole("button", { name: "ft + in" }));
    expect(screen.getByPlaceholderText("Feet")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Inches")).toBeInTheDocument();
  });

  it("shows privacy and terms steps before account creation", () => {
    render(<AuthPage initialMode="signup" onSuccess={vi.fn()} onBack={vi.fn()} />);
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "patient@example.com" } });
    fireEvent.change(screen.getByLabelText("Username"), { target: { value: "patientname" } });
    fireEvent.change(screen.getByLabelText("Full name"), { target: { value: "Patient Name" } });
    fireEvent.change(screen.getByLabelText("Age"), { target: { value: "29" } });
    fireEvent.change(screen.getByLabelText("Height cm"), { target: { value: "170" } });
    fireEvent.change(screen.getByLabelText("Weight kg"), { target: { value: "68" } });
    fireEvent.change(screen.getByLabelText("Sex"), { target: { value: "Prefer not to say" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "password123" } });
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(screen.getByText("Review how your information supports care preparation.")).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText("I reviewed and accept the PMS Health privacy notice."));
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    expect(screen.getByText("Accept the terms before creating your account.")).toBeInTheDocument();
  });

  it("renders layout controls", () => {
    render(<Topbar design="clinical" setDesign={vi.fn()} onMenu={vi.fn()} user={admin} onLogout={vi.fn()} />);
    expect(screen.getByText("Clinical operations")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "User" })).not.toBeInTheDocument();
    render(<Sidebar mode="user" page="overview" setPage={vi.fn()} open={false} setOpen={vi.fn()} />);
    expect(screen.getByText("Assessment")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Reports" })).not.toBeInTheDocument();
    render(<Sidebar mode="admin" page="overview" setPage={vi.fn()} open={false} setOpen={vi.fn()} />);
    expect(screen.getAllByText("Profile").length).toBeGreaterThan(0);
    expect(screen.queryByText("Quality review")).not.toBeInTheDocument();
  });

  it("keeps the hidden reports route highlighted as Assessment in the sidebar", () => {
    render(<Sidebar mode="user" page="reports" setPage={vi.fn()} open={false} setOpen={vi.fn()} />);
    expect(screen.getByRole("button", { name: "Assessment" })).toHaveClass("active");
    expect(screen.queryByRole("button", { name: "Reports" })).not.toBeInTheDocument();
  });

  it("renders user overview without the large appearance panel", () => {
    render(<UserOverview user={user} token="token" assessments={[assessment]} setPage={vi.fn()} updateUser={vi.fn()} notify={vi.fn()} />);
    expect(screen.getByText(/latest assessment shows medium awareness/i)).toBeInTheDocument();
    expect(screen.queryByText("Choose your workspace theme")).not.toBeInTheDocument();
  });

  it("renders assessment and symptom drawer sections", () => {
    render(<AssessmentForm token="token" onCreated={vi.fn()} notify={vi.fn()} />);
    expect(screen.getByText("Record what you are feeling")).toBeInTheDocument();
    expect(screen.getByText("Add symptoms")).toBeInTheDocument();
    expect(screen.getByText("Drop symptoms here")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Example: 2")).toHaveValue(null);
    render(<SymptomDrawer selected={["Fever"]} onSelect={vi.fn()} />);
    expect(screen.getAllByText("Fever").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Cardiovascular").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Chest pressure").length).toBeGreaterThan(0);
  });

  it("combines guided and report assessment flows in one workspace", () => {
    vi.spyOn(api, "get").mockRejectedValue(new Error("No draft"));
    render(<AssessmentWorkspace token="token" onCreated={vi.fn().mockResolvedValue(undefined)} notify={vi.fn()} />);

    expect(screen.getByRole("tab", { name: /Guided assessment/i })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByText("Record what you are feeling")).toBeVisible();
    const durationInput = screen.getByPlaceholderText("Example: 2");
    fireEvent.change(durationInput, { target: { value: "3" } });

    fireEvent.click(screen.getByRole("tab", { name: /Report assessment/i }));
    expect(screen.getByRole("tab", { name: /Report assessment/i })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByText("Upload a report")).toBeVisible();

    fireEvent.click(screen.getByRole("tab", { name: /Guided assessment/i }));
    expect(screen.getByPlaceholderText("Example: 2")).toHaveValue(3);
  });

  it("opens report assessment from the reports route alias", () => {
    vi.spyOn(api, "get").mockRejectedValue(new Error("No draft"));
    render(
      <MainContent
        mode="user"
        page="reports"
        setPage={vi.fn()}
        user={user}
        token="token"
        assessments={[]}
        analytics={analytics}
        rules={[rule]}
        questions={[question]}
        refresh={vi.fn().mockResolvedValue(undefined)}
        updateUser={vi.fn()}
        notify={vi.fn()}
      />,
    );
    expect(screen.getByRole("tab", { name: /Report assessment/i })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByText("Upload a report")).toBeVisible();
  });

  it("shows an empty connected-health message from guided assessment", async () => {
    vi.spyOn(api, "get").mockImplementation((url) => {
      if (url === "/health-timeline") return Promise.resolve({ data: [] });
      return Promise.reject(new Error("No draft"));
    });
    render(<AssessmentForm token="token" onCreated={vi.fn()} notify={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: /Use connected health data/i }));
    await waitFor(() => expect(screen.getByText("No connected health records are available yet.")).toBeInTheDocument());
  });

  it("keeps the care guide hidden until every draft follow-up is answered", async () => {
    const draft = { ...assessment, status: "PENDING_FOLLOW_UP" as const, careSummary: null, explanation: null };
    vi.spyOn(api, "get").mockResolvedValue({ data: draft });
    vi.spyOn(api, "post").mockResolvedValue({ data: assessment });
    render(<AssessmentForm token="token" onCreated={vi.fn().mockResolvedValue(undefined)} notify={vi.fn()} />);

    await waitFor(() => expect(screen.getByText("0/4 answered")).toBeInTheDocument());
    expect(screen.queryByText("Summary")).not.toBeInTheDocument();
    for (let index = 0; index < 4; index += 1) {
      await waitFor(() => expect(screen.getByText(`Question ${index + 1} of 4`)).toBeInTheDocument());
      fireEvent.click(screen.getByRole("button", { name: "No" }));
      const action = screen.getByRole("button", { name: index < 3 ? "Next" : "Prepare guide" });
      await waitFor(() => expect(action).toBeEnabled());
      fireEvent.click(action);
    }
    await waitFor(() => expect(screen.getByText("Summary")).toBeInTheDocument());
  }, 10000);

  it("renders reports, history, profile, and recent assessments", () => {
    render(<Reports token="token" notify={vi.fn()} />);
    expect(screen.getByText("Upload a report")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Use connected health data/i })).toBeInTheDocument();
    render(<History assessments={[assessment]} token="token" />);
    expect(screen.getByText("Assessment history")).toBeInTheDocument();
    render(<Profile user={user} token="token" updateUser={vi.fn()} notify={vi.fn()} />);
    expect(screen.getByText("Patient profile")).toBeInTheDocument();
    expect(screen.getByText("Profile photo")).toBeInTheDocument();
    expect(screen.getByText("Allergies")).toBeInTheDocument();
    expect(screen.getByText("Chronic conditions")).toBeInTheDocument();
    render(<RecentAssessments assessments={[assessment]} />);
    expect(screen.getByText("Recent assessments")).toBeInTheDocument();
  });

  it("stages connected-health credentials before saving a connection", async () => {
    vi.spyOn(api, "get").mockResolvedValue({ data: [] });
    const post = vi.spyOn(api, "post").mockImplementation((url) => {
      if (url === "/connections/APPLE_HEALTH/start") {
        return Promise.resolve({ data: { provider: "APPLE_HEALTH", authorizationUrl: "pms-health://apple", permissionSummary: "Patient-approved Apple Health import." } });
      }
      return Promise.resolve({ data: {} });
    });
    render(<ConnectedHealth token="token" notify={vi.fn()} />);
    fireEvent.click(screen.getAllByRole("button", { name: /Review permissions/i })[0]);
    await waitFor(() => expect(screen.getByText("Patient-approved Apple Health import.")).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText("patient@example.com or portal ID"), { target: { value: "patient@example.com" } });
    fireEvent.change(screen.getByPlaceholderText("Dummy credential for connection check"), { target: { value: "password123" } });
    fireEvent.click(screen.getByRole("button", { name: /Connect API link/i }));
    await waitFor(() => expect(screen.getByText("Connection established. Review and save this source.")).toBeInTheDocument(), { timeout: 1500 });
    fireEvent.click(screen.getByRole("button", { name: "Save connection" }));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/connections/APPLE_HEALTH/callback", expect.objectContaining({ externalAccountId: "patient@example.com" }), expect.anything()));
  });

  it("imports Samsung smartwatch sample records during connected-health sync", async () => {
    const notify = vi.fn();
    vi.spyOn(api, "get").mockImplementation((url) => {
      if (url === "/connections") {
        return Promise.resolve({
          data: [{
            id: 12,
            provider: "SAMSUNG_HEALTH",
            displayName: "Samsung Health",
            status: "CONNECTED",
            connectedAt: "2026-06-09T08:00:00",
            lastSyncAt: null,
            permissionSummary: "Samsung permission",
          }],
        });
      }
      if (url === "/health-timeline") return Promise.resolve({ data: [] });
      return Promise.reject(new Error("Unexpected get"));
    });
    const post = vi.spyOn(api, "post").mockResolvedValue({
      data: [
        {
          id: 1,
          sourceType: "DEVICE",
          recordType: "Vital reading",
          label: "Resting heart rate",
          valueText: "96",
          unit: "bpm",
          sourceName: "Samsung Galaxy Watch",
          notes: "Sample value",
          observedAt: "2026-06-09T08:00:00",
        },
      ],
    });

    render(<ConnectedHealth token="token" notify={notify} />);

    await waitFor(() => expect(screen.getByText("Samsung Health")).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: "Sync Samsung Health" }));

    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/connections/12/sync",
      expect.objectContaining({
        records: expect.arrayContaining([
          expect.objectContaining({ label: "Resting heart rate", sourceName: "Samsung Galaxy Watch" }),
        ]),
      }),
      expect.anything(),
    ));
    expect(notify).toHaveBeenCalledWith("1 connected health record imported.");
  });

  it("opens a complete assessment report from history", () => {
    render(<History assessments={[assessment]} token="token" />);
    fireEvent.click(screen.getByRole("button", { name: /15 May \| Fever/i }));
    expect(screen.getByText("ASM-10 care-preparation record")).toBeInTheDocument();
    expect(screen.getByText("Follow-up answers")).toBeInTheDocument();
  });

  it("uses blank dynamic profile questions and preserves reviewed answers", () => {
    const incompleteUser = { ...user, allergies: undefined, chronicConditions: undefined, profileCompletion: 42, profileSetupComplete: false };
    render(<ProfileSetupPrompt user={incompleteUser} token="token" updateUser={vi.fn()} notify={vi.fn()} />);
    const select = screen.getByRole("combobox");
    expect(select).toHaveValue("");
    expect(screen.getByRole("button", { name: "Next question" })).toBeDisabled();
    fireEvent.change(select, { target: { value: "No known allergies" } });
    expect(screen.getByRole("button", { name: "Next question" })).toBeEnabled();
    fireEvent.click(screen.getByRole("button", { name: "Next question" }));
    fireEvent.click(screen.getByRole("button", { name: "Back" }));
    expect(screen.getByRole("combobox")).toHaveValue("No known allergies");
  });

  it("converts and formats patient height in both units", () => {
    expect(feetInchesToCm(5, 4)).toBe(162.6);
    expect(formatHeight(162)).toBe("162 cm (5 ft 4 in)");
  });

  it("renders care-preparation guide sections", () => {
    render(<CarePrepGuide insight={assessment} riskLevel={assessment.riskLevel} riskScore={assessment.riskScore} reasons={assessment.reasons} suggestions={assessment.suggestions} />);
    expect(screen.getByText("Summary")).toBeInTheDocument();
    expect(screen.getByText("PMS reviewed fever and weakness as a care-preparation guide.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /View full care details/i })).toBeInTheDocument();
    expect(screen.queryByText("Why this matters")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /View full care details/i }));
    expect(screen.getByText("Why this matters")).toBeInTheDocument();
    expect(screen.getByText("Possible directions to discuss")).toBeInTheDocument();
    expect(screen.getByText("What to do next")).toBeInTheDocument();
    expect(screen.getByText("Care tips")).toBeInTheDocument();
    expect(screen.getByText("Bring a symptom timeline to the clinician conversation.")).toBeInTheDocument();
    expect(screen.getByText("Doctor questions")).toBeInTheDocument();
    expect(screen.getByText("Personalized guidance")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Hide full care details/i }));
    expect(screen.queryByText("Why this matters")).not.toBeInTheDocument();
  });

  it("keeps urgent warnings visible while care details are collapsed", () => {
    render(<CarePrepGuide insight={{ ...assessment, urgentWarning: "Seek urgent care for breathing difficulty." }} riskLevel="HIGH" riskScore={92} />);
    expect(screen.getByText("Seek urgent care for breathing difficulty.")).toBeInTheDocument();
    expect(screen.queryByText("Why this matters")).not.toBeInTheDocument();
  });

  it("renders drawer-style care guides expanded by default", () => {
    render(<CarePrepGuide insight={assessment} riskLevel={assessment.riskLevel} riskScore={assessment.riskScore} defaultExpanded />);
    expect(screen.getByText("Why this matters")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Hide full care details/i })).toBeInTheDocument();
  });

  it("renders admin overview, management sections, and staff profile", async () => {
    const refresh = vi.fn().mockResolvedValue(undefined);
    vi.spyOn(api, "get").mockResolvedValue({ data: {
      mode: "provider",
      providerChain: ["ollama", "openai"],
      ollamaModel: "gemma4:31b",
      ollamaBaseUrl: "https://ollama.com/api",
      ollamaApiKeyPresent: true,
      openAiModel: "gpt-4o-mini",
      openAiBaseUrl: "https://api.openai.com/v1",
      openAiApiKeyPresent: false,
      lastProviderAttempt: "ollama",
      lastFallbackReason: "No provider call has been attempted.",
    } });
    render(<AdminOverview analytics={analytics} assessments={[assessment]} setPage={vi.fn()} token="token" />);
    expect(screen.getByText("Common symptom patterns")).toBeInTheDocument();
    render(<AssessmentTable assessments={[assessment]} token="token" />);
    expect(screen.getAllByText("Care review").length).toBeGreaterThan(0);
    render(<Rules token="token" rules={[rule]} refresh={refresh} notify={vi.fn()} />);
    expect(screen.getByText("Safety rule review")).toBeInTheDocument();
    render(<Questions token="token" questions={[question]} refresh={refresh} notify={vi.fn()} />);
    expect(screen.getByText("Assessment question bank")).toBeInTheDocument();
    render(<AdminProfile user={admin} token="token" />);
    expect(screen.getByText("Staff profile")).toBeInTheDocument();
    expect(screen.getByText("Backend provider chain")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("gemma4:31b")).toBeInTheDocument());
    expect(screen.queryByText("Quality review")).not.toBeInTheDocument();
  });

  it("submits structured staff safety rules", async () => {
    const post = vi.spyOn(api, "post").mockResolvedValue({ data: rule });
    render(<Rules token="token" rules={[]} refresh={vi.fn().mockResolvedValue(undefined)} notify={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Add rule" }));
    fireEvent.change(screen.getByLabelText("Rule name"), { target: { value: "Persistent fever review" } });
    fireEvent.change(screen.getByLabelText("Primary symptom"), { target: { value: "Fever" } });
    fireEvent.change(screen.getByLabelText("Explanation"), { target: { value: "Persistent fever should be reviewed." } });
    fireEvent.click(screen.getByRole("button", { name: "Save safety rule" }));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/admin/rules", expect.objectContaining({ primarySymptom: "Fever" }), expect.anything()));
  });

  it("submits managed assessment questions", async () => {
    const post = vi.spyOn(api, "post").mockResolvedValue({ data: question });
    render(<Questions token="token" questions={[]} refresh={vi.fn().mockResolvedValue(undefined)} notify={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Create question" }));
    fireEvent.change(screen.getByLabelText("Use for symptom"), { target: { value: "Fever" } });
    fireEvent.change(screen.getByLabelText("Question"), { target: { value: "Has the fever become worse since yesterday?" } });
    fireEvent.click(screen.getByRole("button", { name: "Save assessment question" }));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/admin/questions", expect.objectContaining({ symptomKey: "Fever" }), expect.anything()));
  });

  it("suggests and saves paused AI question drafts", async () => {
    const post = vi.spyOn(api, "post")
      .mockResolvedValueOnce({ data: { symptomKey: "Fever", suggestions: ["Is the fever worse today?", "Did this begin suddenly?"], aiMode: "OLLAMA" } })
      .mockResolvedValue({ data: question });
    render(<Questions token="token" questions={[]} refresh={vi.fn().mockResolvedValue(undefined)} notify={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: /Suggest with AI/i }));
    fireEvent.change(screen.getByLabelText("Use for symptom"), { target: { value: "Fever" } });
    fireEvent.click(screen.getByRole("button", { name: "Generate draft questions" }));
    await waitFor(() => expect(screen.getByText("Is the fever worse today?")).toBeInTheDocument());
    expect(screen.getByText("Draft source: Gemma 4 31B")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Save selected as paused questions" }));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/admin/questions/suggest", expect.objectContaining({ symptomKey: "Fever" }), expect.anything()));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/admin/questions", expect.objectContaining({ prompt: "Is the fever worse today?", active: false }), expect.anything()));
  });

  it("renders design picker directly", () => {
    render(<DesignPicker design="paper" setDesign={vi.fn()} />);
    expect(screen.getByText("Paper Console")).toBeInTheDocument();
  });
});
