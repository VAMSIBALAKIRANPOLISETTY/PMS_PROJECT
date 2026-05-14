# Patient Health Assessment and Tracking System

Admin Web + User PWA capstone prototype built with **Java Spring Boot**, **React + TypeScript**, and **PostgreSQL**.

This system helps users enter symptoms, vitals, and health details, then gives a safe **Low / Medium / High** awareness result with reasons and follow-up questions. It does **not** diagnose disease, prescribe medicine, or replace a doctor.

---

## Medical Disclaimer

This application does not provide medical diagnosis, treatment, prescription, or emergency service. It is only for educational and health-awareness purposes. For serious symptoms or emergencies, consult a qualified medical professional immediately.

---

## Current Branch

Latest requested test branch:

```text
PMS_Test1
```

Major changes in this branch:

- Backend changed from Python/FastAPI to **Java Spring Boot**
- Frontend changed from `.jsx` to **TypeScript React `.tsx`**
- Landing page appears first
- Login and signup are dynamic
- Logged-in users see only their own assessments
- Admin can see all assessments and analytics
- Follow-up questions are generated after assessment
- Main symptom field has a fixed searchable symptom drawer
- Health input values are validated on frontend and backend

---

## Software To Install

### 1. Git

Used to clone and push the project.

Download:

```text
https://git-scm.com/downloads
```

Check:

```bash
git --version
```

### 2. Java JDK

Required for Spring Boot backend.

Recommended:

```text
JDK 17 or later
```

This project was tested with JDK 20.

Download:

```text
https://www.oracle.com/java/technologies/downloads/
```

Check:

```bash
java --version
```

If Maven wrapper says `JAVA_HOME` is not set, set it on Windows PowerShell:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-20"
```

Change the path if your JDK folder is different.

### 3. Node.js

Required for the React TypeScript frontend.

Recommended:

```text
Node.js 20 or later
```

Download:

```text
https://nodejs.org/
```

Check:

```bash
node --version
npm --version
```

### 4. Docker Desktop

Used to run PostgreSQL locally.

Download:

```text
https://www.docker.com/products/docker-desktop/
```

Check:

```bash
docker --version
docker compose version
```

### 5. VS Code

Recommended editor.

Download:

```text
https://code.visualstudio.com/
```

Useful extensions:

- Extension Pack for Java
- Spring Boot Extension Pack
- ESLint
- Prettier
- Docker
- Thunder Client

### 6. Postman Or Thunder Client

Optional, used to test APIs.

---

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
      styles.css

  docs/
    architecture.md

  docker-compose.yml
  README.md
```

---

## Backend Explanation

Backend location:

```text
backend/
```

Technology:

- Java
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL driver
- H2 database for test profile

Main backend files:

```text
BackendApplication.java
controller/AuthController.java
controller/AssessmentController.java
controller/AdminController.java
service/AuthService.java
service/AssessmentService.java
service/RiskEngineService.java
service/SeedDataService.java
model/AppUser.java
model/Assessment.java
repository/UserRepository.java
repository/AssessmentRepository.java
```

The backend uses simple token-based authentication. After login/signup, the backend returns a token. The frontend sends that token using:

```text
Authorization: Bearer <token>
```

This is simple and suitable for a capstone prototype. For production, replace it with JWT or Spring Security session management.

---

## Frontend Explanation

Frontend location:

```text
frontend/
```

Technology:

- React
- TypeScript
- Vite
- Recharts
- Lucide React icons
- CSS custom properties

Main frontend files:

```text
frontend/src/main.tsx
frontend/src/styles.css
frontend/index.html
frontend/vite.config.js
```

The frontend now starts with a landing page. From there:

1. User clicks Login or Sign up
2. User authenticates
3. The app loads their own records from the backend
4. Admin users can access admin mode
5. Normal users only see user mode

---

## Demo Accounts

The Spring Boot backend seeds these accounts when the database is empty:

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

You can also sign up with a new account from the landing page.

---

## Database Setup

PostgreSQL is configured through Docker Compose.

Database details:

```text
Database: pms_db
Username: pms_user
Password: pms_password
Port: 5432
```

Start database:

```bash
docker compose up -d
```

Check containers:

```bash
docker ps
```

Stop database:

```bash
docker compose down
```

Reset database completely:

```bash
docker compose down -v
```

Use `down -v` only when you want to delete all saved database data.

---

## Run Backend

Open terminal in project root:

```bash
cd C:\Users\vamsi\Desktop\PROJECTS\PMS
```

Start PostgreSQL first:

```bash
docker compose up -d
```

Go to backend:

```bash
cd backend
```

Set JAVA_HOME if needed:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-20"
```

Run backend:

```bash
.\mvnw.cmd spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/api/health
```

---

## Run Frontend

Open another terminal:

```bash
cd C:\Users\vamsi\Desktop\PROJECTS\PMS\frontend
```

Install dependencies:

```bash
npm install
```

Start frontend:

```bash
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

If a browser blocks localhost, use:

```text
http://127.0.0.1:5173
```

The frontend proxy sends API requests to:

```text
http://localhost:8080
```

Proxy config is in:

```text
frontend/vite.config.js
```

---

## API Endpoints

Base URL:

```text
http://localhost:8080/api
```

### Health

```http
GET /api/health
```

### Signup

```http
POST /api/auth/register
```

Example:

```json
{
  "email": "newuser@example.com",
  "username": "newuser",
  "fullName": "New User",
  "password": "password123",
  "role": "USER"
}
```

### Login

```http
POST /api/auth/login
```

Example:

```json
{
  "identifier": "user@example.com",
  "password": "password123"
}
```

You can use email or username in `identifier`.

### Current User

```http
GET /api/auth/me
```

Requires:

```text
Authorization: Bearer <token>
```

### Create Assessment

```http
POST /api/assessments
```

Example:

```json
{
  "mainSymptom": "Fever",
  "severity": 6,
  "durationDays": 4,
  "temperatureF": 100.4,
  "oxygenLevel": 97,
  "heartRate": 88,
  "chronicCondition": "None"
}
```

Validation rules:

```text
mainSymptom: 2 to 120 characters
severity: 1 to 10
durationDays: 0 to 365
temperatureF: 90.0 to 110.0
oxygenLevel: 50 to 100
heartRate: 35 to 220
```

### List Assessments

```http
GET /api/assessments
```

Normal user:

```text
Sees only their own assessments.
```

Admin:

```text
Sees all assessments.
```

### Admin Analytics

```http
GET /api/admin/analytics
```

Admin token required.

### Admin Rules

```http
GET /api/admin/rules
POST /api/admin/rules
```

### Admin Questions

```http
GET /api/admin/questions
```

---

## Risk Engine Logic

Risk engine file:

```text
backend/src/main/java/com/pms/backend/service/RiskEngineService.java
```

It calculates score using:

- Severity
- Symptom duration
- Temperature
- Oxygen level
- Heart rate
- Red-flag symptom combinations
- Chronic condition context

Risk levels:

```text
0-39   = Low
40-69  = Medium
70-100 = High
```

It also returns:

- Reasons
- Suggestions
- Follow-up questions

Example:

```text
Fever + 4 days + 100.4 F
=> Medium risk
=> Follow-up questions about fever duration, chills, weakness, and highest temperature
```

---

## Main Symptom Drawer

On the assessment page, the right side has a fixed symptom drawer.

It includes:

- Search input
- Common symptoms list
- Click-to-fill main symptom

The drawer helps avoid random symptom spelling and keeps the assessment flow cleaner.

---

## Validation And Testing

Frontend build:

```bash
cd frontend
npm run build
```

Backend tests:

```bash
cd backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-20"
.\mvnw.cmd test "-Dspring.profiles.active=test"
```

The backend test profile uses H2 in-memory database, so it does not require PostgreSQL during tests.

---

## Common Problems

### JAVA_HOME is not defined

Set:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-20"
```

### PostgreSQL connection failed

Make sure Docker Desktop is running:

```bash
docker compose up -d
docker ps
```

### Frontend cannot reach backend

Check backend is running:

```text
http://localhost:8080/api/health
```

Check frontend proxy:

```text
frontend/vite.config.js
```

### Port already in use

Frontend alternate port:

```bash
npm run dev -- --port 5174
```

Backend alternate port:

```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

---

## Security Scope

Included:

- Password hashing with BCrypt
- Simple token login
- Role-based admin checks
- Frontend and backend input validation
- User-owned assessment records

Not included:

- Production JWT refresh flow
- HIPAA compliance
- Real medical data security
- Hospital-grade audit logs
- Real diagnosis
- Prescription logic

---

## Future Improvements

- Replace simple token store with JWT/Spring Security
- Add PDF text extraction in Spring Boot using PDFBox
- Add OCR for scanned reports
- Add editable patient profile form
- Add real OpenAI/Gemini/Azure OpenAI integration
- Add test coverage for controllers and services
- Add Dockerfile for backend and frontend
- Add deployment setup

---

## Quick Start

Terminal 1:

```bash
docker compose up -d
```

Terminal 2:

```bash
cd backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-20"
.\mvnw.cmd spring-boot:run
```

Terminal 3:

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```
