import type { Analytics, Assessment, DesignId, Mode, Notify, Page, Question, Rule, User } from "./types";
import { AdminOverview } from "./pages/admin/AdminOverview";
import { AssessmentTable } from "./pages/admin/AssessmentTable";
import { Datasets } from "./pages/admin/Datasets";
import { Questions } from "./pages/admin/Questions";
import { Rules } from "./pages/admin/Rules";
import { AssessmentForm } from "./pages/user/AssessmentForm";
import { History } from "./pages/user/History";
import { Profile } from "./pages/user/Profile";
import { Reports } from "./pages/user/Reports";
import { UserOverview } from "./pages/user/UserOverview";

interface MainContentProps {
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
  updateUser: (user: User) => void;
  notify: Notify;
}

export function MainContent(props: MainContentProps) {
  if (props.mode === "admin") {
    if (props.page === "assessments") return <AssessmentTable assessments={props.assessments} />;
    if (props.page === "rules") return <Rules rules={props.rules} />;
    if (props.page === "questions") return <Questions questions={props.questions} />;
    if (props.page === "datasets") return <Datasets />;
    return <AdminOverview analytics={props.analytics} assessments={props.assessments} setPage={props.setPage} />;
  }
  if (props.page === "assessment") return <AssessmentForm token={props.token} onCreated={props.refresh} notify={props.notify} />;
  if (props.page === "reports") return <Reports notify={props.notify} />;
  if (props.page === "history") return <History assessments={props.assessments} />;
  if (props.page === "profile") return <Profile user={props.user} token={props.token} updateUser={props.updateUser} notify={props.notify} />;
  return <UserOverview user={props.user} token={props.token} assessments={props.assessments} setPage={props.setPage} design={props.design} setDesign={props.setDesign} updateUser={props.updateUser} notify={props.notify} />;
}
