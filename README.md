# Patient Health Assessment and Tracking System

Patient workspace + clinical operations capstone prototype built with **Java Spring Boot**, **React + TypeScript**, and **PostgreSQL**.

This system helps users enter symptoms, optional temperature details, reports, and health history, then gives a safe care-preparation guide with a compact summary first and expandable full details for reasons, possible directions, monitoring notes, doctor questions, and follow-up questions. It does **not** diagnose disease, prescribe medicine, or replace a doctor.

## Detailed Project Description

The Patient Health Assessment and Tracking System is a capstone-style full-stack web application designed to support safe health-awareness workflows for normal users and administrative reviewers. The project focuses on collecting structured health inputs, processing those inputs through a controlled rule-based risk engine, and presenting the result in a clear and non-diagnostic format. The system is intentionally positioned as an educational and awareness prototype rather than a medical decision-making tool. It helps users organize their symptoms, optional temperature data, reports, and health history before speaking with a qualified medical professional.

The application has two role-appropriate workspaces: the patient health workspace and the clinical operations workspace. The patient workspace is built for people who want to create an account, log in, fill out a health assessment form, review risk-awareness output, answer follow-up prompts, and inspect previous assessment reports. The clinical operations workspace is restricted to staff accounts. Staff users can review assessment records, see analytics, manage upward-only operational safety rules, maintain the guided question bank, and view a read-only staff profile. Patient and staff sessions remain separate so administrative accounts are not treated like patient profiles.

On the patient side, public account creation always creates an adult patient account and collects basic profile information such as age, height, weight, and sex. Height can be entered in centimeters or feet and inches; the API stores centimeters consistently. Signup uses three steps: account details, privacy-notice acknowledgment, and terms acceptance. The backend stores the accepted privacy-notice and terms versions with timestamps for the project record. Staff accounts are provisioned separately and use a dedicated Staff login path. After patient login, users finish a richer health-history setup through progressive mixed-control question cards covering allergies, chronic conditions, medicines, family history, mental health history, sleep quality, and lifestyle. New accounts do not receive invented health-history values. Legacy placeholder-like values require explicit patient review before setup is complete. The assessment form supports selecting up to five symptoms from a grouped searchable symptom drawer. It collects a deliberate severity selection, duration, optional body temperature, and chronic-condition context without assuming default patient answers. The backend saves this intake as a resumable draft, returns four to seven follow-up questions, and generates the structured care-preparation guide only after all follow-ups are answered. Rule-based urgent warnings can still appear immediately before the questions.

The risk engine is deliberately simple, explainable, and controlled. It does not use uncontrolled diagnosis logic, and it does not claim that a user has any specific disease. Instead, it evaluates broad risk indicators such as high severity, long duration, optional abnormal temperature, multiple selected symptoms, chronic-condition context, and red-flag symptom combinations. Protected red-flag checks are rule-based before AI-style wording is generated, so urgent warnings cannot be lowered by generated text. Staff can add structured operational rules that use symptom, severity, duration, and chronic-condition matching. These rules can only raise a score floor and optionally show the standard urgent message; they cannot reduce risk or replace protected emergency wording. Active staff-managed questions can join future assessment drafts when their symptom matches. Follow-up answers can adjust the risk explanation when the user reports concerning details such as breathing difficulty, fainting, chest pain, confusion, bleeding, vomiting, or vision changes. The output stays in the language of care preparation: it explains why the entered data may need attention, gives non-diagnostic directions to discuss, and encourages professional consultation when symptoms are serious or persistent.

The frontend is implemented with React, TypeScript, Vite, Recharts, Lucide icons, and CSS custom properties. The current branch refactors the earlier single-file frontend into component and section files. This makes the code easier to understand, test, and extend. Authentication pages live under `pages/auth`, user-facing sections live under `pages/user`, admin-facing sections live under `pages/admin`, and shared interface elements live under `components`. The public landing page now keeps only public product information on the first screens, with Home, How It Works, Safety, and Contact sections. User profile details, admin information, assessment history, and private health data remain inside authenticated screens only. Shared TypeScript types, API helpers, static data, and formatting utilities are separated into `types.ts`, `api.ts`, `data.ts`, and `utils.ts`.

The backend is implemented with Java Spring Boot, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL, and an H2 test profile. PostgreSQL is used for normal local runtime on a development system. The H2 profile is used only for automated backend tests so the core Spring context can be validated without requiring a running database server. The backend creates and manages users, assessments, report insight requests, rules, questions, and analytics through controller, service, repository, DTO, and model layers. The current AI layer is a backend-owned mock `AiInsightService`; the frontend does not call AI providers or store AI keys.

Authentication uses signed JWT access tokens. After patient login, staff login, or patient signup, the backend returns a JWT in the existing `token` field and the frontend sends it in the `Authorization: Bearer <token>` header. The JWT subject is the user ID, and signed claims include role, username, and email. The backend validates token signature, expiry, issuer, and user existence on every protected request. Admin access still checks the current database role, not only the JWT claim. Public registration always creates a patient, accepts self-registration for adults age 18 and older, and requires privacy-notice and terms acknowledgment. `POST /api/auth/login` accepts patient accounts only, while `POST /api/auth/staff-login` accepts staff accounts only. Patients can see only their own completed assessments. Staff users can access clinical-operations analytics and completed assessment records. This is an access-token prototype without refresh tokens; production deployment would still need rate limiting, audit logging, stronger operational controls, and a full security review.

The system avoids Docker by design in the current branch. It is intended to run on a normal development system with IntelliJ IDEA or VS Code, Java 17 or later, Maven, Node.js, and a locally installed PostgreSQL server. The backend connection defaults to `pms_db`, `pms_user`, and `pms_password`, but these can be changed using environment variables. This makes the project easier to run on college lab systems, personal laptops, or another development machine where Docker Desktop is not installed.

Testing is part of the current project structure. Backend testing verifies that the Spring Boot application context loads successfully with the H2 test profile, public registration creates adult patient accounts with acknowledgment metadata, patient and staff login paths remain isolated, draft assessments stay outside completed analytics, red-flag warnings remain immediate, risk rules remain predictable, and mock insight output stays structured. Frontend testing uses Vitest and Testing Library to render every major section and verify the legal signup steps, blank intake controls, grouped symptom library, pending follow-up gate, labeled profile fields, and completed care-preparation guide. The project also supports a production frontend build using TypeScript and Vite.

Overall, the project demonstrates a complete MVP foundation for a patient health-awareness system. It combines JWT authentication, role-based screens, structured health input, explainable risk scoring, backend-owned mock AI care-prep wording, frontend validation, backend validation, persistence, analytics, reusable frontend components, and documentation. Future improvements could include a production AI provider adapter, refresh tokens, real PDF extraction, appointment booking, doctor dashboards, multilingual support, richer test coverage, and deployment configuration. Even with those future possibilities, the current scope remains intentionally safe: synthetic/demo data only, no diagnosis, no prescription, and no emergency decision-making.

## Medical Disclaimer

This application does not provide medical diagnosis, treatment, prescription, or emergency service. It is only for educational and health-awareness purposes. For serious symptoms or emergencies, consult a qualified medical professional immediately.

## Current Branch

Use this branch for continuing development:

```text
PMS_Test2
```

This branch contains the Spring Boot backend, React TypeScript frontend, polished public landing/auth UI, sticky public and workspace headers, public Safety, Contact, Privacy, and Terms sections, adult patient-only three-step signup, dedicated Staff login, explicit profile setup flow, dual-unit height input, editable labeled patient profile, grouped multi-symptom assessments, resumable draft follow-ups, compact summary-first care-preparation results, reusable completed-report drawers, clinical-operations analytics, operational safety-rule management, managed assessment questions, a read-only staff profile, backend-owned mock AI care-prep guides, report insight endpoints, validation, and a componentized frontend structure.

## Presentation Decks

Two editable PowerPoint decks are included in the project root:

```text
PMS-Health-Professional-Demonstration.pptx
PMS-Health-Normal-Presentation.pptx
```

`PMS-Health-Professional-Demonstration.pptx` is the polished MNC-style deck for formal review. `PMS-Health-Normal-Presentation.pptx` is a simpler classroom-style deck with a plain blue header, white background, and direct bullet structure. Both decks cover the project snapshot, problem statement, solution overview, system architecture, demo flow, and future scope. Both also clearly state that the current AI behavior is mock AI only.

## Importing The Project On Another System

Use these steps when opening the project on a new laptop or lab system:

```powershell
git clone https://github.com/VAMSIBALAKIRANPOLISETTY/PMS_PROJECT.git
cd PMS_PROJECT
git checkout PMS_Test2
```

Backend import in IntelliJ IDEA:

1. Open IntelliJ IDEA.
2. Choose `File > Open` and select the `backend` folder, or open the project root and select the backend Maven project.
3. Let IntelliJ import Maven dependencies from `backend/pom.xml`.
4. Select a JDK 17 or newer SDK.
5. Create the local PostgreSQL database using the SQL commands in this README.
6. Run the Spring Boot application from the main backend application class, or use:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend import in VS Code:

1. Open the `frontend` folder in VS Code.
2. Install dependencies and start Vite:

```powershell
cd frontend
npm install
npm run dev
```

Presentation import:

- Open `PMS-Health-Professional-Demonstration.pptx` directly in Microsoft PowerPoint.
- Open `PMS-Health-Normal-Presentation.pptx` if you want the simpler classroom-style version.
- For Google Slides, use `File > Import slides` or upload the PPTX to Google Drive and open it with Google Slides. The deck is built with editable text and shapes, so labels and diagrams can be adjusted for the final demo.

## System Architecture

PMS follows a layered full-stack architecture:

```text
React + TypeScript Frontend
  -> Vite API Proxy
  -> Spring Boot REST API
  -> Backend Services
       - AuthService
       - AssessmentService
       - RiskEngineService
       - AiInsightService
       - ReportInsightController
  -> JPA Repositories
  -> PostgreSQL
```

The frontend is responsible for public pages, patient screens, staff screens, form validation, authenticated API calls, and rendering the care-preparation guide. It does not own medical-safety decisions and it does not contact any AI provider.

The Vite development server proxies `/api` calls to the Spring Boot backend. By default it targets `http://localhost:8080`, and `VITE_API_PROXY_TARGET` can override the backend URL for side-by-side branch testing. Spring Boot owns authentication, role separation, assessment drafts, follow-up finalization, report insights, staff rule management, staff question management, analytics, and all persistence logic. JPA repositories store users, assessments, rules, questions, and related records in PostgreSQL for normal local runtime. Backend tests use the H2 profile so automated checks can run without a PostgreSQL server.

The staff-side `Operational Rules` and `Question Bank` feed future assessment drafts through the backend service layer. Staff rules are upward-only safeguards: they can raise a risk score floor when configured conditions match, but they cannot lower risk or disable protected red-flag warnings. Staff questions can join future guided assessments when active and symptom-matched while the backend keeps the final follow-up set controlled.

Detailed diagrams and architecture notes are available at `docs/architecture.md`. Future pregnancy care-preparation architecture is described in `docs/pregnancy-care-prep-feature-plan.md`.

For a short speaking sheet before a professor or manager demo, use `docs/demo-quick-summary.md`.

## JWT Authentication

PMS now uses signed JWT access tokens instead of the earlier in-memory token map.

- Login, staff login, and registration still return `AuthResponse { token, user }`.
- The browser still sends `Authorization: Bearer <token>` for private API calls.
- JWT subject is the database user ID.
- JWT claims include role, username, and email for traceability.
- The backend validates signature, expiry, issuer, and user existence for every protected request.
- Staff authorization still reads the current database role through `AuthService.requireAdmin`, so changing a staff role in the database is respected even if an older token contains a stale role claim.
- The current access token expires after 12 hours by default.
- No refresh-token flow is included in this pass.

JWT environment variables:

```powershell
$env:JWT_SECRET="replace-with-a-long-random-secret-at-least-32-characters"
$env:JWT_ISSUER="PMS Health"
$env:JWT_EXPIRATION_HOURS="12"
```

For local demos, the backend has a development fallback secret. For any shared, deployed, or production-like run, set `JWT_SECRET` yourself and do not commit it.

## AI Usage Clarification

Short answer for professor or manager demo: **PMS currently uses mock AI, not real external AI.**

- No external AI API is called in the current branch.
- The frontend never stores AI provider keys and never sends requests directly to an AI provider.
- `AiInsightService` is the backend interface for AI-style care-preparation output.
- `MockAiInsightService` is the active implementation for predictable demo and test output.
- The mock service generates plain-language care summaries, explanations, possible directions, monitoring notes, doctor-prep questions, trusted source links, and report insight wording.
- `RiskEngineService` remains the safety source of truth. Protected red flags and urgent warnings are rule-based before AI-style wording is generated.
- A real AI provider can be added later through a backend-only provider adapter using environment variables such as `AI_MODE`, `AI_PROVIDER`, `AI_API_KEY`, and `AI_MODEL`.

This means PMS demonstrates the complete AI integration architecture without sending real health data to an external model. In a production version, provider configuration, prompt validation, output validation, privacy review, logging rules, retention rules, and security controls would need to be designed before real patient data is used.

## Required Software

Install these on the development system:

- IntelliJ IDEA or VS Code
- Git
- Java JDK 17 or later
- Maven or the included Maven wrapper
- Node.js 20 or later
- PostgreSQL 16 or compatible local PostgreSQL server
- Postman or Thunder Client, optional for API testing

Docker is not required and is not used by this project.

## Project Structure

```text
PMS/
  backend/
    pom.xml
    mvnw.cmd
    src/main/java/com/pms/backend/
      config/
      controller/
      dto/
      model/
      repository/
      service/
    src/main/resources/
      application.properties

  frontend/
    index.html
    package.json
    vite.config.js
    src/
      main.tsx
      App.tsx
      MainContent.tsx
      api.ts
      data.ts
      types.ts
      utils.ts
      components/
      pages/
        admin/
        auth/
        user/
      __tests__/
      styles.css

  docs/
    architecture.md
    pregnancy-care-prep-feature-plan.md
    testing-guide.md
```

## Database Setup Without Docker

Create a local PostgreSQL database and user:

```sql
CREATE DATABASE pms_db;
CREATE USER pms_user WITH PASSWORD 'pms_password';
GRANT ALL PRIVILEGES ON DATABASE pms_db TO pms_user;
```

If PostgreSQL uses stricter schema permissions, connect to `pms_db` as a superuser and run:

```sql
GRANT ALL ON SCHEMA public TO pms_user;
ALTER SCHEMA public OWNER TO pms_user;
```

Default backend connection:

```text
Database: pms_db
Username: pms_user
Password: pms_password
Host: localhost
Port: 5432
```

You can override the defaults with environment variables:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/pms_db"
$env:DATABASE_USERNAME="pms_user"
$env:DATABASE_PASSWORD="pms_password"
$env:CORS_ORIGIN="http://localhost:5173"
$env:JWT_SECRET="replace-with-a-long-random-secret-at-least-32-characters"
$env:JWT_ISSUER="PMS Health"
$env:JWT_EXPIRATION_HOURS="12"
$env:AI_MODE="mock"
$env:AI_PROVIDER=""
$env:AI_API_KEY=""
$env:AI_MODEL=""
```

## Run Backend

From the project root:

```powershell
cd backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-20"
.\mvnw.cmd spring-boot:run
```

Use your actual JDK path if different. The app sets the JVM default time zone to `Asia/Kolkata` at startup so PostgreSQL does not reject older Windows time zone IDs such as `Asia/Calcutta`.

Backend URL:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/api/health
```

## Swagger / OpenAPI

Swagger is built into the Spring Boot backend so all endpoints can be viewed and tested from one page.

Start the backend, then open:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI files:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

Recommended Swagger flow:

1. Run `GET /api/health` first.
2. Login as a patient using `POST /api/auth/login`, or login as staff using `POST /api/auth/staff-login`.
3. Copy the returned JWT `token`.
4. Click Swagger `Authorize`.
5. Enter `Bearer <token>`.
6. Test patient endpoints with a patient token and admin endpoints with a staff token.

Swagger documents the PMS API as a care-preparation API only. It does not change endpoint behavior, and it does not make PMS a diagnosis, prescription, or emergency service.

## Run Frontend

Open another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

The Vite proxy forwards `/api` requests to:

```text
http://localhost:8080
```

For side-by-side branch testing, override the proxy target before starting Vite:

```powershell
$env:VITE_API_PROXY_TARGET="http://localhost:8082"
npm run dev -- --host 127.0.0.1 --port 5172
```

## Demo Accounts

The backend seeds these accounts when the database is empty:

```text
User:
Email: user@example.com
Username: anaya
Password: password123

Admin:
Email: admin@example.com
Username: admin
Password: password123
```

New patients can sign up from the landing page. Staff should use the separate `Staff login` link on the login screen.

## Demo Presentation Guide

Use this flow when presenting the project:

1. Open the landing page and explain the product boundary: PMS is a care-preparation assistant, not a diagnosis or treatment system.
2. Show login/signup. Mention that public signup creates adult patient accounts only, collects basic profile context, and records privacy-notice and terms acknowledgments before account creation.
3. Login as the demo user and show the profile setup card. Explain that question-specific controls require reviewed answers before the panel disappears and that richer health history helps prepare better follow-up questions and doctor notes.
4. Open the assessment page. Select up to five symptoms from the grouped symptom drawer, choose severity, enter duration, choose temperature availability, and select chronic-condition context.
5. Click Prepare care guide. Explain that the backend saves a resumable pending draft and runs rule-based safety checks immediately.
6. Answer every follow-up card using Yes, No, or Not sure, with an optional note. Mention that red-flag warnings can appear before the guide and are never delayed by the questions.
7. Submit the answers and show the compact result first: risk, score, urgent warning if present, and Summary. Click `View full care details` to expand Why this matters, Possible directions to discuss, What to do next, Doctor questions, and Trusted sources.
8. Open Reports. Upload a PDF name, paste report notes or abnormal values, answer follow-up cards, and generate the report care-prep guide.
9. Open History and click an assessment record to show the reusable full care-preparation report drawer.
10. Log out, use the separate Staff login link, and sign in with the demo admin account. Show analytics, full-width assessment records, the same report drawer, upward-only safety-rule management, managed assessment questions, and the read-only staff profile.
11. Explain future pregnancy support as a safe care-preparation extension: PMS could collect pregnancy or postpartum context, check protected maternal warning signs with rules, and help users prepare questions for a doctor or midwife without diagnosing.

Suggested short explanation:

```text
PMS helps users organize symptoms, health history, and report notes before they speak to a doctor. It does not diagnose. The backend uses rules for safety and red flags, then a mock AI insight service creates plain-language care-preparation output. The user receives a compact summary first and can expand directions, monitoring notes, doctor questions, and trusted source links instead of only seeing a Low, Medium, or High label.
```

Pregnancy-focused future scope:

```text
PMS can be extended to support pregnant and postpartum users by collecting optional pregnancy context, checking urgent maternal warning signs with protected rules, and preparing a clear summary for a doctor or midwife. In PMS_Test2 this would remain mock AI wording only; all maternal safety decisions would stay rule-based.
```

## Where Key Functions Are Used

- `AuthService`: handles adult patient-only registration, stored privacy and terms acknowledgments, separate patient and staff login paths, JWT-backed user lookup, database role checks, explicit health-history completion, and profile-completion percentage.
- `JwtService`: creates signed JWT access tokens and validates bearer token signature, issuer, expiry, and user ID before protected endpoints use the current user.
- `AssessmentService`: saves pending intake drafts, resumes or discards patient-owned drafts, finalizes completed records after follow-up answers, and keeps unfinished drafts outside normal history.
- `RiskEngineService`: calculates score, Low / Medium / High level, reasons, follow-up questions, suggestions, protected urgent warnings, upward-only operational rule matches, and symptom-matched managed questions.
- `AiInsightService`: backend interface for AI-style care-preparation output. The frontend never calls AI providers directly.
- `MockAiInsightService`: current development/mock implementation that generates care summary, explanation, possible directions, monitoring plan, doctor questions, trusted links, and report insights.
- `ReportInsightController`: exposes backend report follow-up and report insight endpoints.
- `CarePrepGuide`: shared React component that renders compact summary-first care-preparation results for assessments and reports, with a button to expand or hide full details.
- `AssessmentReportDrawer`: shared React component that opens completed patient and staff assessment records without leaving the current screen.
- `AssessmentForm`: collects symptoms, severity, duration, temperature availability, chronic condition, follow-up answers, and displays the care-prep guide.
- `Reports`: collects report file name, pasted report notes, report follow-up answers, and displays the report care-prep guide.

## Expected Manager Questions

- What problem does PMS solve?
  It helps users prepare better health information before consulting a doctor by organizing symptoms, reports, follow-up answers, and safe next-step questions.

- Does the system diagnose disease?
  No. It gives care-preparation guidance only. It does not diagnose, prescribe medicine, or replace a doctor.

- Where is AI used?
  AI is represented through the backend `AiInsightService`. Currently the app uses `MockAiInsightService` for predictable demo output. A real provider can be added later behind the backend using environment variables.

- Did we use JWT?
  Yes. Login, staff login, and signup return signed JWT access tokens. The frontend stores the token and sends it as `Authorization: Bearer <token>`. The backend validates the signature and expiry, then still checks the current database role.

- Why use rules plus AI instead of only AI?
  Health safety needs predictable guardrails. Rule-based red-flag checks handle urgent warnings first, and AI-style output only improves explanation and wording.

- What happens if a user enters chest pain or breathing difficulty?
  `RiskEngineService` detects red-flag patterns and returns an urgent warning. Generated wording cannot lower that warning.

- Why do reports ask follow-up questions?
  Report values need context. The system asks about symptoms, abnormal values, medicines, chronic conditions, and doctor review before creating a safe preparation guide.

- How can PMS help pregnant women in the future?
  PMS can add optional pregnancy and postpartum context, pregnancy-aware symptom groups, protected maternal warning-sign rules, and doctor or midwife preparation notes. It would not diagnose pregnancy complications, estimate fetal condition, prescribe medicine, or replace urgent care.

- How is user privacy handled in this prototype?
  Adult signup records the accepted privacy-notice and terms versions with timestamps. Normal users can see only their own completed assessments, and unfinished drafts stay private. Admin users can view completed records for demo analytics. This is still a capstone prototype, not a HIPAA-certified production system.

- What database and stack are used?
  Spring Boot, Spring Data JPA, PostgreSQL for local runtime, H2 for tests, React TypeScript, Vite, Recharts, and CSS.

- How would real AI be added later?
  Add a provider-backed implementation of `AiInsightService`, configure `AI_MODE`, `AI_PROVIDER`, `AI_API_KEY`, and `AI_MODEL`, validate structured JSON output, and keep the existing rule engine as the safety layer.

- What is the current limitation?
  It is a capstone MVP using mock AI output and synthetic/demo data. It is not ready for real patient data or production medical use.

## API Endpoints

Base URL:

```text
http://localhost:8080/api
```

Important endpoints:

```http
GET  /api/health
POST /api/auth/register
POST /api/auth/login
POST /api/auth/staff-login
GET  /api/auth/me
PUT  /api/auth/profile
GET  /api/assessments
POST /api/assessments
GET  /api/assessments/pending
POST /api/assessments/{id}/follow-ups
DELETE /api/assessments/{id}/draft
POST /api/reports/follow-ups
POST /api/reports/insight
GET  /api/admin/analytics
GET  /api/admin/rules
POST /api/admin/rules
PATCH /api/admin/rules/{id}/active
GET  /api/admin/questions
POST /api/admin/questions
PATCH /api/admin/questions/{id}/active
```

Authenticated requests use:

```text
Authorization: Bearer <token>
```

The token is a signed JWT access token returned by registration, patient login, or staff login.

Normal users only see their own completed assessments. Admin users can see completed assessments and admin analytics. Pending drafts remain private to the patient until finalized or discarded.

### How Staff Rules Work

`Add rule` creates an operational safety rule for future patient assessments. Staff choose one required symptom and optional matching conditions such as a second symptom, minimum severity, minimum duration, or chronic-condition keyword. Every configured condition must match. A matched active rule applies `max(currentScore, configuredScoreFloor)` and may show the standard urgent message. It cannot lower risk, disable protected red flags, or introduce custom emergency wording.

### How Managed Questions Work

`Create question` adds a Yes / No / Not sure compatible follow-up prompt for a specific symptom or `General`. Active symptom-matched questions can join future assessment drafts while the backend keeps the final follow-up set between four and seven questions. Paused questions remain visible to staff but do not enter new patient drafts.

## Frontend Sections

The frontend is split by section:

- `pages/auth`: landing, legal notice, login, and three-step signup pages
- `pages/user`: user overview, assessment form, symptom drawer, reports, history, profile, recent assessments
- `pages/admin`: clinical operations overview, full-width assessment table, operational safety rules, managed assessment questions, and read-only staff profile
- `components`: shared layout, care-preparation guide, and reusable UI pieces
- `types.ts`, `api.ts`, `data.ts`, `utils.ts`: shared TypeScript contracts and helpers

## Validation And Testing

Backend test profile uses H2 in-memory database, so backend tests do not require PostgreSQL:

```powershell
cd backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-20"
.\mvnw.cmd test "-Dspring.profiles.active=test"
```

Frontend section tests render every major auth, user, admin, and layout section:

```powershell
cd frontend
npm test
```

Frontend production build:

```powershell
cd frontend
npm run build
```

Recommended full validation before pushing:

```powershell
cd backend
.\mvnw.cmd test "-Dspring.profiles.active=test"

cd ..\frontend
npm test -- --run
npm run build
```

A detailed manual testing guide is available at `docs/testing-guide.md`. It covers IntelliJ setup, Swagger testing, patient and staff API flows, browser testing, white-box testing, black-box testing, pregnancy future-scope testing, negative testing, security-focused checks, and suggested learning/course pointers.

## Current Tested Status

Validated on `PMS_Test2`:

- Backend Spring context, JWT patient/staff authentication, risk engine, and mock AI insight tests passed with H2 test profile
- Backend controller/API tests cover OpenAPI docs, patient assessment flow, staff authorization boundaries, admin rules, admin questions, missing-token checks, underage signup, invalid follow-up answers, and draft discard behavior
- Frontend patient/staff auth, section, compact care-prep guide, and expanded drawer test suite passed
- Frontend TypeScript and Vite production build passed
- Runtime PostgreSQL path documented for local PostgreSQL, no Docker

## Current AI And Future Provider Plan

The current project does not call a real external AI model. It uses controlled prototype logic: the backend risk engine is rule-based, and the backend-owned `MockAiInsightService` generates care-preparation wording for assessments and reports. This is intentional for the current branch because it keeps the system predictable, testable, and safe while the main full-stack structure is being completed.

The mock insight behavior has been moved behind backend endpoints. The React frontend never calls an AI provider directly and never stores provider API keys. Instead, the frontend submits symptoms, optional temperature data, report text or report notes, health-history answers, and follow-up answers to Spring Boot endpoints. The Spring Boot backend calls the internal `AiInsightService`, which currently has a local mock implementation for tests and development.

Current implementation and next phases:

1. Backend DTOs now expose structured care-prep fields so model-style output is predictable.
2. `AiInsightService` now owns assessment and report insight generation in the backend service layer.
3. `MockAiInsightService` is used for tests and development so automated checks do not require internet or paid API access.
4. A provider-backed implementation can be added later, configured only through environment variables such as `AI_MODE`, `AI_PROVIDER`, `AI_API_KEY`, and `AI_MODEL`.
5. Report summary generation and assessment insight refresh logic stay behind backend endpoints.
6. AI-style output returns care summary, explanation, possible directions, urgent warning, monitoring plan, doctor questions, trusted source links, and four to seven follow-up questions.
7. The existing rule engine remains the safety layer. Protected hard-coded red flags cannot be disabled or lowered. Staff-created operational rules are additive score floors only. AI can improve wording and ask better follow-up questions, but it must not independently diagnose disease, prescribe medicine, or make emergency decisions.
8. Tests now cover the risk engine, red-flag urgent warnings, mock AI structured output, and frontend rendering of the care-prep guide.

The production AI version should follow strict safety rules. The prompt and backend validation must tell the AI that PMS is only a health-awareness assistant. It must avoid disease diagnosis, medication instructions, dosage advice, emergency triage promises, or statements that replace a doctor. If the user enters severe symptoms such as chest pain, breathing difficulty, fainting, confusion, heavy bleeding, or sudden weakness, the system should show safe guidance to seek urgent professional care. Real patient data should not be used with an AI provider until privacy, consent, logging, retention, and compliance requirements are designed properly.

This plan keeps the current project stable while making the AI upgrade clear: mock logic stays active for development and tests in `PMS_Test2`, any future real AI would live behind the backend, and medical-safety boundaries remain part of the system design.

## Pregnancy Care-Preparation Future Plan

A dedicated future-plan document is available at `docs/pregnancy-care-prep-feature-plan.md`.

The pregnancy extension would help pregnant and postpartum users prepare for care by collecting optional pregnancy context, organizing symptoms, checking protected maternal warning signs, and generating a compact care-preparation summary for a doctor or midwife conversation. It would not diagnose pregnancy complications, estimate fetal condition, provide fetal monitoring, prescribe medication, replace prenatal care, or replace emergency services.

The plan is based on official and reputable maternal-health resources:

- [CDC Hear Her urgent maternal warning signs](https://www.cdc.gov/hearher/maternal-warning-signs/index.html)
- [WHO antenatal care recommendations](https://www.who.int/publications/i/item/9789241549912/)
- [ACOG pregnancy patient education](https://www.acog.org/womens-health/pregnancy/during-pregnancy)
- [March of Dimes pregnancy resources](https://www.marchofdimes.org/find-support/topics/pregnancy)

For `PMS_Test2`, pregnancy-support wording remains a future mock-AI demonstration only. The rule engine would own maternal warning signs, urgent warnings, and all safety boundaries.

## Privacy And Production Review

PMS currently records a privacy-notice acknowledgment and terms acceptance for the project workflow. It does not claim hospital, HIPAA, or regulatory status. A production health application would need a dedicated privacy and legal review, including data retention, breach response, logging, access controls, and any applicable notice requirements.

Useful official references:

- [HHS HIPAA covered entities guidance](https://www.hhs.gov/hipaa/for-professionals/covered-entities/index.html)
- [HHS Notice of Privacy Practices guidance](https://www.hhs.gov/hipaa/for-professionals/privacy/guidance/privacy-practices-for-protected-health-information/index.html)
- [FTC Health Breach Notification Rule guidance](https://www.ftc.gov/business-guidance/resources/complying-ftcs-health-breach-notification-rule)

## Security Scope

Included for prototype:

- Password hashing
- Signed JWT access tokens
- Separate patient and staff login paths
- Adult patient-only public registration
- Stored privacy-notice and terms acknowledgments
- Role-based admin checks
- User-owned pending drafts and completed-assessment filtering
- Frontend and backend validation
- Synthetic demo seed data

Not included:

- Medical diagnosis
- HIPAA-grade compliance
- Production identity management
- Real patient data handling
- Emergency workflow
- Advanced audit logging

This project must not be used with real patient data without major security and compliance upgrades.
