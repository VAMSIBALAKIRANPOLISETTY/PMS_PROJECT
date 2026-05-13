FOLLOW_UPS = {
    "cough": [
        "How long have you had the cough?",
        "Is it dry or with mucus?",
        "Do you have breathing difficulty?",
        "Any chest pain?",
    ],
    "headache": [
        "Where is the pain located?",
        "Was it sudden or gradual?",
        "Any vision problem or vomiting?",
        "Do you feel dizziness?",
    ],
    "fever": [
        "How many days have you had fever?",
        "Do you have chills or weakness?",
        "What is your highest temperature?",
        "Any cough or breathing issue?",
    ],
}


def calculate_risk(payload: dict) -> dict:
    symptom = payload.get("main_symptom", "").lower()
    details = payload.get("symptom_details", {})
    vitals = payload.get("vitals", {})
    history = payload.get("medical_history", {})
    score = 20
    reasons: list[str] = []

    severity = int(details.get("severity", 0) or 0)
    temperature = float(vitals.get("temperature", 0) or 0)
    oxygen = float(vitals.get("oxygen", 100) or 100)

    if "chest pain" in symptom and ("breathing" in symptom or details.get("breathing_difficulty")):
        score += 65
        reasons.append("Chest pain with breathing difficulty is a red-flag combination.")
    if severity >= 7:
        score += 22
        reasons.append("The reported symptom severity is high.")
    if "fever" in symptom and ("3" in str(details.get("duration", "")) or "4" in str(details.get("duration", ""))):
        score += 24
        reasons.append("Fever duration is longer than three days.")
    if temperature >= 103:
        score += 35
        reasons.append("Temperature is very high.")
    elif temperature >= 100.4:
        score += 14
        reasons.append("Temperature is above normal range.")
    if oxygen < 94:
        score += 36
        reasons.append("Oxygen level is below the safe demo threshold.")
    if history.get("diabetes") and "dizziness" in symptom:
        score += 20
        reasons.append("Dizziness with diabetes history needs follow-up questions.")

    score = min(score, 100)
    risk_level = "High" if score >= 70 else "Medium" if score >= 40 else "Low"
    if not reasons:
        reasons.append("No major red-flag indicator was detected from the provided data.")

    follow_ups = []
    for key, prompts in FOLLOW_UPS.items():
        if key in symptom:
            follow_ups.extend(prompts)

    suggestions = [
        "Track symptoms, vitals, and any changes over time.",
        "Drink fluids and rest if appropriate for your situation.",
        "Consult a qualified doctor for diagnosis or if symptoms persist.",
    ]
    if risk_level == "High":
        suggestions.insert(0, "Seek urgent medical advice for severe or worsening symptoms.")

    return {
        "risk_score": score,
        "risk_level": risk_level,
        "reasons": reasons,
        "follow_up_questions": follow_ups[:5],
        "suggestions": suggestions,
    }
