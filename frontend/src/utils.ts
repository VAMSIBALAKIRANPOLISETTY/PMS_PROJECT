import type { Assessment, RiskLevel } from "./types";

export function displayRisk(level: RiskLevel) {
  return level.charAt(0) + level.slice(1).toLowerCase();
}

export function clampNumber(value: string | number, min: number, max: number, fallback: number) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(max, Math.max(min, parsed));
}

export function feetInchesToCm(feet: string | number, inches: string | number) {
  const parsedFeet = Number(feet);
  const parsedInches = Number(inches);
  if (!Number.isFinite(parsedFeet) || !Number.isFinite(parsedInches)) return 0;
  return Math.round(((parsedFeet * 12) + parsedInches) * 2.54 * 10) / 10;
}

export function cmToFeetInches(cm?: number) {
  if (!cm || !Number.isFinite(cm)) return { feet: "", inches: "" };
  const totalInches = Math.round(cm / 2.54);
  return { feet: String(Math.floor(totalInches / 12)), inches: String(totalInches % 12) };
}

export function formatHeight(cm?: number) {
  if (!cm || !Number.isFinite(cm)) return "Height not set";
  const imperial = cmToFeetInches(cm);
  return `${cm} cm (${imperial.feet} ft ${imperial.inches} in)`;
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short" }).format(new Date(value));
}

export function trendFromAssessments(assessments: Assessment[]) {
  const latest = [...assessments].slice(0, 7).reverse();
  if (latest.length === 0) return [{ day: "Now", risk: 0, temp: 98.6, sleep: 0 }];
  return latest.map((item) => ({
    day: formatDate(item.createdAt),
    risk: item.riskScore,
    temp: item.temperatureF ?? 98.6,
    sleep: 7,
  }));
}
