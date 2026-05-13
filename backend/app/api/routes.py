from typing import Annotated

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt
from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.database import get_db
from app.models.entities import AdminRule, Assessment, Question, RiskLevel, Role, UploadedReport, User
from app.schemas.health import AssessmentCreate, AssessmentOut, LoginRequest, QuestionCreate, RegisterRequest, RuleCreate, TokenResponse
from app.services.pdf_service import extract_pdf_text, summarize_report_text
from app.services.risk_engine import calculate_risk
from app.services.security import ALGORITHM, create_access_token, hash_password, verify_password

router = APIRouter(prefix="/api")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login")


def public_user(user: User) -> dict:
    return {"id": user.id, "email": user.email, "username": user.username, "full_name": user.full_name, "role": user.role.value}


def get_current_user(token: Annotated[str, Depends(oauth2_scheme)], db: Annotated[Session, Depends(get_db)]) -> User:
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=[ALGORITHM])
        subject = payload.get("sub")
    except JWTError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token") from exc
    user = db.scalar(select(User).where(User.email == subject))
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user


def require_admin(user: Annotated[User, Depends(get_current_user)]) -> User:
    if user.role != Role.admin:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Admin access required")
    return user


@router.get("/health")
def health() -> dict:
    return {"status": "ok", "service": settings.app_name}


@router.post("/auth/register", response_model=TokenResponse)
def register(payload: RegisterRequest, db: Annotated[Session, Depends(get_db)]) -> TokenResponse:
    existing = db.scalar(select(User).where(or_(User.email == payload.email, User.username == payload.username)))
    if existing:
        raise HTTPException(status_code=409, detail="Email or username already exists")

    role = Role.admin if payload.role == "admin" else Role.user
    user = User(
        email=payload.email,
        username=payload.username,
        full_name=payload.full_name,
        hashed_password=hash_password(payload.password),
        role=role,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    token = create_access_token(user.email, user.role.value)
    return TokenResponse(access_token=token, user=public_user(user))


@router.post("/auth/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Annotated[Session, Depends(get_db)]) -> TokenResponse:
    user = db.scalar(select(User).where(or_(User.email == payload.identifier, User.username == payload.identifier)))
    if not user or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    token = create_access_token(user.email, user.role.value)
    return TokenResponse(access_token=token, user=public_user(user))


@router.get("/me")
def me(user: Annotated[User, Depends(get_current_user)]) -> dict:
    return public_user(user)


@router.get("/assessments", response_model=list[AssessmentOut])
def list_assessments(user: Annotated[User, Depends(get_current_user)], db: Annotated[Session, Depends(get_db)]) -> list[Assessment]:
    query = select(Assessment).order_by(Assessment.created_at.desc())
    if user.role != Role.admin:
        query = query.where(Assessment.user_id == user.id)
    return list(db.scalars(query))


@router.post("/assessments", response_model=AssessmentOut)
def create_assessment(payload: AssessmentCreate, user: Annotated[User, Depends(get_current_user)], db: Annotated[Session, Depends(get_db)]) -> Assessment:
    result = calculate_risk(payload.model_dump())
    assessment = Assessment(
        user_id=user.id,
        main_symptom=payload.main_symptom,
        symptom_details=payload.symptom_details,
        vitals=payload.vitals,
        risk_level=RiskLevel(result["risk_level"]),
        risk_score=result["risk_score"],
        reasons=result["reasons"],
        suggestions=result["suggestions"],
        follow_up_questions=result["follow_up_questions"],
    )
    db.add(assessment)
    db.commit()
    db.refresh(assessment)
    return assessment


@router.post("/reports/upload")
async def upload_report(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    file: UploadFile = File(...),
) -> dict:
    if file.content_type != "application/pdf":
        raise HTTPException(status_code=400, detail="Only PDF uploads are supported")
    raw_text = await extract_pdf_text(file)
    summary = summarize_report_text(raw_text)
    report = UploadedReport(user_id=user.id, filename=file.filename or "report.pdf", raw_text=raw_text, mock_summary=summary)
    db.add(report)
    db.commit()
    return {"id": report.id, "filename": report.filename, "summary": summary, "characters_extracted": len(raw_text)}


@router.get("/admin/analytics")
def admin_analytics(_: Annotated[User, Depends(require_admin)], db: Annotated[Session, Depends(get_db)]) -> dict:
    rows = list(db.scalars(select(Assessment)))
    high_count = sum(1 for row in rows if row.risk_level == RiskLevel.high)
    medium_count = sum(1 for row in rows if row.risk_level == RiskLevel.medium)
    low_count = sum(1 for row in rows if row.risk_level == RiskLevel.low)
    return {
        "total_users": db.scalar(select(func.count(User.id))) or 0,
        "total_assessments": len(rows),
        "high_risk_count": high_count,
        "risk_split": {"Low": low_count, "Medium": medium_count, "High": high_count},
    }


@router.get("/admin/questions")
def list_questions(_: Annotated[User, Depends(require_admin)], db: Annotated[Session, Depends(get_db)]) -> list[dict]:
    return [
        {"id": item.id, "symptom_key": item.symptom_key, "prompt": item.prompt, "input_type": item.input_type, "is_active": item.is_active}
        for item in db.scalars(select(Question).order_by(Question.symptom_key))
    ]


@router.post("/admin/questions")
def create_question(payload: QuestionCreate, _: Annotated[User, Depends(require_admin)], db: Annotated[Session, Depends(get_db)]) -> dict:
    question = Question(**payload.model_dump())
    db.add(question)
    db.commit()
    db.refresh(question)
    return {"id": question.id, "prompt": question.prompt}


@router.get("/admin/rules")
def list_rules(_: Annotated[User, Depends(require_admin)], db: Annotated[Session, Depends(get_db)]) -> list[dict]:
    return [
        {"id": item.id, "condition_label": item.condition_label, "risk_level": item.risk_level.value, "score": item.score, "explanation": item.explanation}
        for item in db.scalars(select(AdminRule).order_by(AdminRule.id))
    ]


@router.post("/admin/rules")
def create_rule(payload: RuleCreate, _: Annotated[User, Depends(require_admin)], db: Annotated[Session, Depends(get_db)]) -> dict:
    rule = AdminRule(
        condition_label=payload.condition_label,
        risk_level=RiskLevel(payload.risk_level),
        score=payload.score,
        explanation=payload.explanation,
    )
    db.add(rule)
    db.commit()
    db.refresh(rule)
    return {"id": rule.id, "condition_label": rule.condition_label}
