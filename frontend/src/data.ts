import {
  ClipboardList,
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

export const symptomGroups = [
  { category: "General", symptoms: ["Chills", "Dehydration", "Fatigue", "Fever", "Heavy bleeding", "Loss of appetite", "Low blood sugar", "Severe weakness", "Sweating", "Swelling", "Weakness"] },
  { category: "Respiratory", symptoms: ["Breathing difficulty", "Congestion", "Cough", "Shortness of breath", "Sore throat", "Wheezing"] },
  { category: "Cardiovascular", symptoms: ["Chest pain", "Chest pressure", "High blood pressure", "Palpitations"] },
  { category: "Digestive", symptoms: ["Abdominal pain", "Acidity", "Constipation", "Diarrhea", "Nausea", "Stomach cramps", "Vomiting"] },
  { category: "Neurological", symptoms: ["Confusion", "Dizziness", "Fainting", "Headache", "Migraine", "Seizure", "Vision problem"] },
  { category: "Musculoskeletal", symptoms: ["Back pain", "Body pain", "Joint pain", "Neck pain", "Muscle cramps"] },
  { category: "Skin and allergy", symptoms: ["Allergic reaction", "Itching", "Rash", "Skin redness"] },
  { category: "ENT and eye", symptoms: ["Ear pain", "Eye redness", "Runny nose", "Throat pain", "Tooth pain"] },
  { category: "Urinary", symptoms: ["Frequent urination", "Painful urination"] },
  { category: "Mental wellbeing", symptoms: ["Anxiety", "Low mood", "Panic symptoms", "Sleep difficulty"] },
];

export const possibleSymptoms = symptomGroups.flatMap((group) => group.symptoms);

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
  ["profile", "Profile", UserRound],
];

export const emptyAnalytics: Analytics = {
  totalUsers: 0,
  totalAssessments: 0,
  highRiskCount: 0,
  mediumRiskCount: 0,
  lowRiskCount: 0,
  commonSymptoms: [],
};
