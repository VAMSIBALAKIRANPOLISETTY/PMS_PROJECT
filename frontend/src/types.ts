import type { LucideIcon } from "lucide-react";

export type Role = "USER" | "ADMIN";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type AiMode = "MOCK" | "OLLAMA" | "OPENAI" | "PROVIDER";
export type AssessmentStatus = "PENDING_FOLLOW_UP" | "COMPLETED";
export type Mode = "user" | "admin";
export type Page = "overview" | "assessment" | "reports" | "history" | "profile" | "assessments" | "rules" | "questions";
export type DesignId = "clinical" | "paper" | "vital";

export interface User {
  id: number;
  email: string;
  username: string;
  fullName: string;
  role: Role;
  age?: number;
  sex?: string;
  heightCm?: number;
  weightKg?: number;
  allergies?: string;
  chronicConditions?: string;
  lifestyle?: string;
  medications?: string;
  familyHistory?: string;
  mentalHealthHistory?: string;
  sleepQuality?: string;
  profileCompletion?: number;
  profileSetupComplete?: boolean;
}

export interface Assessment {
  id: number;
  userId: number;
  patient: string;
  mainSymptom: string;
  symptoms: string[];
  severity: number;
  durationDays: number;
  temperatureAvailable: boolean;
  temperatureF?: number | null;
  chronicCondition?: string;
  riskScore: number;
  riskLevel: RiskLevel;
  status: AssessmentStatus;
  reasons: string[];
  suggestions: string[];
  followUpQuestions: string[];
  followUpAnswers: string[];
  careSummary?: string | null;
  explanation?: string | null;
  possibleDirections: string[];
  urgentWarning?: string | null;
  monitoringPlan: string[];
  careTips: string[];
  doctorPrepQuestions: string[];
  trustedSourceLinks: string[];
  aiMode?: AiMode | null;
  createdAt: string;
}

export interface CarePrepGuideData {
  careSummary?: string | null;
  explanation?: string | null;
  possibleDirections: string[];
  urgentWarning?: string | null;
  monitoringPlan: string[];
  careTips?: string[];
  doctorPrepQuestions: string[];
  trustedSourceLinks: string[];
  aiMode?: AiMode | null;
}

export interface ReportFollowUpResponse {
  reportName: string;
  followUpQuestions: string[];
  aiMode: AiMode;
}

export interface ReportInsight extends CarePrepGuideData {
  reportName: string;
  followUpQuestions: string[];
  followUpAnswers: string[];
}

export type Notify = (message: string, tone?: "success" | "warning" | "danger") => void;

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
  primarySymptom?: string | null;
  secondarySymptom?: string | null;
  minSeverity?: number | null;
  minDurationDays?: number | null;
  chronicConditionKeyword?: string | null;
  riskLevel: RiskLevel;
  score: number;
  urgent?: boolean;
  active?: boolean;
  explanation: string;
}

export interface QuestionSuggestionResponse {
  symptomKey: string;
  suggestions: string[];
  aiMode: AiMode;
}

export type NavItem = [Page, string, LucideIcon];
