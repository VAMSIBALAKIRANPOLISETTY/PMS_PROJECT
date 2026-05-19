import { ArrowRight } from "lucide-react";
import { RiskPill } from "../../components/RiskPill";
import type { Rule } from "../../types";

export function Rules({ rules }: { rules: Rule[] }) {
  return (
    <section className="panel wide" data-section="admin-rules">
      <div className="section-title"><div><p className="eyebrow">Admin control</p><h2>Rule engine logic</h2></div><button className="primary-button">Add rule<ArrowRight size={17} /></button></div>
      <div className="rule-grid">{rules.map((rule) => <div className="rule-card" key={rule.id}><span>{rule.conditionLabel}</span><RiskPill value={rule.riskLevel} /><p>{rule.explanation}</p></div>)}</div>
    </section>
  );
}
