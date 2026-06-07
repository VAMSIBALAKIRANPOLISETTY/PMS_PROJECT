# PMS Demo Quick Summary

Use this as a speaking sheet before a professor, guide, or manager demo. It is intentionally shorter than the README.

## 30-Second Explanation

PMS Health is a full-stack care-preparation system. A patient can create an account, complete basic health history, enter symptoms, answer guided follow-up questions, and receive a safe summary to discuss with a doctor. Staff users have a separate clinical-operations workspace to review completed records, manage safety rules, and manage question prompts. PMS does not diagnose, prescribe, or replace emergency care.

## 1-Minute Explanation

PMS Health solves the problem of users forgetting or poorly organizing health details before meeting a doctor. The frontend is React and TypeScript, the backend is Spring Boot, and PostgreSQL stores users, assessments, rules, and questions. The assessment flow first collects symptoms, severity, duration, temperature availability, and health context. The backend saves a draft, asks follow-up questions, then creates a care-preparation guide with a compact summary and expandable details. Safety is rule-based: red flags and urgent warnings are handled by Java rules first. In `PMS_Test2`, AI behavior is represented by a backend mock insight service only, so no external AI API is called.

## Patient Flow Talking Points

- Landing page explains the product boundary and safety purpose.
- Signup is patient-only, adult-only, and includes privacy and terms acknowledgment.
- Profile setup collects reviewed health-history cards instead of invented defaults.
- Guided assessment supports up to five symptoms and required follow-up cards.
- Completed result shows summary first, then detailed reasons, next steps, doctor questions, and trusted links.
- History opens a full report drawer for completed assessments.

## Staff Flow Talking Points

- Staff use a separate Staff login and cannot enter patient assessment flows.
- Clinical operations shows analytics, risk mix, and completed assessment review.
- Staff can add operational rules that only raise awareness risk for future assessments.
- Protected red-flag safeguards always stay active and cannot be downgraded.
- Staff can add active or inactive question prompts for future guided assessments.
- Staff profile is read-only in this version.

## AI Explanation

- `PMS_Test2` uses mock AI only.
- No OpenAI, Ollama, or other external AI provider is called in this branch.
- `AiInsightService` is the backend interface.
- `MockAiInsightService` creates predictable demo wording for summaries, explanations, monitoring notes, doctor questions, and trusted links.
- The frontend never stores AI keys and never calls an AI provider.

## Safety Explanation

- PMS is care preparation and health awareness only.
- It does not diagnose disease.
- It does not prescribe medicine.
- It does not replace emergency care.
- Rule-based red flags control urgent warnings.
- AI-style wording cannot lower rule-based urgent warnings.

## JWT Explanation

- PMS now uses signed JWT access tokens.
- Login, staff login, and signup return a token in the same `token` response field.
- The frontend sends `Authorization: Bearer <token>`.
- The backend validates the JWT signature, issuer, expiry, and user ID.
- Admin endpoints still check the current database role, not only the JWT role claim.
- Current access tokens expire after 12 hours by default.

## Expected Questions And Short Answers

- What is the main problem?
  Users often do not organize symptoms, reports, and health history clearly before consulting a doctor.

- What is your solution?
  PMS guides the user through structured intake, follow-up questions, and a care-preparation report.

- Does it diagnose?
  No. It prepares information and safe discussion points for a qualified medical professional.

- Did you use real AI?
  In `PMS_Test2`, no. It uses mock AI through the backend `AiInsightService`.

- Why mock AI?
  It keeps the demo predictable, avoids sending health data to external providers, and lets us test the system safely.

- Where are safety rules handled?
  `RiskEngineService` owns score, risk level, red flags, urgent warning logic, and staff-created upward-only rules.

- Where is JWT used?
  `JwtService` creates and validates signed tokens. `AuthService` uses it before loading the current user and checking roles.

- What can staff do?
  Staff can review completed assessments, see analytics, manage rules, manage questions, and view staff profile details.

- How can this support pregnant women later?
  Future pregnancy support can collect pregnancy or postpartum context, check maternal warning signs with protected rules, and prepare doctor or midwife discussion notes.

- What is not production-ready yet?
  It still needs production security hardening, privacy/legal review, audit logging, deployment work, and medical expert review before real patient use.
