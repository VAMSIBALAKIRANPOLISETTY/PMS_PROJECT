import { AlertTriangle, BookOpen, CheckCircle2, ClipboardList, HelpCircle, Stethoscope } from "lucide-react";
import { RiskPill } from "./RiskPill";
import type { CarePrepGuideData, RiskLevel } from "../types";
import { displayRisk } from "../utils";

interface CarePrepGuideProps {
  title?: string;
  insight: CarePrepGuideData;
  riskLevel?: RiskLevel;
  riskScore?: number;
  reasons?: string[];
  suggestions?: string[];
}

function sourceParts(source: string) {
  const match = source.match(/https?:\/\/\S+/);
  if (!match) return { label: source, url: "" };
  return {
    label: source.replace(match[0], "").replace(/[:\-]\s*$/, "").trim() || match[0],
    url: match[0],
  };
}

export function CarePrepGuide({ title = "Care-preparation guide", insight, riskLevel, riskScore, reasons = [], suggestions = [] }: CarePrepGuideProps) {
  return (
    <div className="care-guide">
      <div className="care-guide-header">
        <div>
          <p className="eyebrow">Rules + AI mode: {insight.aiMode}</p>
          <h2>{title}</h2>
        </div>
        {riskLevel && <RiskPill value={riskLevel} />}
      </div>

      {typeof riskScore === "number" && (
        <div className="score-ring compact"><strong>{riskScore}</strong><span>{riskLevel ? `${displayRisk(riskLevel)} score` : "Score"}</span></div>
      )}

      {insight.urgentWarning && (
        <div className="urgent-warning"><AlertTriangle size={20} /><strong>{insight.urgentWarning}</strong></div>
      )}

      <section className="care-section">
        <div><Stethoscope size={18} /><strong>Summary</strong></div>
        <p>{insight.careSummary}</p>
      </section>

      <section className="care-section">
        <div><CheckCircle2 size={18} /><strong>Why this matters</strong></div>
        <p>{insight.explanation}</p>
        {reasons.length > 0 && <ul>{reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul>}
      </section>

      <section className="care-section">
        <div><ClipboardList size={18} /><strong>Possible directions to discuss</strong></div>
        <ul>{insight.possibleDirections.map((item) => <li key={item}>{item}</li>)}</ul>
      </section>

      <section className="care-section">
        <div><BookOpen size={18} /><strong>What to do next</strong></div>
        <ul>
          {[...suggestions, ...insight.monitoringPlan].map((item) => <li key={item}>{item}</li>)}
        </ul>
      </section>

      <section className="care-section">
        <div><HelpCircle size={18} /><strong>Doctor questions</strong></div>
        <ul>{insight.doctorPrepQuestions.map((item) => <li key={item}>{item}</li>)}</ul>
      </section>

      <section className="care-section source-list">
        <div><BookOpen size={18} /><strong>Trusted sources</strong></div>
        <ul>
          {insight.trustedSourceLinks.map((source) => {
            const parts = sourceParts(source);
            return <li key={source}>{parts.url ? <a href={parts.url} target="_blank" rel="noreferrer">{parts.label}</a> : source}</li>;
          })}
        </ul>
      </section>
    </div>
  );
}
