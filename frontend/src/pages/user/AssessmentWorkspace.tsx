import { useEffect, useState } from "react";
import { FileText, Stethoscope } from "lucide-react";
import type { Notify } from "../../types";
import { AssessmentForm } from "./AssessmentForm";
import { Reports } from "./Reports";

type AssessmentMode = "guided" | "report";

interface AssessmentWorkspaceProps {
  token: string;
  notify: Notify;
  onCreated: () => Promise<void>;
  initialMode?: AssessmentMode;
}

export function AssessmentWorkspace({ token, notify, onCreated, initialMode = "guided" }: AssessmentWorkspaceProps) {
  const [activeMode, setActiveMode] = useState<AssessmentMode>(initialMode);

  useEffect(() => {
    setActiveMode(initialMode);
  }, [initialMode]);

  return (
    <div className="assessment-hub" data-section="assessment-hub">
      <section className="panel assessment-hub-header">
        <div>
          <p className="eyebrow">Assessments</p>
          <h2>Choose how you want to prepare your care guide</h2>
          <p>Use guided symptoms or upload report details in one workspace.</p>
        </div>
        <div className="segmented assessment-mode-tabs" role="tablist" aria-label="Assessment type">
          <button
            type="button"
            id="guided-assessment-tab"
            role="tab"
            aria-selected={activeMode === "guided"}
            aria-controls="guided-assessment-panel"
            className={activeMode === "guided" ? "active" : ""}
            onClick={() => setActiveMode("guided")}
          >
            <Stethoscope size={17} /> Guided assessment
          </button>
          <button
            type="button"
            id="report-assessment-tab"
            role="tab"
            aria-selected={activeMode === "report"}
            aria-controls="report-assessment-panel"
            className={activeMode === "report" ? "active" : ""}
            onClick={() => setActiveMode("report")}
          >
            <FileText size={17} /> Report assessment
          </button>
        </div>
      </section>

      <div
        id="guided-assessment-panel"
        className="assessment-tab-panel"
        role="tabpanel"
        aria-labelledby="guided-assessment-tab"
        hidden={activeMode !== "guided"}
      >
        <AssessmentForm token={token} onCreated={onCreated} notify={notify} />
      </div>
      <div
        id="report-assessment-panel"
        className="assessment-tab-panel"
        role="tabpanel"
        aria-labelledby="report-assessment-tab"
        hidden={activeMode !== "report"}
      >
        <Reports token={token} notify={notify} onCreated={onCreated} />
      </div>
    </div>
  );
}
