# Patient Health Assessment and Tracking System

Admin Web + User PWA capstone prototype built with **Java Spring Boot**, **React + TypeScript**, and **PostgreSQL**.

This system helps users enter symptoms, vitals, and health details, then gives a safe **Low / Medium / High** awareness result with reasons and follow-up questions. It does **not** diagnose disease, prescribe medicine, or replace a doctor.

## Detailed Project Description

The Patient Health Assessment and Tracking System is a capstone-style full-stack web application designed to support safe health-awareness workflows for normal users and administrative reviewers. The project focuses on collecting structured health inputs, processing those inputs through a controlled rule-based risk engine, and presenting the result in a clear and non-diagnostic format. The system is intentionally positioned as an educational and awareness prototype rather than a medical decision-making tool. It helps users organize their symptoms, vitals, and history before speaking with a qualified medical professional.

The application has two major workspaces: the User PWA workspace and the Admin Web Dashboard. The User PWA workspace is built for patients or normal users who want to create an account, log in, fill out a health assessment form, review risk awareness output, answer follow-up prompts, and track their previous assessment history. The Admin Web Dashboard is built for administrative review of the system's synthetic/demo data. Admin users can review assessment records, see analytics, inspect risk rules, view the dynamic question bank, and understand the dataset plan used for testing.

On the user side, the assessment form collects practical health indicators such as the main symptom, symptom severity, duration in days, body temperature, oxygen level, heart rate, and chronic condition context. The frontend validates these values before submission, and the backend also validates the request to avoid accepting invalid health data. After submission, the backend returns a risk score, a Low / Medium / High risk level, reasons for that level, safe suggestions, and follow-up questions. These follow-up questions are intended to make the next conversation with a doctor or caretaker more organized.

The risk engine is deliberately simple, explainable, and controlled. It does not use uncontrolled diagnosis logic, and it does not claim that a user has any specific disease. Instead, it evaluates broad risk indicators such as high severity, long duration, abnormal temperature, low oxygen, elevated heart rate, and red-flag symptom combinations. The output stays in the language of health awareness: it explains why the entered data may need attention and encourages professional consultation when symptoms are serious or persistent.

The frontend is implemented with React, TypeScript, Vite, Recharts, Lucide icons, and CSS custom properties. The current branch refactors the earlier single-file frontend into component and section files. This makes the code easier to understand, test, and extend. Authentication pages live under `pages/auth`, user-facing sections live under `pages/user`, admin-facing sections live under `pages/admin`, and shared interface elements live under `components`. Shared TypeScript types, API helpers, static data, and formatting utilities are separated into `types.ts`, `api.ts`, `data.ts`, and `utils.ts`.

The backend is implemented with Java Spring Boot, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL, and an H2 test profile. PostgreSQL is used for normal local runtime on a development system. The H2 profile is used only for automated backend tests so the core Spring context can be validated without requiring a running database server. The backend creates and manages users, assessments, rules, questions, and analytics through controller, service, repository, DTO, and model layers.

Authentication in this prototype uses a simple token-based approach. After login or signup, the backend returns a token and the frontend sends it in the `Authorization: Bearer <token>` header. The backend uses that token to identify the current user and apply role-based behavior. Normal users can see only their own assessments. Admin users can access admin analytics and all assessment records. This is appropriate for a capstone prototype, but production deployment would require a stronger security model such as Spring Security with JWT signing, refresh tokens, rate limiting, audit logging, and stronger operational controls.

The system avoids Docker by design in the current branch. It is intended to run on a normal development system with IntelliJ IDEA or VS Code, Java 17 or later, Maven, Node.js, and a locally installed PostgreSQL server. The backend connection defaults to `pms_db`, `pms_user`, and `pms_password`, but these can be changed using environment variables. This makes the project easier to run on college lab systems, personal laptops, or another development machine where Docker Desktop is not installed.

Testing is part of the current project structure. Backend testing verifies that the Spring Boot application context loads successfully with the H2 test profile. Frontend testing uses Vitest and Testing Library to render every major section: landing page, auth page, layout controls, user overview, assessment form, symptom drawer, reports, history, profile, recent assessments, admin overview, assessment table, rules, questions, datasets, and design picker. The project also supports a production frontend build using TypeScript and Vite.

Overall, the project demonstrates a complete MVP foundation for a patient health-awareness system. It combines authentication, role-based screens, structured health input, explainable risk scoring, frontend validation, backend validation, persistence, analytics, reusable frontend components, and documentation. Future improvements could include a production security model, editable patient profile forms, real PDF extraction, appointment booking, doctor dashboards, multilingual support, richer test coverage, and deployment configuration. Even with those future possibilities, the current scope remains intentionally safe: synthetic/demo data only, no diagnosis, no prescription, and no emergency decision-making.

## Medical Disclaimer

This application does not provide medical diagnosis, treatment, prescription, or emergency service. It is only for educational and health-awareness purposes. For serious symptoms or emergencies, consult a qualified medical professional immediately.

## Current Branch

Use this branch for continuing development:

```text
PMS_Test1
```

This branch contains the Spring Boot backend, React TypeScript frontend, dynamic auth flow, user-owned assessments, admin analytics, follow-up questions, validation, and a componentized frontend structure.

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
GET  /api/assessments
POST /api/assessments
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
