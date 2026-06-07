# PMS Manual Testing Guide

This guide explains how to test PMS from IntelliJ, Swagger, the browser, and the terminal. PMS is a care-preparation and health-awareness system only. Testing should confirm that the app never claims diagnosis, prescription, or emergency service.

## 1. Testing Goals

- Confirm public pages, patient flows, staff flows, and API endpoints work together.
- Confirm safety rules and red-flag warnings are rule-based and cannot be downgraded.
- Confirm mock AI output stays structured, safe, and non-diagnostic.
- Confirm optional provider mode is backend-only, tries Ollama first, uses OpenAI as fallback, validates structured output, and falls back safely to mock.
- Confirm completed results show a compact summary first and reveal long details only after the user expands them.
- Confirm role boundaries: patients use patient endpoints, staff use admin endpoints.
- Confirm negative cases fail safely without exposing stack traces.

## 2. IntelliJ Backend Setup

1. Open IntelliJ IDEA.
2. Choose `File > Open` and select the project root or `backend` folder.
3. Reload Maven when IntelliJ asks, or open the Maven panel and click Reload.
4. Select JDK 17 or newer from `File > Project Structure > Project SDK`.
5. Open `backend/src/main/java/com/pms/backend/BackendApplication.java`.
6. Optional but recommended: add `JWT_SECRET`, `AI_MODE`, and any provider keys to the Run Configuration environment variables instead of hard-coding them.
7. Run the application from the green Run button, or use:

```powershell
cd backend
$env:JWT_SECRET="replace-with-a-long-random-secret-at-least-32-characters"
.\mvnw.cmd spring-boot:run
```

8. Verify the backend is running:

```text
http://localhost:8080/api/health
```

## 3. Running Automated Tests

Backend tests use the H2 test profile, so PostgreSQL is not required for automated backend testing:

```powershell
cd backend
.\mvnw.cmd test "-Dspring.profiles.active=test"
```

Frontend tests and production build:

```powershell
cd frontend
npm test -- --run
npm run build
```

In IntelliJ, you can also run backend tests from the Maven panel:

1. Open Maven panel.
2. Expand `backend > Lifecycle`.
3. Double-click `test`.
4. Check the Run window for failed test names and stack traces.

## 4. Swagger API Testing Flow

Start the backend first, then open:

```text
http://localhost:8080/swagger-ui.html
```

Swagger also exposes:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

Recommended patient flow:

1. Run `GET /api/health`.
2. Run `POST /api/auth/register` with a new adult patient account, or run `POST /api/auth/login` with:

```json
{
  "identifier": "user@example.com",
  "password": "password123"
}
```

3. Copy the returned JWT `token`.
4. Click Swagger `Authorize`.
5. Enter:

```text
Bearer <token>
```

6. Run `GET /api/auth/me`.
7. Run `POST /api/assessments` with one to five symptoms.
8. Copy the returned assessment `id` and answer every returned follow-up question using `Yes`, `No`, or `Not sure`.
9. Run `POST /api/assessments/{id}/follow-ups`.
10. Confirm the completed response includes summary, explanation, possible directions, monitoring plan, doctor questions, trusted links, `aiMode`, and status `COMPLETED`.
11. Run `GET /api/assessments` and confirm the completed item appears in history.

JWT checks in Swagger:

- Use the copied token with `GET /api/auth/me`. Expected: current user is returned.
- Remove the token and run `GET /api/auth/me`. Expected: 4xx response.
- Enter `Bearer not-a-jwt`. Expected: 4xx response with a safe error message.
- Use a patient token on `GET /api/admin/analytics`. Expected: forbidden.
- Use a staff token on `POST /api/assessments`. Expected: rejected because patient access is required.
- Restart the backend and reuse an unexpired token signed with the same `JWT_SECRET`. Expected: token still works because validation is stateless.

Recommended staff flow:

1. Run `POST /api/auth/staff-login`:

```json
{
  "identifier": "admin@example.com",
  "password": "password123"
}
```

2. Authorize with the returned staff token.
3. Run `GET /api/admin/analytics`.
4. Run `GET /api/admin/rules`.
5. Run `POST /api/admin/rules` with a new operational rule.
6. Run `PATCH /api/admin/rules/{id}/active` to pause or activate it.
7. Run `GET /api/admin/questions`.
8. Run `POST /api/admin/questions` with a General or symptom-specific Yes/No-style prompt.
9. Run `POST /api/admin/questions/suggest` to prepare AI draft prompts for staff review.
10. Run `PATCH /api/admin/questions/{id}/active` to pause or activate it.

## 5. Manual Browser Test Flow

Start backend and frontend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run

cd ..\frontend
npm run dev
```

Open:

```text
http://localhost:5173
```

Check these screens:

- Landing: Home, How It Works, Safety, Contact, Privacy, Terms.
- Signup: account details, privacy acknowledgment, terms acceptance.
- Login: patient login and Staff login link.
- Patient overview: profile setup progress and no large Appearance panel.
- Profile setup: every card requires a current answer and disappears after final save.
- Assessment: symptom drawer, severity, duration, temperature availability, chronic condition, follow-up cards, compact completed care guide, and `View full care details` toggle.
- History: clicking a record opens the full assessment report drawer.
- Reports: report follow-ups and report care-preparation guide.
- Staff overview: analytics, compact charts, risk mix, full-width care review.
- Staff rules: create rule, activate/deactivate rule.
- Staff questions: create question, activate/deactivate question.
- Staff AI questions: suggest drafts, save selected drafts as paused, then activate only after review.
- Staff profile: read-only profile information.

## 6. White-Box Testing Pointers

White-box testing checks internal logic with knowledge of the code:

- `AuthService`: adult signup, privacy and terms required, patient login rejects staff, staff login rejects patients, profile completion only reaches 100 percent after explicit setup.
- `AssessmentService`: draft creation, pending resume, discard behavior, follow-up answer validation, completed care guide generation, completed-only history.
- `RiskEngineService`: low, medium, high, red flags, urgent warnings, follow-up refinement, custom rules raising only.
- `MockAiInsightService`: safe wording, structured fields, no diagnosis or prescription claims.
- `ConfiguredAiInsightService`: mock default mode, provider-chain configuration checks, Ollama -> OpenAI -> mock fallback, and rule-owned urgent warnings.
- `OllamaInsightClient`: Ollama Cloud chat request shape, `gemma4:31b` model configuration, structured JSON parsing, invalid provider response handling, and timeout/error fallback through the configured service.
- `OpenAiInsightClient`: OpenAI Responses API request shape, Structured Outputs parsing, invalid provider response handling, and timeout/error fallback through the configured service.
- Admin rules/questions: create, activate, pause, and future assessment matching behavior.

Good techniques:

- Boundary value testing: age 17, 18, 120, 121; severity 0, 1, 10, 11; symptom count 0, 1, 5, 6.
- Decision table testing: patient token vs staff token vs missing token for each endpoint group.
- Path testing: draft created, draft discarded, draft finalized, draft from another user rejected.
- Error testing: invalid follow-up answer, missing consent, duplicate email, wrong password.

## 7. Black-Box Testing Pointers

Black-box testing checks behavior from the user or API perspective without reading code:

- Can a new adult patient create an account only after privacy and terms acceptance?
- Can an underage user create an account? Expected: no.
- Can patient login access staff analytics? Expected: no.
- Can staff login create patient assessments? Expected: no.
- Can an assessment be finalized before every follow-up answer is selected? Expected: no.
- Does a red-flag symptom show urgent warning before follow-ups? Expected: yes.
- Does history hide unfinished drafts? Expected: yes.
- Do staff-created inactive rules or questions affect future assessments? Expected: no.

## 8. API And Security Checklist

Use Swagger or Postman to check:

- Missing bearer token returns a 4xx response for private endpoints.
- Malformed JWT returns a safe 4xx response.
- Expired JWT and wrong-signature JWT cases are covered by backend tests.
- Patient token is rejected from `/api/admin/**`.
- Staff token is rejected from patient-only draft creation.
- One patient cannot finalize or discard another patient's draft.
- More than five symptoms is rejected by validation.
- Invalid follow-up choices are rejected.
- Error responses contain a user-safe message, not stack traces.
- API keys are never present in frontend code or browser storage.
- Mock AI output does not diagnose, prescribe, or make emergency promises.
- Optional provider output cannot create or soften urgent warnings.
- If `AI_MODE=provider` is used without `OLLAMA_API_KEY`, `OPENAI_API_KEY`, or legacy `AI_API_KEY`, the response still succeeds with `aiMode=MOCK`.
- If Ollama fails while OpenAI is configured, the response can return `aiMode=OPENAI`.
- If Ollama succeeds, the response can return `aiMode=OLLAMA`.
- Protected red-flag wording remains rule-based.

JWT automated test checklist:

- Login/register returns a three-part JWT.
- Valid JWT works for `GET /api/auth/me`.
- Missing, malformed, expired, and wrong-signature JWTs are rejected.
- Patient JWT is rejected from admin endpoints.
- Staff JWT is rejected from patient assessment creation.
- Backend restart does not invalidate unexpired tokens when `JWT_SECRET` remains the same.
- `JWT_SECRET` never appears in frontend source, browser storage, screenshots, or committed documentation examples as a real secret.

Compare this checklist with the OWASP API Security Top 10 topics: broken object-level authorization, broken authentication, unrestricted resource consumption, broken function-level authorization, sensitive data exposure, and unsafe API consumption.

## 9. Suggested Learning Path

Start with testing fundamentals:

- ISTQB Foundation or any software testing basics course.
- Black-box testing: equivalence partitioning, boundary value analysis, decision tables, state transition testing.
- White-box testing: statement coverage, branch coverage, path coverage, unit testing.

Backend testing:

- JUnit 5 fundamentals.
- Spring Boot testing with `@SpringBootTest`.
- MockMvc controller testing.
- Bean Validation testing.
- Repository testing with H2.

API testing:

- Swagger/OpenAPI basics.
- Postman API testing.
- Writing API test assertions.
- Authentication and authorization test cases.

Frontend testing:

- React Testing Library.
- Vitest.
- User-event testing.
- Accessibility checks and responsive UI checks.

Security testing:

- OWASP API Security Top 10.
- Basic authentication and authorization testing.
- Input validation and error-handling review.
- Sensitive-data handling basics.

Optional next steps:

- Playwright or Cypress for full browser automation.
- Test coverage tools such as JaCoCo for Java.
- CI with GitHub Actions.
- Performance smoke testing with k6 or JMeter.

## 10. Future Pregnancy Care-Preparation Testing

The pregnancy module is future scope, but test planning should be ready before implementation.

Manual scenarios:

- Pregnant user enters severe headache and vision changes. Expected: urgent maternal warning appears before mock, Ollama, or OpenAI wording.
- Postpartum user enters heavy bleeding, fever, or fainting. Expected: urgent warning remains visible in the compact summary.
- Pregnant user enters nausea without red flags. Expected: PMS asks safe context questions and prepares doctor or midwife discussion notes without diagnosis.
- User selects "Prefer not to say" for pregnancy context. Expected: PMS still supports general assessment and states that missing context may reduce preparation detail.

Negative scenarios:

- PMS must not diagnose preeclampsia, miscarriage, ectopic pregnancy, infection, clot, depression, or any other condition.
- PMS must not estimate fetal health, fetal movement normality, or whether a baby is safe.
- PMS must not prescribe medication, dosage, supplements, bed rest, diet treatment, or exercise treatment.
- PMS must not tell the user to ignore warning signs, wait at home, or delay urgent care.
- Mock or provider wording must not lower or remove Java-owned maternal warning-sign output.
- Ollama provider failures must fall back to OpenAI or mock wording without changing urgent warnings.
- OpenAI provider failures must fall back to mock wording without changing urgent warnings.

Documentation acceptance:

- README links to `docs/pregnancy-care-prep-feature-plan.md`.
- Architecture docs show the pregnancy flow diagram.
- Test cases cite rule-owned safety first and optional provider wording second.

## 11. Demo Test Script

Use this short script before a professor or manager demo:

1. Run backend tests.
2. Run frontend tests.
3. Run frontend build.
4. Start PostgreSQL, backend, and frontend.
5. Open Swagger and confirm health endpoint.
6. Login as patient, create an assessment, answer follow-ups, and verify the completed care guide.
7. Open frontend and repeat the same flow visually.
8. Login as staff and verify analytics, care review, rule creation, and question creation.
9. Explain clearly: rules control safety, mock AI is the default wording layer, optional Ollama/OpenAI wording is backend-only, and PMS does not diagnose or prescribe.
