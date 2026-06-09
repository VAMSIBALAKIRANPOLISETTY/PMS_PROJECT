# PMS Health Business Selling Points

## Short Product Summary

PMS Health is a patient-owned care-preparation platform. It helps a person organize symptoms, health history, report values, connected-health records, and follow-up answers before speaking with a doctor. The product does not diagnose or prescribe. It prepares a clear, structured health conversation so the patient and care team can use time more effectively.

## 30-Second Business Pitch

People often reach a doctor with scattered information: symptoms in memory, reports in files, smartwatch data in another app, and medical history in different places. PMS Health brings those inputs into one patient workspace, applies protected safety rules first, and then uses backend AI wording to produce a simple care-preparation guide. The result is a compact summary, possible discussion directions, tips, monitoring notes, and doctor questions that can be saved, reviewed, and exported.

## Two-Minute Product Explanation

PMS Health solves a communication and preparation problem in healthcare. A patient may know something feels wrong, but they may not know how to explain severity, duration, health-history context, recent reports, wearable trends, or warning signs clearly. PMS gives them a guided assessment and a report-based assessment in one place. It asks follow-up questions, stores the final guide in history, and lets staff review completed records from a clinical operations workspace.

The product uses a safety-first architecture. The Java rule engine owns risk scoring, red-flag detection, and urgent warning boundaries. AI is used only after those rules to improve the wording of summaries, follow-up prompts, tips, and doctor-preparation questions. In `PMS_Test3`, the backend tries Ollama Gemma 4 31B first, then OpenAI, then internal fallback output. The frontend never stores provider keys and never calls AI providers directly.

The long-term vision is a patient health context hub. PMS can connect to wearable apps, hospital portals, uploaded lab reports, manual vitals, and future mobile health integrations. That means the product can evolve from a symptom form into a preparation layer that combines profile, reports, trends, and care-team discussion notes.

## Core Selling Points

- Unified assessment workspace: Guided symptom assessment and report-based assessment live on the same patient page, so users do not need to learn two separate flows.
- Structured patient profile: The product supports real patient-record style details such as body basics, blood type, allergies, chronic conditions, medications, family history, emergency contact, care team, insurance, lifestyle, and preferences.
- Report-based assessment: Users can upload PDF, text, or image report files. Text-based reports and pasted report content can be extracted into notable observations, summary, tips, directions, and doctor questions.
- Connected Health foundation: PMS has a connector model for Apple Health, Android Health Connect, Samsung Health, hospital portals, lab uploads, and manual entry. Current connectors store safe metadata and normalized timeline records.
- Rules plus AI: Safety rules decide risk and urgent warning behavior. AI only improves wording, question drafts, summaries, tips, and patient-friendly explanations.
- Provider fallback chain: `PMS_Test3` supports Ollama Gemma 4 31B first, OpenAI second, and internal fallback last, so the product can keep working even when a provider is unavailable.
- Saved history and export: Completed symptom and report assessments are saved, can be reopened in a detailed drawer, and can be exported as a printable PDF summary.
- Staff workspace: Staff can review completed assessment records, analytics, safety rules, managed questions, and AI-generated question drafts before activation.
- Swagger API transparency: All major endpoints can be explored from Swagger, which makes the product easier to test, explain, and integrate.
- JWT authentication: Login returns signed JWT access tokens. The backend validates token signature, expiry, issuer, user existence, and role before allowing private operations.

## What Makes PMS Different

- It is not just a chatbot. It has structured intake, database persistence, role-based access, rule-owned safety logic, report history, staff controls, and export.
- It is not just a symptom checker. It prepares a care conversation using symptoms, profile details, follow-up answers, report values, and future connected-health data.
- It is not LLM-only. AI cannot lower risk, remove urgent warnings, diagnose, prescribe, or replace care. This creates a stronger safety story for a business audience.
- It can grow through integrations. Apple Health, Android Health Connect, Samsung Health, SMART on FHIR hospital portals, and report uploads can all feed the same normalized timeline model.

## Business Value

- Better patient preparation before appointments.
- Less missing context during medical conversations.
- A reusable patient history that can be reviewed over time.
- Clear staff oversight through rules, question bank, analytics, and care-review records.
- Lower risk from AI misuse because rules own safety decisions.
- Expansion path for connected devices, hospital records, pregnancy care-preparation, multilingual support, and doctor dashboards.

## Safe Product Boundary

PMS Health is for health awareness and care preparation. It does not diagnose disease, prescribe medication, estimate emergency severity, replace a doctor, replace a hospital portal, or make clinical decisions. Urgent warning signs remain rule-owned and should direct users to appropriate professional care.

## Quick Lines To Remember

- "PMS is a care-preparation workspace, not a diagnosis app."
- "Rules decide safety; AI improves communication."
- "The patient owns the context: symptoms, reports, history, and connected data."
- "The output is not just Low, Medium, or High. It explains why, what to monitor, what to ask, and what to bring to a doctor."
- "The product can scale from today’s assessment workflow into a connected health hub."
