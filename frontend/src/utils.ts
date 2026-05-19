import type { Assessment, RiskLevel } from "./types";

export function displayRisk(level: RiskLevel) {
  return level.charAt(0) + level.slice(1).toLowerCase();
}

export function clampNumber(value: string | number, min: number, max: number, fallback: number) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(max, Math.max(min, parsed));
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short" }).format(new Date(value));
}

export function trendFromAssessments(assessments: Assessment[]) {
  const latest = [...assessments].slice(0, 7).reverse();
  if (latest.length === 0) return [{ day: "Now", risk: 0, temp: 98.6, heart: 0, sleep: 0 }];
  return latest.map((item) => ({
    day: formatDate(item.createdAt),
    risk: item.riskScore,
    temp: item.temperatureF,
    heart: item.heartRate,
    sleep: 7,
  }));
}
