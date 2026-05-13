from datetime import datetime
from typing import Any

from pydantic import BaseModel, EmailStr, Field


class RegisterRequest(BaseModel):
    email: EmailStr
    username: str = Field(min_length=3, max_length=80)
    full_name: str
    password: str = Field(min_length=8)
    role: str = "user"


class LoginRequest(BaseModel):
    identifier: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict[str, Any]


class AssessmentCreate(BaseModel):
    main_symptom: str
    symptom_details: dict[str, Any] = Field(default_factory=dict)
    vitals: dict[str, Any] = Field(default_factory=dict)
    medical_history: dict[str, Any] = Field(default_factory=dict)


class AssessmentOut(BaseModel):
    id: int
    main_symptom: str
    risk_level: str
    risk_score: int
    reasons: list[str]
    suggestions: list[str]
    follow_up_questions: list[str]
    created_at: datetime

    model_config = {"from_attributes": True}


class QuestionCreate(BaseModel):
    symptom_key: str
    prompt: str
    input_type: str = "text"


class RuleCreate(BaseModel):
    condition_label: str
    risk_level: str
    score: int
    explanation: str
