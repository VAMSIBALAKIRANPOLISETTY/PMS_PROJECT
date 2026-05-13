from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.entities import AdminRule, Assessment, Consent, PatientProfile, Question, RiskLevel, Role, User
from app.services.risk_engine import calculate_risk
from app.services.security import hash_password


def seed_demo_data(db: Session) -> None:
    user_count = db.scalar(select(func.count(User.id)))
    if user_count:
        return

    admin = User(
        email="admin@example.com",
        username="admin",
        full_name="Demo Admin",
        hashed_password=hash_password("password123"),
        role=Role.admin,
    )
    patient = User(
        email="user@example.com",
        username="anaya",
        full_name="Anaya Rao",
        hashed_password=hash_password("password123"),
        role=Role.user,
    )
    db.add_all([admin, patient])
    db.flush()

    db.add(Consent(user_id=patient.id, accepted_disclaimer=True))
    db.add(
        PatientProfile(
            user_id=patient.id,
            age=24,
            gender="Female",
            height_cm=162,
            weight_kg=58,
            medical_history={"diabetes": False, "asthma": False},
            medications=[],
            allergies=[],
            lifestyle={"sleep_hours": 7, "activity": "Moderate", "smoking": False},
        )
    )

    samples = [
        {"main_symptom": "Fever and weakness", "symptom_details": {"severity": 6, "duration": "4 days"}, "vitals": {"temperature": 100.4}},
        {"main_symptom": "Chest pain with breathing difficulty", "symptom_details": {"severity": 9}, "vitals": {"oxygen": 95}},
        {"main_symptom": "Mild headache", "symptom_details": {"severity": 3, "duration": "1 day"}, "vitals": {"temperature": 98.2}},
    ]
    for sample in samples:
        result = calculate_risk(sample)
        db.add(
            Assessment(
                user_id=patient.id,
                main_symptom=sample["main_symptom"],
                symptom_details=sample["symptom_details"],
                vitals=sample["vitals"],
                risk_level=RiskLevel(result["risk_level"]),
                risk_score=result["risk_score"],
                reasons=result["reasons"],
                suggestions=result["suggestions"],
                follow_up_questions=result["follow_up_questions"],
            )
        )

    db.add_all(
        [
            Question(symptom_key="cough", prompt="How long have you had the cough?"),
            Question(symptom_key="cough", prompt="Do you have breathing difficulty?"),
            Question(symptom_key="headache", prompt="Any vision problem or vomiting?"),
            AdminRule(condition_label="Chest pain + breathing difficulty", risk_level=RiskLevel.high, score=90, explanation="Red flag combination."),
            AdminRule(condition_label="Fever > 3 days + weakness", risk_level=RiskLevel.medium, score=55, explanation="Duration and fatigue increase risk."),
            AdminRule(condition_label="Mild headache + no red flags", risk_level=RiskLevel.low, score=20, explanation="No severe indicator."),
        ]
    )
    db.commit()
