# PMS Product Quick Summary

Use this as a short speaking sheet before a project review or product walkthrough.

## 30-Second Explanation

PMS Health is a patient health-preparation platform. It helps a patient organize symptoms, profile details, health history, reports, and connected-health records before speaking with a qualified medical professional. The backend uses rule-based safety checks for red flags and then uses the AI insight layer only to improve summaries, tips, follow-up questions, and doctor-preparation notes. PMS does not diagnose, prescribe, or replace urgent care.

## 1-Minute Explanation

The system has two workspaces. Patients can sign up, complete a structured profile, create symptom assessments, answer follow-up cards, upload health reports, review saved history, export assessment summaries, and manage connected-health sources. Staff use a separate clinical-operations workspace to review completed records, monitor analytics, manage upward-only safety rules, and maintain the question bank. Authentication uses JWT access tokens, and the frontend never stores AI provider keys. In `PMS_Test3`, backend provider mode can use OpenAI, Ollama Cloud, and local fallback output in the configured order. The medical-safety layer remains rule-owned.

## Patient Flow

1. Create account with privacy and terms acknowledgment.
2. Finish patient-record profile: identity, body basics, blood type, emergency contact, clinical background, care team, lifestyle, and preferences.
3. Select symptoms from the grouped symptom library.
4. Enter severity, duration, temperature availability, and chronic-condition context.
5. Answer follow-up cards.
6. View compact care guide first, then expand details.
7. Upload report PDF/text, answer report follow-ups, save report assessment, and export PDF.
8. Connect health sources and review normalized health timeline records.

## Staff Flow

1. Use Staff login.
2. Review clinical operations analytics.
3. Open completed assessment and report records.
4. Add active/inactive safety rules that can only raise risk.
5. Add active/inactive follow-up questions.
6. Use AI question suggestions as draft prompts that staff must review before saving.

## AI Explanation

- AI does not decide risk level.
- AI does not own urgent warnings.
- Java rules own red flags and safety boundaries.
- AI helps with wording, summaries, tips, and question suggestions.
- Frontend never calls Ollama or OpenAI.
- Provider chain in `PMS_Test3`: configurable OpenAI/Ollama Cloud -> local fallback.

## JWT Explanation

- Login returns a signed JWT access token.
- Browser sends `Authorization: Bearer <token>`.
- Backend validates signature, issuer, expiry, and user existence.
- Staff access checks the current database role, not only token claims.

## Strong Selling Point

PMS is not just a symptom form. It combines symptoms, structured profile, lab reports, wearable/app data, hospital-record concepts, and manual entries into one patient-owned care-preparation summary.

## Expected Questions

- Is PMS a diagnosis app?
  No. It is a care-preparation and health-awareness product.

- Where is AI used?
  Backend only, after rules. It improves wording, summaries, questions, and tips.

- Why keep local fallback output?
  It keeps workflows reliable if external providers fail or are not configured.

- Can PMS use real Apple Watch or hospital data today?
  The backend and UI now support the connector model and normalized timeline records. Real Apple Health, Health Connect, Samsung Health, and hospital connections require native companion apps or SMART on FHIR integration work.

- What makes report assessment valuable?
  Reports become saved records with extracted values, follow-up answers, care-preparation summary, history drawer, staff review visibility, and PDF export.
