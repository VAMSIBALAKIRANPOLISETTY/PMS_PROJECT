import type { Analytics, Assessment, Mode, Notify, Page, Question, Rule, User } from "./types";
import { AdminOverview } from "./pages/admin/AdminOverview";
import { AdminProfile } from "./pages/admin/AdminProfile";
import { AssessmentTable } from "./pages/admin/AssessmentTable";
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
  updateUser: (user: User) => void;
  notify: Notify;
}

export function MainContent(props: MainContentProps) {
  if (props.mode === "admin") {
    if (props.page === "assessments") return <AssessmentTable assessments={props.assessments} />;
    if (props.page === "rules") return <Rules token={props.token} rules={props.rules} refresh={props.refresh} notify={props.notify} />;
    if (props.page === "questions") return <Questions token={props.token} questions={props.questions} refresh={props.refresh} notify={props.notify} />;
    if (props.page === "profile") return <AdminProfile user={props.user} />;
    return <AdminOverview analytics={props.analytics} assessments={props.assessments} setPage={props.setPage} />;
  }
  if (props.page === "assessment") return <AssessmentForm token={props.token} onCreated={props.refresh} notify={props.notify} />;
  if (props.page === "reports") return <Reports token={props.token} notify={props.notify} />;
  if (props.page === "history") return <History assessments={props.assessments} />;
  if (props.page === "profile") return <Profile user={props.user} token={props.token} updateUser={props.updateUser} notify={props.notify} />;
  return <UserOverview user={props.user} token={props.token} assessments={props.assessments} setPage={props.setPage} updateUser={props.updateUser} notify={props.notify} />;
}
