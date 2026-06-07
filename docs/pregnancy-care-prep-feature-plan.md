# Pregnancy Care-Preparation Future Plan

This document describes a future PMS module for pregnant and postpartum users. It is a product and engineering plan only. The current PMS application does not yet implement pregnancy-specific screens, profile fields, or pregnancy-specific rule outputs.

PMS should remain a care-preparation and health-awareness assistant. It must not diagnose pregnancy complications, estimate fetal condition, replace prenatal care, provide emergency service, prescribe medication, or advise a user to delay contacting a qualified clinician.

## Purpose

Pregnant and postpartum users often need help organizing symptoms, health history, and questions before contacting a doctor, midwife, emergency department, or care team. A PMS pregnancy module could help by:

- Capturing pregnancy or postpartum context in a structured way.
- Recognizing urgent maternal warning signs using protected rules.
- Asking short follow-up questions that prepare the user for a clinical conversation.
- Creating a compact summary and expandable detail guide.
- Providing trusted source links and doctor or midwife discussion questions.

The service boundary stays the same as the rest of PMS: care preparation only, not diagnosis or triage.

## Source Basis

The future module should be guided by official and reputable maternal-health information:

- [CDC Hear Her urgent maternal warning signs](https://www.cdc.gov/hearher/maternal-warning-signs/index.html): warning signs can occur during pregnancy and within one year after delivery, and urgent signs should prompt immediate medical care.
- [WHO antenatal care recommendations](https://www.who.int/publications/i/item/9789241549912/): antenatal care should be person-centered and cover nutrition, maternal and fetal assessment, preventive measures, common symptoms, and care quality.
- [ACOG pregnancy patient education](https://www.acog.org/womens-health/pregnancy/during-pregnancy): pregnancy education includes discomforts, prenatal testing, health and safety, infection prevention, medical problems, and special considerations.
- [March of Dimes pregnancy resources](https://www.marchofdimes.org/find-support/topics/pregnancy): pregnancy education includes healthy habits, preeclampsia, bleeding, prenatal checkups, and other support topics.

These references should guide prompts, wording, warning signs, and trusted links. They should not be copied as legal or clinical policy without review.

## Future Patient Profile Additions

Add optional pregnancy context fields only after privacy and medical review:

- Pregnancy status: not pregnant, trying to conceive, pregnant, postpartum, prefer not to say.
- Gestational week, if pregnant and known.
- Postpartum time window, if postpartum and known.
- Expected due date, if known.
- Current care status: has OB-GYN, midwife, primary clinician, clinic appointment scheduled, no current care team, prefer not to say.
- High-risk pregnancy flag only if already told by a clinician.
- Clinician-diagnosed pregnancy-related conditions, such as gestational diabetes, hypertension, preeclampsia history, anemia, thyroid condition, or other.
- Current medications or supplements as user-entered context only, without advice or dose recommendations.

All fields should allow "Not sure" or "Prefer not to say" where appropriate. PMS should explain that incomplete pregnancy context may reduce the usefulness of preparation notes.

## Future Assessment Improvements

Add pregnancy-aware symptom grouping without changing the existing maximum-selected-symptom behavior:

- Vaginal bleeding or fluid leaking.
- Severe headache or headache that gets worse.
- Vision changes.
- Extreme swelling of hands or face.
- Severe belly pain that does not go away.
- Fever of 100.4 F / 38 C or higher.
- Trouble breathing.
- Chest pain, pressure, or fast-beating heart.
- Dizziness or fainting.
- Severe nausea or vomiting.
- Baby movement stopped or slowed, when currently pregnant and far enough along to track movement.
- Severe swelling, redness, or pain in leg or arm.
- Overwhelming tiredness or severe weakness.
- Thoughts of harming self or baby.
- Postpartum heavy bleeding, clots, discharge, fever, pain, or mental health crisis concerns.

Protected maternal warning-sign rules should run before mock or provider wording. Follow-up cards should ask safe context questions, such as timing, whether symptoms are worsening, whether the user is pregnant or postpartum, and whether a care team has already been contacted. They should not ask the user to self-diagnose a complication.

## Future Care-Preparation Output

The completed guide should use the current PMS summary-first pattern:

- Compact summary: pregnancy context, key symptoms, and whether urgent warning rules matched.
- Urgent warning: clear message when maternal red flags are present, with instruction to seek immediate medical care.
- Why this matters: plain-language explanation that symptoms can be important during pregnancy or postpartum without naming a diagnosis.
- What to track: timing, severity, temperature, bleeding amount, fluid leakage, movement change if applicable, medicines taken, and care-team contact attempts.
- Questions for doctor or midwife: what to mention first, whether symptoms need same-day review, what records to bring, and when to seek urgent care if symptoms change.
- Trusted sources: CDC Hear Her, WHO antenatal care, ACOG patient education, and March of Dimes pregnancy resources.

PMS must not tell users that symptoms are safe, normal, or not serious when pregnancy or postpartum red flags are present.

## PMS_Test3 AI Position

`PMS_Test3` uses mock AI-style wording by default and can optionally call OpenAI from the backend when provider environment variables are configured. A future pregnancy module in this branch should still keep all maternal safety decisions in Java rule logic.

OpenAI provider wording may:

- Convert rule-owned assessment context into plain-language explanations.
- Summarize user-entered pregnancy or postpartum context.
- Suggest questions to bring to a doctor or midwife.
- Rewrite monitoring notes in clearer language.
- Return trusted source link labels from backend-approved source lists.

OpenAI provider wording must not:

- Diagnose pregnancy complications.
- Estimate fetal condition.
- Prescribe medication or dosage.
- Replace emergency or prenatal care.
- Lower, remove, or soften rule-triggered urgent warnings.
- Invent custom emergency instructions outside backend-approved wording.

If provider mode is unavailable, times out, refuses, or returns invalid structured output, PMS should fall back to mock wording and keep the same rule-owned warning behavior.

## Future Data And API Notes

No API changes are implemented yet. A future implementation may extend existing profile and assessment DTOs with pregnancy context fields while keeping backward compatibility for non-pregnant users.

Recommended future interface behavior:

- Pregnancy fields are optional and user-owned.
- Existing assessment endpoints continue to work for all users.
- Maternal warning-sign results use the same `urgentWarning`, `careSummary`, `explanation`, `monitoringPlan`, `doctorPrepQuestions`, and `trustedSourceLinks` response fields.
- Staff-created rules remain upward-only and cannot disable protected maternal warning-sign rules.
- OpenAI provider output remains backend-only and is validated against PMS structured care-prep fields.

## Future Test Plan

Backend tests should cover:

- Pregnant and postpartum profile context is optional.
- Maternal warning-sign symptoms trigger urgent warnings before wording generation.
- Severe headache, vision changes, bleeding, trouble breathing, chest pain, fainting, severe belly pain, fever, reduced fetal movement, and self-harm concerns are protected rule scenarios.
- Mock and provider wording cannot lower maternal urgent warnings.
- Provider failure falls back to mock wording.
- Non-pregnant users continue to receive the existing general PMS assessment flow.

Frontend tests should cover:

- Pregnancy context fields show clear optional labels.
- Pregnancy-aware symptom grouping is searchable and selectable.
- Completed guides stay summary-first.
- Urgent maternal warning remains visible while full details are collapsed.
- Trusted source links render correctly.

Manual tests should confirm:

- The UI never claims diagnosis, prescription, fetal monitoring, or emergency service.
- Red-flag pregnancy or postpartum inputs show urgent care-preparation language immediately.
- The guide encourages contacting a doctor, midwife, emergency service, or qualified care team when warning signs are present.

## Review Before Implementation

Before building this module for real users, PMS needs medical, privacy, and security review. Real pregnancy or postpartum data is sensitive. Production use should include consent wording, retention rules, audit logging, access controls, incident response planning, and review of applicable privacy laws and institutional requirements.
