import type { LucideIcon } from "lucide-react";

export type Role = "USER" | "ADMIN";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type AiMode = "MOCK" | "OLLAMA" | "OPENAI" | "PROVIDER";
export type AssessmentStatus = "PENDING_FOLLOW_UP" | "COMPLETED";
export type AssessmentSourceType = "SYMPTOM" | "REPORT" | "DEVICE" | "HOSPITAL";
export type Mode = "user" | "admin";
export type Page = "overview" | "assessment" | "reports" | "connected" | "history" | "profile" | "assessments" | "rules" | "questions";
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
  dateOfBirth?: string;
  sexAtBirth?: string;
  genderIdentity?: string;
  preferredLanguage?: string;
  phone?: string;
  address?: string;
  bloodType?: string;
  pregnancyStatus?: string;
  emergencyContactName?: string;
  emergencyContactRelationship?: string;
  emergencyContactPhone?: string;
  preferredHospital?: string;
  surgeries?: string;
  immunizations?: string;
  primaryDoctor?: string;
  specialistNames?: string;
  hospitalClinic?: string;
  insuranceProvider?: string;
  insuranceMemberId?: string;
  baselineHeartRate?: string;
  baselineBloodPressure?: string;
  tobaccoAlcoholUse?: string;
  dietNotes?: string;
  connectedDataConsent?: boolean;
  notificationPreference?: string;
  exportFormatPreference?: string;
  dataSharingPreference?: string;
  profilePhotoDataUrl?: string;
  profileCompletion?: number;
  profileSetupComplete?: boolean;
}

export interface LabObservation {
  testName?: string;
  valueText?: string;
  unit?: string;
  referenceRange?: string;
  flag?: string;
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
  sourceType?: AssessmentSourceType;
  sourceName?: string | null;
  sourceRecordId?: string | null;
  reportName?: string | null;
  reportDate?: string | null;
  reportProvider?: string | null;
  connectedHealthSummary?: string | null;
  patientProfilePhotoDataUrl?: string | null;
  extractedObservations?: LabObservation[];
  exportUrl?: string | null;
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

export interface HealthConnection {
  id: number;
  provider: string;
  displayName: string;
  status: string;
  connectedAt?: string | null;
  lastSyncAt?: string | null;
  permissionSummary: string;
}

export interface ConnectionStartResponse {
  provider: string;
  authorizationUrl: string;
  permissionSummary: string;
}

export interface TimelineRecord {
  id: number;
  sourceType: AssessmentSourceType;
  recordType: string;
  label: string;
  valueText?: string | null;
  unit?: string | null;
  sourceName?: string | null;
  notes?: string | null;
  observedAt: string;
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

export interface AiStatus {
  mode: string;
  providerChain: string[];
  ollamaModel: string;
  ollamaBaseUrl: string;
  ollamaApiKeyPresent: boolean;
  openAiModel: string;
  openAiBaseUrl: string;
  openAiApiKeyPresent: boolean;
  lastProviderAttempt: string;
  lastFallbackReason: string;
}

export type NavItem = [Page, string, LucideIcon];
