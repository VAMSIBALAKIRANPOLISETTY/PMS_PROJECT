import type { LucideIcon } from "lucide-react";

export type Role = "USER" | "ADMIN";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type Mode = "user" | "admin";
export type Page = "overview" | "assessment" | "reports" | "history" | "profile" | "assessments" | "rules" | "questions" | "datasets";
export type DesignId = "clinical" | "paper" | "vital";

export interface User {
  id: number;
  email: string;
  username: string;
  fullName: string;
  role: Role;
  age?: number;
  gender?: string;
  heightCm?: number;
  weightKg?: number;
  allergies?: string;
  chronicConditions?: string;
  lifestyle?: string;
}

export interface Assessment {
  id: number;
  userId: number;
  patient: string;
  mainSymptom: string;
  severity: number;
  durationDays: number;
  temperatureF: number;
  oxygenLevel: number;
  heartRate: number;
  chronicCondition?: string;
  riskScore: number;
  riskLevel: RiskLevel;
  reasons: string[];
  suggestions: string[];
  followUpQuestions: string[];
  createdAt: string;
}

export interface Analytics {
  totalUsers: number;
  totalAssessments: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
  commonSymptoms: { symptom: string; count: number }[];
}

export interface Question {
  id: number;
  symptomKey: string;
  prompt: string;
  inputType: string;
  active: boolean;
}

export interface Rule {
  id: number;
  conditionLabel: string;
  riskLevel: RiskLevel;
  score: number;
  explanation: string;
}

export type NavItem = [Page, string, LucideIcon];
