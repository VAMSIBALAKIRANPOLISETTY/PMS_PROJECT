# Patient Health Assessment and Tracking System Using Hybrid AI

Admin Web + User PWA prototype for collecting patient health information, running rule-based risk assessment, uploading text-based PDF reports, generating mock AI summaries, and tracking health history over time.

> Medical disclaimer: This project does not provide medical diagnosis, treatment, prescription, or emergency support. It is only an educational and health-awareness prototype. For serious symptoms or emergencies, consult a qualified medical professional immediately.

---

## 1. Project Overview

This project is a capstone-style healthcare software system with two major parts:

- **User PWA**: A responsive web app for normal users/patients.
- **Admin Web Dashboard**: A separate dashboard for admins to monitor records, rules, questions, datasets, and analytics.

The system collects details such as symptoms, vitals, medical history, lifestyle, allergies, medications, and uploaded report PDFs. It then uses a controlled hybrid AI approach:

- **Rule-based engine** for risk scoring and red-flag detection.
- **Basic NLP-style keyword logic** for PDF/report text summaries.
- **Mock AI response layer** for safe user-friendly explanations.

The first version uses synthetic/demo data only.

---

## 2. Main Features

### User Features

- Demo login area
- Consent/disclaimer positioning
- Patient profile preview
- Symptom and vitals assessment form
- Rule-based risk result: Low, Medium, or High
- Follow-up question preview
- Safe suggestions and explanation
- Text-based PDF upload UI
- Mock report summary
- Assessment history timeline
- Vitals and risk trend charts
- Mobile-friendly PWA-style layout
- Three selectable UI design directions:
  - Clinical Calm
  - Paper Console
  - Vital Signal

### Admin Features

- Admin dashboard view
- User and assessment statistics
- High-risk assessment count
- Common symptom chart
- Risk split chart
- Assessment monitoring table
- Rule management UI
- Dynamic question bank UI
- Synthetic dataset planning UI
- Filters/search visual controls

### Backend Features

- FastAPI backend
- PostgreSQL database support
- SQLAlchemy models
- Email or username login
- JWT token generation
- Role-based access support
- Seeded demo admin and user accounts
- Assessment creation API
- Rule-based risk engine
- PDF upload endpoint
- Text extraction from text-based PDFs
- Mock report summary
- Admin analytics endpoints
- Question and rule management endpoints

---

## 3. Technology Stack

### Frontend

- React
- Vite
- CSS custom properties
- Recharts
- Lucide React icons
- Responsive layout

### Backend

- Python
- FastAPI
- Uvicorn
- SQLAlchemy
- Pydantic
- JWT authentication
- Passlib password hashing
- PyMuPDF for PDF text extraction

### Database

- PostgreSQL
- Docker Compose for local database setup

### Development Tools

- Git
- GitHub
- VS Code
- Postman or Thunder Client
- Docker Desktop
- pgAdmin optional

---

## 4. Folder Structure

```text
PMS/
  backend/
    app/
      api/
        routes.py
      core/
        config.py
      models/
        entities.py
      schemas/
        health.py
      services/
        pdf_service.py
        risk_engine.py
        security.py
        seed.py
      database.py
      main.py
    .env.example
    requirements.txt

  frontend/
    public/
    src/
      main.jsx
      styles.css
    index.html
    package.json
    vite.config.js

  docs/
    architecture.md

  docker-compose.yml
  README.md
  .gitignore
```

---

## 5. Software You Need To Download

Install these before running the project.

### 5.1 Git

Git is used to clone, commit, and push the project.

Download:

```text
https://git-scm.com/downloads
```

Check installation:

```bash
git --version
```

### 5.2 Node.js

Node.js is required for the React frontend.

Recommended version:

```text
Node.js 20 or later
```

Download:

```text
https://nodejs.org/
```

Check installation:

```bash
node --version
npm --version
```

### 5.3 Python

Python is required for the FastAPI backend.

Recommended version:

```text
Python 3.11 or later
```

Download:

```text
https://www.python.org/downloads/
```

Important during installation:

```text
Tick "Add Python to PATH"
```

Check installation:

```bash
python --version
```

### 5.4 Docker Desktop

Docker Desktop is the easiest way to run PostgreSQL locally.

Download:

```text
https://www.docker.com/products/docker-desktop/
```

Check installation:

```bash
docker --version
docker compose version
```

### 5.5 VS Code

VS Code is recommended for editing and running the project.

Download:

```text
https://code.visualstudio.com/
```

Recommended VS Code extensions:

- Python
- Pylance
- ESLint
- Prettier
- Thunder Client
- Docker

### 5.6 Postman or Thunder Client

Use this to test backend APIs.

Postman:

```text
https://www.postman.com/downloads/
```

Thunder Client is a VS Code extension.

### 5.7 pgAdmin Optional

pgAdmin helps you visually inspect the PostgreSQL database.

Download:

```text
https://www.pgadmin.org/download/
```

---

## 6. Demo Login Accounts

The backend automatically creates these accounts when it starts for the first time and connects to an empty database.

```text
Admin:
Email: admin@example.com
Username: admin
Password: password123

User:
Email: user@example.com
Username: anaya
Password: password123
```

The frontend currently displays demo account information visually. Backend authentication endpoints are also available.

---

## 7. How The System Works

### Step 1: User Enters Health Data

The user enters symptoms, severity, duration, vitals, and medical history.

Example:

```text
Main symptom: Fever and weakness
Severity: 6/10
Duration: 4 days
Temperature: 100.4 F
Oxygen: 97
```

### Step 2: Rule Engine Calculates Risk

The backend risk engine checks:

- Symptom type
- Severity
- Duration
- Temperature
- Oxygen level
- Red-flag combinations
- Medical history

Then it returns:

- Risk score
- Risk level
- Reasons
- Follow-up questions
- Safe suggestions

### Step 3: User Sees Safe Output

The output explains risk in simple language and includes a medical disclaimer.

The app never says:

```text
You have disease X.
```

It only says:

```text
Your entered information shows medium risk indicators. Please consult a doctor if symptoms persist.
```

### Step 4: Admin Monitors System

The admin dashboard shows:

- Total users
- Total assessments
- High-risk count
- PDF uploads
- Common symptoms
- Risk split
- Assessment table
- Rules and question bank

---

## 8. Running The Project Locally

Open a terminal in this folder:

```bash
cd C:\Users\vamsi\Desktop\PROJECTS\PMS
```

Or open the folder in VS Code:

```bash
code C:\Users\vamsi\Desktop\PROJECTS\PMS
```

---

## 9. Start PostgreSQL Database

Make sure Docker Desktop is running.

Then run:

```bash
docker compose up -d
```

This starts PostgreSQL with:

```text
Database: pms_db
Username: pms_user
Password: pms_password
Port: 5432
```

Check running containers:

```bash
docker ps
```

Stop database:

```bash
docker compose down
```

Stop database and remove stored data:

```bash
docker compose down -v
```

Use `down -v` only when you want to reset all database data.

---

## 10. Setup And Run Backend

Open a terminal:

```bash
cd backend
```

Create a Python virtual environment:

```bash
python -m venv .venv
```

Activate the virtual environment on Windows PowerShell:

```bash
.venv\Scripts\Activate.ps1
```

If PowerShell blocks activation, run:

```bash
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

Then activate again:

```bash
.venv\Scripts\Activate.ps1
```

Install backend dependencies:

```bash
pip install -r requirements.txt
```

Create `.env` file from the example:

```bash
copy .env.example .env
```

Start the backend:

```bash
uvicorn app.main:app --reload
```

Backend runs at:

```text
http://localhost:8000
```

FastAPI docs:

```text
http://localhost:8000/docs
```

Health check:

```text
http://localhost:8000/api/health
```

---

## 11. Setup And Run Frontend

Open another terminal:

```bash
cd frontend
```

Install frontend dependencies:

```bash
npm install
```

Start frontend:

```bash
npm run dev
```

Frontend runs at:

```text
http://localhost:5173
```

If localhost is blocked in any tool, use:

```text
http://127.0.0.1:5173
```

Build frontend for production:

```bash
npm run build
```

Preview production build:

```bash
npm run preview
```

---

## 12. Backend Environment Variables

Backend configuration is in:

```text
backend/.env
```

Example:

```env
APP_NAME="Patient Health Assessment API"
DATABASE_URL="postgresql+psycopg://pms_user:pms_password@localhost:5432/pms_db"
JWT_SECRET="change-this-demo-secret"
ACCESS_TOKEN_EXPIRE_MINUTES=1440
CORS_ORIGINS="http://localhost:5173"
```

Meaning:

- `APP_NAME`: Name shown in FastAPI docs.
- `DATABASE_URL`: PostgreSQL connection string.
- `JWT_SECRET`: Secret key used to sign login tokens.
- `ACCESS_TOKEN_EXPIRE_MINUTES`: Token lifetime.
- `CORS_ORIGINS`: Frontend URLs allowed to call backend.

For a real production app, change `JWT_SECRET`.

---

## 13. Important API Endpoints

Base URL:

```text
http://localhost:8000/api
```

### Health Check

```http
GET /api/health
```

### Register

```http
POST /api/auth/register
```

Example JSON:

```json
{
  "email": "newuser@example.com",
  "username": "newuser",
  "full_name": "New User",
  "password": "password123",
  "role": "user"
}
```

### Login With Email Or Username

```http
POST /api/auth/login
```

Example JSON:

```json
{
  "identifier": "user@example.com",
  "password": "password123"
}
```

Or:

```json
{
  "identifier": "anaya",
  "password": "password123"
}
```

### Get Current User

```http
GET /api/me
```

Requires Bearer token.

### Create Assessment

```http
POST /api/assessments
```

Example JSON:

```json
{
  "main_symptom": "Fever and weakness",
  "symptom_details": {
    "severity": 6,
    "duration": "4 days"
  },
  "vitals": {
    "temperature": 100.4,
    "oxygen": 97
  },
  "medical_history": {
    "diabetes": false,
    "asthma": false
  }
}
```

### List Assessments

```http
GET /api/assessments
```

Admin sees all assessments. User sees only their own assessments.

### Upload PDF Report

```http
POST /api/reports/upload
```

Use multipart form data:

```text
file: report.pdf
```

Only text-based PDFs are supported in version one. Scanned image PDFs are out of scope.

### Admin Analytics

```http
GET /api/admin/analytics
```

Admin token required.

### Admin Questions

```http
GET /api/admin/questions
POST /api/admin/questions
```

### Admin Rules

```http
GET /api/admin/rules
POST /api/admin/rules
```

---

## 14. Database Tables

The backend creates tables automatically on startup.

Important tables:

- `users`
- `consents`
- `patient_profiles`
- `assessments`
- `questions`
- `admin_rules`
- `uploaded_reports`
- `audit_logs`

The models are defined in:

```text
backend/app/models/entities.py
```

---

## 15. Risk Engine Explanation

The risk engine is in:

```text
backend/app/services/risk_engine.py
```

It checks inputs and calculates a score.

Example rules:

- Chest pain + breathing difficulty increases risk strongly.
- Severity greater than or equal to 7 increases risk.
- Fever longer than three days increases risk.
- Temperature above normal increases risk.
- Very high temperature increases risk more.
- Oxygen below demo threshold increases risk.
- Diabetes history + dizziness triggers follow-up logic.

Risk classification:

```text
0-39   = Low
40-69  = Medium
70-100 = High
```

The system also returns reasons so the output is explainable.

---

## 16. PDF Report Processing

PDF logic is in:

```text
backend/app/services/pdf_service.py
```

Current scope:

- Accept PDF files
- Extract readable text using PyMuPDF
- Detect a few common words such as hemoglobin, glucose, cholesterol, WBC, platelet, blood pressure
- Generate a mock summary

Not included in version one:

- OCR
- Scanned report reading
- X-ray/MRI image analysis
- Medical diagnosis
- Lab-value clinical interpretation

---

## 17. Frontend UI Explanation

Frontend entry file:

```text
frontend/src/main.jsx
```

Frontend styling:

```text
frontend/src/styles.css
```

The UI has two modes:

- User mode
- Admin mode

The top segmented control switches between them.

The design selector switches between:

- Clinical Calm
- Paper Console
- Vital Signal

These are controlled using CSS variables on `body[data-theme]`.

Charts are built using Recharts.

Icons are from Lucide React.

---

## 18. Validation Commands

Run frontend build:

```bash
cd frontend
npm run build
```

Run backend syntax check:

```bash
cd backend
python -m compileall app
```

Check API manually:

```bash
curl http://localhost:8000/api/health
```

---

## 19. Common Problems And Fixes

### Problem: `docker` command not found

Fix:

- Install Docker Desktop.
- Restart your terminal.
- Make sure Docker Desktop is running.

### Problem: PostgreSQL connection error

Fix:

Run:

```bash
docker compose up -d
docker ps
```

Confirm PostgreSQL is running on port `5432`.

### Problem: Python virtual environment activation blocked

Fix:

```bash
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

Then:

```bash
.venv\Scripts\Activate.ps1
```

### Problem: Frontend port already in use

Fix:

Stop the old frontend terminal or run:

```bash
npm run dev -- --port 5174
```

### Problem: Backend port already in use

Fix:

Run backend on another port:

```bash
uvicorn app.main:app --reload --port 8001
```

If you change backend port, update frontend proxy in:

```text
frontend/vite.config.js
```

### Problem: PDF summary says no readable text

Reason:

The PDF is probably scanned or image-based.

Fix:

Use a text-based PDF for this version.

---

## 20. Security Scope

Included for demo:

- Password hashing
- JWT authentication
- Role field
- Admin-only endpoint checks
- File type validation for PDF endpoint
- Environment-based configuration

Not included:

- HIPAA compliance
- Production-grade encryption
- Hospital-grade audit system
- Real patient data handling
- Advanced rate limiting
- Refresh token rotation

This project must not be used with real patient data without major security and compliance upgrades.

---

## 21. Future Enhancements

Possible improvements:

- Real OpenAI/Gemini/Azure OpenAI integration
- OCR for scanned PDFs
- Native Android app
- Doctor dashboard
- Appointment booking
- Multilingual support
- Voice symptom input
- Wearable integration
- Emergency alert workflow
- FHIR/HL7 medical data support
- More advanced rule builder
- Exportable assessment summary PDF
- Better test suite
- Dockerized frontend/backend services

---

## 22. Project Status

Current status:

- Frontend scaffold complete
- Backend scaffold complete
- PostgreSQL configuration complete
- Demo users seeded
- Synthetic data included
- Mock AI summary included
- PDF text extraction included
- Detailed documentation included

This is a demo-ready capstone MVP foundation.

---

## 23. Quick Start Summary

Run database:

```bash
docker compose up -d
```

Run backend:

```bash
cd backend
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
uvicorn app.main:app --reload
```

Run frontend:

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

API docs:

```text
http://localhost:8000/docs
```
