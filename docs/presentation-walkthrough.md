# PMS Health Presentation Walkthrough

## Before Opening The Product

1. Start PostgreSQL if it is not already running.
2. Start the backend from IntelliJ or terminal with the correct environment variables:
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `JWT_SECRET`
   - `AI_MODE=provider` for real provider testing
   - `AI_PROVIDER_CHAIN=openai,ollama`
   - `OPENAI_API_KEY`
   - `OPENAI_MODEL=gpt-4o-mini`
   - `OLLAMA_API_KEY` if Ollama Cloud should be attempted as fallback
   - `OLLAMA_MODEL=gemma4:31b-cloud`
   - `OLLAMA_BASE_URL=https://ollama.com/api`
   - `AI_TIMEOUT_SECONDS=20`
3. Start the frontend:
   - `cd frontend`
   - `npm run dev`
4. Open:
   - Frontend: `http://127.0.0.1:5173`
   - Backend health: `http://127.0.0.1:8080/api/health`
   - Swagger: `http://127.0.0.1:8080/swagger-ui.html`

## Opening Pitch

Use this line:

> PMS Health is a patient-owned care-preparation platform. It organizes symptoms, reports, profile context, and connected-health records into a clear summary before a doctor conversation. The key safety point is that rules own urgent warnings and risk, while AI only improves wording, tips, summaries, and questions.

## Demo Flow

### 1. Landing Page

Show the landing page first. Explain:

- PMS gives users a clear preparation path before care.
- It is not a hospital portal and not a diagnosis tool.
- The product focuses on symptom organization, report preparation, follow-ups, safety guidance, and history.

### 2. Patient Signup Or Login

Show patient login or create a patient account if needed.

Explain:

- Public signup is patient-only.
- The account stores profile and consent details.
- JWT token authentication protects private endpoints.

### 3. Patient Profile

Open the patient profile.

Explain:

- PMS stores real patient-context fields: height, weight, blood type, allergies, chronic conditions, medications, family history, emergency contact, care team, insurance, lifestyle, and preferences.
- Profile photo helps records feel personal and is reflected in history and staff review.
- Better profile context means better preparation notes.

### 4. Connected Health

Open Connected Health.

Explain:

- This is the future-ready integration layer.
- The current product can save connection metadata and normalized timeline records.
- Real integrations can plug in through Apple Health, Android Health Connect, Samsung Health, hospital portals, lab uploads, or manual entry.
- Dummy credentials demonstrate the connection flow, but provider passwords/API tokens are not stored.

Suggested line:

> The selling point is that PMS does not only ask how the user feels today. It can become a patient-owned health context hub that combines profile, reports, wearable trends, and hospital records.

### 5. Assessment Page

Open Assessment.

Show that Guided Assessment and Report Assessment are tabs on the same page.

Explain:

- Guided Assessment is for symptoms.
- Report Assessment is for lab reports, PDFs, images, or pasted report text.
- Both can include connected-health context when records are available.

### 6. Guided Assessment

Select symptoms, severity, duration, temperature mode, and chronic-condition context.

Explain:

- The system first saves a draft and asks follow-up questions.
- Follow-ups create a better care-preparation guide.
- Red flags remain rule-owned and are shown immediately.

After submitting follow-ups, show:

- compact summary first
- risk pill and score
- care summary
- View full care details
- possible directions
- monitoring plan
- care tips
- doctor questions
- trusted links

### 7. Report Assessment

Upload or paste readable report text.

Explain:

- PMS extracts notable report values when text is readable.
- Image upload is accepted, but readable pasted text is required until OCR/vision extraction is added.
- Final report assessment is saved to history and can be exported.

### 8. History And Export

Open History and click a completed assessment.

Explain:

- The full care-preparation record can be reopened.
- Report and symptom assessments use the same drawer format.
- The patient can export a printable PDF summary.

### 9. Staff Workspace

Use Staff login and open the staff workspace.

Explain:

- Staff can review completed records and analytics.
- Staff can manage safety rules and follow-up questions.
- AI can suggest question drafts, but staff must save and activate them.
- Staff profile includes sanitized AI provider status so the team can see provider mode and fallback reason without exposing keys.

### 10. Swagger

Open Swagger.

Explain:

- Swagger lists all API endpoints in one place.
- Login returns a token.
- Authorize with `Bearer <token>`.
- Then test patient and staff endpoints.

## AI Explanation For Business Team

Use this exact explanation:

> PMS_Test3 uses a configurable provider chain. For a restricted company laptop, I run OpenAI first because it is the most reliable cloud provider for the presentation, keep Ollama Cloud as a supported fallback, and then use internal fallback wording if providers are unavailable. The frontend never stores API keys. AI does not score risk or create urgent warnings. The Java rule engine owns safety decisions.

If DevTools shows `aiMode=MOCK`, say:

> That means the backend used the final safe fallback. The staff AI status panel and backend logs show whether it happened because provider mode was off, the key was missing, the model was unavailable, the provider timed out, or the response failed validation.

## Business Q&A

### Is this a diagnosis app?

No. PMS is care preparation and health awareness. It organizes information and creates doctor-preparation notes.

### Why use AI?

AI improves wording, summaries, follow-up prompts, tips, and doctor questions. It does not own medical safety.

### Why rules plus AI?

Rules are predictable and protect red flags. AI makes the output easier for normal users to understand.

### How can this become a bigger product?

By adding real Apple Health, Android Health Connect, Samsung Health, SMART on FHIR hospital integrations, OCR for scanned reports, doctor dashboards, multilingual support, and pregnancy/postpartum care-preparation.

### What is the strongest selling point?

PMS combines symptoms, profile, report values, connected-health records, follow-up answers, staff rules, and AI wording into one patient-owned preparation summary.

## Closing Summary

Use this closing:

> PMS Health turns scattered health information into a structured care-preparation record. It helps the user explain what is happening, what changed, what data matters, and what questions to ask a doctor. The product is safe by design because rules control urgent warnings and AI only improves communication.
