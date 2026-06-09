# PMS Real-World Product Expansion

This document describes how PMS expands from a symptom form into a patient-owned health context hub. The product boundary remains care preparation: PMS organizes information, highlights safety signals through backend rules, and prepares summaries for qualified medical conversations. It does not diagnose, prescribe, estimate fetal condition, or replace emergency care.

## Product Positioning

PMS Health is designed around one core problem: patients often have symptoms, report values, wearable trends, medicines, allergies, and hospital records in different places. During a medical visit, those details are easy to forget or explain poorly. PMS creates one structured workspace where the patient can prepare a clearer summary before speaking with a clinician.

The product value is not only “what symptoms do you have?” The stronger value is:

- Symptom intake plus guided follow-up cards.
- Structured patient record profile with emergency, clinical, care-team, insurance, baseline, and preference fields.
- Saved report-based assessments with extracted values and exportable summaries.
- Connected-health timeline records from devices, apps, hospitals, reports, and manual entry.
- Rule-owned safety checks before AI wording.
- AI wording used only for summaries, tips, questions, and clinician discussion notes.

## Expanded Patient Profile

The patient profile should act as a practical patient record, not only an account page.

Current implemented groups:

- Identity: full name, date of birth, age, sex, sex at birth, gender identity, preferred language, phone, address.
- Body basics: height, weight, blood type, pregnancy or postpartum status.
- Emergency: emergency contact name, relationship, phone, preferred hospital.
- Clinical background: allergies, chronic conditions, surgeries, medications, immunizations, family history, mental health history.
- Care team: primary doctor, specialists, hospital or clinic, insurance provider, member ID.
- Lifestyle and baseline: activity/lifestyle, sleep quality, tobacco or alcohol use, diet notes, baseline heart rate, baseline blood pressure.
- Preferences: connected-data consent, notification preference, export preference, data-sharing preference.

Profile completion is calculated by patient-record groups. Completing only the seven health-history cards no longer means the full profile is 100% complete.

## Report-Based Assessment

Report assessment should behave like a first-class assessment source.

Current implemented direction:

- User uploads a text-based PDF or pastes report text.
- Backend extracts readable report text with PDFBox.
- Backend extracts possible lab observations such as test name, value, unit, range, and flag.
- Backend saves the report as an `Assessment` with `sourceType=REPORT`.
- Backend asks required report follow-up questions.
- Finalized report assessment appears in patient History and staff Care Review.
- The same report drawer shows report metadata, extracted values, summary, reasons, possible directions, care tips, monitoring plan, doctor questions, and trusted links.
- Completed assessments can be exported through `GET /api/assessments/{id}/export`.

Long-term report parsing should align with FHIR concepts:

- `DiagnosticReport`: the overall lab or imaging report.
- `Observation`: individual results inside that report.

This model keeps reports interoperable with hospital and lab systems later.

## Connected Health

Connected Health should be built as a connector platform, not isolated buttons.

Current implemented direction:

- `Connected Health` appears as a patient workspace section.
- Patient can review connection permissions for Apple Health, Android Health Connect, Samsung Health, Hospital Portal, Lab Report Upload, and Manual Entry.
- Backend stores connection metadata.
- Backend stores normalized timeline records instead of raw provider-specific payloads.
- Normalized timeline records can represent vitals, lab results, medication records, conditions, activity summaries, sleep summaries, hospital records, and manual entries.

Future real integrations:

- Apple Health / Apple Watch through an iOS companion app using HealthKit and, where available, Apple Health Records.
- Android and Samsung wearables through Health Connect and Samsung Health Data SDK where supported.
- Hospitals and clinics through SMART on FHIR / FHIR APIs where patient-authorized access is available.
- Manual fallback through PDF upload, CSV upload, copied report text, and manual vitals entry.

The practical flow:

1. User opens Connected Health.
2. User chooses a source.
3. PMS explains permissions in plain language.
4. Provider/native OAuth or device permission flow completes outside the frontend.
5. Backend stores connection metadata.
6. Backend imports provider data into normalized timeline records.
7. Rules process safety-sensitive signals first.
8. AI summarizes trends only after rule processing.
9. User can generate an exportable care-preparation summary.

## AI Role

AI is useful for readability and summarization, not clinical authority.

Allowed AI responsibilities:

- Plain-language report summary.
- Symptom and profile context summary.
- Follow-up question suggestions.
- Care tips.
- Doctor or midwife discussion questions.
- Trend explanation for connected records.

Not allowed:

- Diagnosis.
- Prescription.
- Emergency triage replacement.
- Lowering urgent warnings.
- Replacing protected safety rules.
- Making claims about fetal condition or pregnancy complications.

In `PMS_Test3`, provider mode follows this chain:

1. Ollama Gemma 4 31B.
2. OpenAI fallback.
3. Local fallback output.

The frontend never stores provider keys and never calls AI providers directly.

## Selling Point

PMS does not only ask “how do you feel?” It combines symptoms, profile context, lab reports, wearable trends, manual health events, and hospital records into one patient-owned preparation summary.

That makes PMS useful before appointments, follow-up visits, urgent-care conversations, chronic-condition review, pregnancy care preparation, and family-supported health organization.

## Safety And Compliance Work Needed Before Production

Before real patient use, PMS would need:

- Privacy and consent review.
- Provider data-use review.
- Audit logging.
- Access control hardening.
- Refresh-token and session-management design.
- Rate limiting and abuse protection.
- Production secrets management.
- Clinical safety review for rule content.
- Retention and deletion policies.
- Incident and breach response process.
- Mobile-app privacy review for HealthKit / Health Connect access.
- SMART on FHIR app registration and hospital integration review.
