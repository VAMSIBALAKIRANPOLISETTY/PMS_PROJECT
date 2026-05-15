# Patient Health Assessment and Tracking System

Admin Web + User PWA capstone prototype built with **Java Spring Boot**, **React + TypeScript**, and **PostgreSQL**.

This system helps users enter symptoms, vitals, and health details, then gives a safe **Low / Medium / High** awareness result with reasons and follow-up questions. It does **not** diagnose disease, prescribe medicine, or replace a doctor.

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
