import {
  ClipboardList,
  Database,
  FileText,
  LayoutDashboard,
  LineChart,
  MessageSquareText,
  Settings2,
  UserRound,
} from "lucide-react";
import type { Analytics, DesignId, NavItem } from "./types";

export const designOptions: { id: DesignId; name: string; note: string }[] = [
  { id: "clinical", name: "Clinical Calm", note: "Clean care surfaces with teal, ink, blue, and coral emphasis." },
  { id: "paper", name: "Paper Console", note: "Editorial paper feel with charcoal text, soft blue, and warm accent." },
  { id: "vital", name: "Vital Signal", note: "Energetic product UI with green, plum, and amber status cues." },
];

export const possibleSymptoms = [
  "Abdominal pain", "Acidity", "Allergic reaction", "Anxiety", "Back pain", "Body pain", "Breathing difficulty",
  "Chest pain", "Chills", "Constipation", "Cough", "Dehydration", "Diarrhea", "Dizziness", "Ear pain", "Eye redness",
  "Fatigue", "Fever", "Frequent urination", "Headache", "High blood pressure", "Joint pain", "Loss of appetite",
  "Low blood sugar", "Migraine", "Nausea", "Neck pain", "Palpitations", "Rash", "Runny nose", "Shortness of breath",
  "Sore throat", "Stomach cramps", "Sweating", "Swelling", "Throat pain", "Tooth pain", "Vomiting", "Weakness",
  "Wheezing", "Vision problem",
];

export const userNav: NavItem[] = [
  ["overview", "Overview", LayoutDashboard],
  ["assessment", "Assessment", ClipboardList],
  ["reports", "Reports", FileText],
  ["history", "History", LineChart],
  ["profile", "Profile", UserRound],
];

export const adminNav: NavItem[] = [
  ["overview", "Overview", LayoutDashboard],
  ["assessments", "Assessments", ClipboardList],
  ["rules", "Rules", Settings2],
  ["questions", "Questions", MessageSquareText],
  ["datasets", "Datasets", Database],
];

export const emptyAnalytics: Analytics = {
  totalUsers: 0,
  totalAssessments: 0,
  highRiskCount: 0,
  mediumRiskCount: 0,
  lowRiskCount: 0,
  commonSymptoms: [],
};
