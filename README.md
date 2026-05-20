# Patient Health Assessment and Tracking System

Admin Web + User PWA capstone prototype built with **Java Spring Boot**, **React + TypeScript**, and **PostgreSQL**.

This system helps users enter symptoms, optional temperature details, reports, and health history, then gives a safe **Low / Medium / High** awareness result with reasons and follow-up questions. It does **not** diagnose disease, prescribe medicine, or replace a doctor.

## Detailed Project Description

The Patient Health Assessment and Tracking System is a capstone-style full-stack web application designed to support safe health-awareness workflows for normal users and administrative reviewers. The project focuses on collecting structured health inputs, processing those inputs through a controlled rule-based risk engine, and presenting the result in a clear and non-diagnostic format. The system is intentionally positioned as an educational and awareness prototype rather than a medical decision-making tool. It helps users organize their symptoms, optional temperature data, reports, and health history before speaking with a qualified medical professional.

The application has two major workspaces: the User PWA workspace and the Admin Web Dashboard. The User PWA workspace is built for patients or normal users who want to create an account, log in, fill out a health assessment form, review risk awareness output, answer follow-up prompts, and track their previous assessment history. The Admin Web Dashboard is built for administrative review of the system's synthetic/demo data. Admin users can review assessment records, see analytics, inspect risk rules, view the dynamic question bank, and understand the dataset plan used for testing.

On the user side, account creation collects basic profile information such as age, height, weight, and sex. After login, users can finish a richer health-history setup through progressive question cards covering allergies, chronic conditions, medicines, family history, mental health or psychiatric history, sleep quality, and lifestyle. The assessment form now supports selecting up to five symptoms from an in-page symptom drawer. It collects severity, duration, optional body temperature, and chronic condition context without asking for oxygen level or heart rate because those values are often not immediately available to normal users. After submission, the backend returns a risk score, a Low / Medium / High risk level, reasons for that level, safe suggestions, and four to seven follow-up questions. Users can answer those follow-ups and refresh the assessment insight.

The risk engine is deliberately simple, explainable, and controlled. It does not use uncontrolled diagnosis logic, and it does not claim that a user has any specific disease. Instead, it evaluates broad risk indicators such as high severity, long duration, optional abnormal temperature, multiple selected symptoms, chronic-condition context, and red-flag symptom combinations. Follow-up answers can adjust the risk explanation when the user reports concerning details such as breathing difficulty, fainting, chest pain, confusion, bleeding, vomiting, or vision changes. The output stays in the language of health awareness: it explains why the entered data may need attention and encourages professional consultation when symptoms are serious or persistent.

The frontend is implemented with React, TypeScript, Vite, Recharts, Lucide icons, and CSS custom properties. The current branch refactors the earlier single-file frontend into component and section files. This makes the code easier to understand, test, and extend. Authentication pages live under `pages/auth`, user-facing sections live under `pages/user`, admin-facing sections live under `pages/admin`, and shared interface elements live under `components`. The public landing page now keeps only public product information on the first screens, with Home, How It Works, Safety, and Contact sections. User profile details, admin information, assessment history, and private health data remain inside authenticated screens only. Shared TypeScript types, API helpers, static data, and formatting utilities are separated into `types.ts`, `api.ts`, `data.ts`, and `utils.ts`.

The backend is implemented with Java Spring Boot, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL, and an H2 test profile. PostgreSQL is used for normal local runtime on a development system. The H2 profile is used only for automated backend tests so the core Spring context can be validated without requiring a running database server. The backend creates and manages users, assessments, rules, questions, and analytics through controller, service, repository, DTO, and model layers.

Authentication in this prototype uses a simple token-based approach. After login or signup, the backend returns a token and the frontend sends it in the `Authorization: Bearer <token>` header. The backend uses that token to identify the current user and apply role-based behavior. Normal users can see only their own assessments. Admin users can access admin analytics and all assessment records. This is appropriate for a capstone prototype, but production deployment would require a stronger security model such as Spring Security with JWT signing, refresh tokens, rate limiting, audit logging, and stronger operational controls.

The system avoids Docker by design in the current branch. It is intended to run on a normal development system with IntelliJ IDEA or VS Code, Java 17 or later, Maven, Node.js, and a locally installed PostgreSQL server. The backend connection defaults to `pms_db`, `pms_user`, and `pms_password`, but these can be changed using environment variables. This makes the project easier to run on college lab systems, personal laptops, or another development machine where Docker Desktop is not installed.

Testing is part of the current project structure. Backend testing verifies that the Spring Boot application context loads successfully with the H2 test profile. Frontend testing uses Vitest and Testing Library to render every major section: landing page, auth page, layout controls, user overview, assessment form, symptom drawer, reports, history, profile, recent assessments, admin overview, assessment table, rules, questions, datasets, and design picker. The project also supports a production frontend build using TypeScript and Vite.

Overall, the project demonstrates a complete MVP foundation for a patient health-awareness system. It combines authentication, role-based screens, structured health input, explainable risk scoring, frontend validation, backend validation, persistence, analytics, reusable frontend components, and documentation. Future improvements could include a production AI insight service, a production security model, real PDF extraction, appointment booking, doctor dashboards, multilingual support, richer test coverage, and deployment configuration. Even with those future possibilities, the current scope remains intentionally safe: synthetic/demo data only, no diagnosis, no prescription, and no emergency decision-making.

## Medical Disclaimer

This application does not provide medical diagnosis, treatment, prescription, or emergency service. It is only for educational and health-awareness purposes. For serious symptoms or emergencies, consult a qualified medical professional immediately.

## Current Branch

Use this branch for continuing development:

```text
PMS_Test1
```

This branch contains the Spring Boot backend, React TypeScript frontend, dynamic public landing/auth UI, landing Safety and Contact sections, profile setup flow, editable user profile, multi-symptom assessments, user-owned records, admin analytics, follow-up questions, validation, and a componentized frontend structure.

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

New users can also sign up from the landing page.

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
GET  /api/auth/me
PUT  /api/auth/profile
GET  /api/assessments
POST /api/assessments
POST /api/assessments/{id}/follow-ups
GET  /api/admin/analytics
GET  /api/admin/rules
POST /api/admin/rules
GET  /api/admin/questions
```

Authenticated requests use:

```text
Authorization: Bearer <token>
```

Normal users only see their own assessments. Admin users can see all assessments and admin analytics.

## Frontend Sections

The frontend is split by section:

- `pages/auth`: landing and login/signup pages
- `pages/user`: user overview, assessment form, symptom drawer, reports, history, profile, recent assessments
- `pages/admin`: admin overview, assessment table, rules, questions, datasets
- `components`: shared layout and reusable UI pieces
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
npm test
npm run build
```

## Current Tested Status

Validated on `PMS_Test1`:

- Backend Spring context test passed with H2 test profile
- Frontend section test suite passed
- Frontend TypeScript and Vite production build passed
- Runtime PostgreSQL path documented for local PostgreSQL, no Docker

## Future AI Integration Plan

The current project does not call a real external AI model. It uses controlled prototype logic: the backend risk engine is rule-based, and any report or insight behavior in the frontend is treated as demo/mock behavior for the capstone MVP. This is intentional for the current branch because it keeps the system predictable, testable, and safe while the main full-stack structure is being completed.

In the future version, the mock insight behavior should be removed from the frontend and replaced with a backend-owned AI service. The React frontend should never call an AI provider directly and should never store provider API keys. Instead, the frontend should submit symptoms, optional temperature data, report text, health-history answers, and follow-up answers to Spring Boot endpoints. The Spring Boot backend should then call an internal service such as `AiInsightService`, which can have two implementations: a local mock implementation for tests and development, and a real provider implementation for production-style AI insight generation.

The AI service should be added in phases:

1. Create backend DTOs for AI input and AI output so the model response is structured and predictable.
2. Add an `AiInsightService` interface in the backend service layer.
3. Keep a `MockAiInsightService` for tests so automated checks do not require internet or paid API access.
4. Add a provider-backed implementation later, configured only through environment variables such as `AI_PROVIDER`, `AI_API_KEY`, and `AI_MODEL`.
5. Move report summary generation and assessment insight refresh logic from frontend mock behavior into backend endpoints.
6. Require the AI output to return JSON fields such as summary, key observations, safe suggestions, risk explanation, and four to seven follow-up questions.
7. Keep the existing rule engine as a safety layer. AI can improve wording and ask better follow-up questions, but it should not independently diagnose disease, prescribe medicine, or make emergency decisions.
8. Add tests for the mock AI service, provider fallback behavior, invalid AI output, missing API keys, and frontend rendering of loading, success, and error states.

The production AI version should follow strict safety rules. The prompt and backend validation must tell the AI that PMS is only a health-awareness assistant. It must avoid disease diagnosis, medication instructions, dosage advice, emergency triage promises, or statements that replace a doctor. If the user enters severe symptoms such as chest pain, breathing difficulty, fainting, confusion, heavy bleeding, or sudden weakness, the system should show safe guidance to seek urgent professional care. Real patient data should not be used with an AI provider until privacy, consent, logging, retention, and compliance requirements are designed properly.

This plan keeps the current project stable while making the AI upgrade clear: mock logic stays only for development and tests, real AI lives behind the backend, and medical-safety boundaries remain part of the system design.

## Security Scope

Included for prototype:

- Password hashing
- Simple token authentication
- Role-based admin checks
- User-owned assessment filtering
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
