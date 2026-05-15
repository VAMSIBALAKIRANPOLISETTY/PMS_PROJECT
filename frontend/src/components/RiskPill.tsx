import type { RiskLevel } from "../types";
import { displayRisk } from "../utils";

export function RiskPill({ value }: { value: RiskLevel }) {
  return <span className={`risk-pill ${displayRisk(value).toLowerCase()}`}>{displayRisk(value)}</span>;
}
