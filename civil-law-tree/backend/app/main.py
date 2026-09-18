from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from jose import JWTError, jwt
from pydantic import BaseModel, EmailStr, Field
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session, sessionmaker

from .models import AuditLog, Base, ContentRecord, ContentVersion, StudyProgress, Subscription, User
from .security import create_token, hash_password, verify_password

ENV = os.getenv("ENV", "dev")
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql+psycopg://civil:tree@db:5432/civil_tree")
JWT_SECRET = os.getenv("JWT_SECRET", "dev-only-change-me")
if ENV == "production" and JWT_SECRET == "dev-only-change-me":
    raise RuntimeError("JWT_SECRET must be supplied in production")

engine = create_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)
app = FastAPI(title="درخت قانون مدنی API", version="0.1.0")

allowed_origins = [x.strip() for x in os.getenv("CORS_ORIGINS", "").split(",") if x.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins or (["*"] if ENV == "dev" else []),
    allow_credentials=True,
    allow_methods=["GET", "POST", "PATCH"],
    allow_headers=["Authorization", "Content-Type"],
)


@app.on_event("startup")
def startup() -> None:
    if ENV == "dev":
        Base.metadata.create_all(engine)


def db_session():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


class RegisterIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=10, max_length=128)
    full_name: str = Field(default="", max_length=200)


class LoginIn(BaseModel):
    email: EmailStr
    password: str


class ProgressIn(BaseModel):
    state: str
    starred: bool = False
    weakness_score: int = Field(default=0, ge=0, le=100)


class ContentPatch(BaseModel):
    payload: dict
    sources: list[dict] = []
    review_status: str = "AI_DRAFT"
    reviewer: str = ""


def current_user(
    authorization: str | None = Header(default=None),
    db: Session = Depends(db_session),
) -> User:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Authentication required")
    token = authorization.removeprefix("Bearer ").strip()
    try:
        claims = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
        user_id = int(claims["sub"])
    except (JWTError, KeyError, ValueError):
        raise HTTPException(401, "Invalid session")
    user = db.get(User, user_id)
    if not user or not user.is_active:
        raise HTTPException(401, "Inactive account")
    return user


def require_admin(user: User = Depends(current_user)) -> User:
    if user.role not in {"admin", "reviewer"}:
        raise HTTPException(403, "Admin/reviewer role required")
    return user


def has_premium(db: Session, user: User) -> bool:
    now = datetime.now(timezone.utc)
    rows = db.scalars(
        select(Subscription).where(
            Subscription.user_id == user.id,
            Subscription.status == "active",
        )
    ).all()
    return any(x.plan_code != "free" and (x.ends_at is None or x.ends_at > now) for x in rows)


@app.get("/health")
def health():
    return {"ok": True, "service": "civil-law-tree"}


@app.post("/auth/register")
def register(body: RegisterIn, db: Session = Depends(db_session)):
    if db.scalar(select(User).where(User.email == body.email.lower())):
        raise HTTPException(409, "Email already registered")
    user = User(
        email=body.email.lower(),
        password_hash=hash_password(body.password),
        full_name=body.full_name.strip(),
    )
    db.add(user)
    db.flush()
    db.add(Subscription(user_id=user.id, plan_code="free", status="active"))
    db.add(AuditLog(actor_user_id=user.id, action="REGISTER", target=f"user:{user.id}"))
    db.commit()
    return {"access_token": create_token(user.id, user.role, JWT_SECRET), "token_type": "bearer"}


@app.post("/auth/login")
def login(body: LoginIn, db: Session = Depends(db_session)):
    user = db.scalar(select(User).where(User.email == body.email.lower()))
    if not user or not verify_password(body.password, user.password_hash):
        raise HTTPException(401, "Invalid credentials")
    return {"access_token": create_token(user.id, user.role, JWT_SECRET), "token_type": "bearer"}


@app.get("/me")
def me(user: User = Depends(current_user), db: Session = Depends(db_session)):
    return {
        "id": user.id,
        "email": user.email,
        "full_name": user.full_name,
        "role": user.role,
        "premium": has_premium(db, user),
    }


@app.get("/content/articles")
def articles(
    course_id: int | None = None,
    user: User = Depends(current_user),
    db: Session = Depends(db_session),
):
    records = db.scalars(select(ContentRecord).order_by(ContentRecord.article_key)).all()
    premium = has_premium(db, user)
    result = []
    for record in records:
        if record.publication_status != "APPROVED" and user.role not in {"admin", "reviewer"}:
            continue
        if record.is_premium and not premium and record.article_key != "190":
            continue
        version = db.scalar(
            select(ContentVersion).where(
                ContentVersion.record_id == record.id,
                ContentVersion.version == record.current_version,
            )
        )
        if not version:
            continue
        payload = json.loads(version.payload_json)
        if course_id and payload.get("courseId") != course_id:
            continue
        result.append(payload)
    return result


@app.get("/content/articles/{article_key}")
def article(
    article_key: str,
    user: User = Depends(current_user),
    db: Session = Depends(db_session),
):
    record = db.scalar(select(ContentRecord).where(ContentRecord.article_key == article_key))
    if not record:
        raise HTTPException(404, "Article not found")
    if record.publication_status != "APPROVED" and user.role not in {"admin", "reviewer"}:
        raise HTTPException(404, "Article not published")
    if record.is_premium and not has_premium(db, user) and article_key != "190":
        raise HTTPException(402, "Premium entitlement required")
    version = db.scalar(
        select(ContentVersion).where(
            ContentVersion.record_id == record.id,
            ContentVersion.version == record.current_version,
        )
    )
    return json.loads(version.payload_json)


@app.put("/progress/{article_key}")
def save_progress(
    article_key: str,
    body: ProgressIn,
    user: User = Depends(current_user),
    db: Session = Depends(db_session),
):
    allowed = {"UNSEEN", "LEARNING", "UNDERSTOOD", "REVIEW", "HARD", "MASTERED"}
    if body.state not in allowed:
        raise HTTPException(422, "Invalid progress state")
    row = db.scalar(
        select(StudyProgress).where(
            StudyProgress.user_id == user.id,
            StudyProgress.article_key == article_key,
        )
    )
    if row is None:
        row = StudyProgress(user_id=user.id, article_key=article_key)
        db.add(row)
    row.state = body.state
    row.starred = body.starred
    row.weakness_score = body.weakness_score
    db.commit()
    return {"ok": True}


@app.get("/subscriptions/me")
def subscription(user: User = Depends(current_user), db: Session = Depends(db_session)):
    rows = db.scalars(select(Subscription).where(Subscription.user_id == user.id)).all()
    return [
        {
            "plan_code": x.plan_code,
            "status": x.status,
            "starts_at": x.starts_at,
            "ends_at": x.ends_at,
            "provider": x.provider,
        }
        for x in rows
    ]


@app.post("/payments/intents")
def payment_intent(user: User = Depends(current_user)):
    # Provider adapter is deliberately not faked. Production activation requires
    # a real merchant/store account, verified callback URL and provider-specific signature checks.
    raise HTTPException(503, "Payment provider is not configured")


@app.patch("/admin/content/{article_key}")
def admin_update_content(
    article_key: str,
    body: ContentPatch,
    admin: User = Depends(require_admin),
    db: Session = Depends(db_session),
):
    record = db.scalar(select(ContentRecord).where(ContentRecord.article_key == article_key))
    if record is None:
        record = ContentRecord(article_key=article_key, current_version=0, publication_status="AI_DRAFT")
        db.add(record)
        db.flush()
    next_version = record.current_version + 1
    version = ContentVersion(
        record_id=record.id,
        version=next_version,
        payload_json=json.dumps(body.payload, ensure_ascii=False),
        source_json=json.dumps(body.sources, ensure_ascii=False),
        review_status=body.review_status,
        author=f"user:{admin.id}",
        reviewer=body.reviewer,
    )
    db.add(version)
    record.current_version = next_version
    record.publication_status = body.review_status
    db.add(AuditLog(
        actor_user_id=admin.id,
        action="CONTENT_VERSION_CREATE",
        target=f"article:{article_key}:v{next_version}",
        details=body.review_status,
    ))
    db.commit()
    return {"article_key": article_key, "version": next_version, "status": body.review_status}


@app.post("/admin/content/{article_key}/approve")
def approve_content(
    article_key: str,
    admin: User = Depends(require_admin),
    db: Session = Depends(db_session),
):
    record = db.scalar(select(ContentRecord).where(ContentRecord.article_key == article_key))
    if not record:
        raise HTTPException(404, "Article not found")
    version = db.scalar(
        select(ContentVersion).where(
            ContentVersion.record_id == record.id,
            ContentVersion.version == record.current_version,
        )
    )
    if not version or not json.loads(version.source_json):
        raise HTTPException(409, "At least one traceable source is required before approval")
    version.review_status = "APPROVED"
    version.reviewer = version.reviewer or f"user:{admin.id}"
    record.publication_status = "APPROVED"
    db.add(AuditLog(
        actor_user_id=admin.id,
        action="CONTENT_APPROVE",
        target=f"article:{article_key}:v{record.current_version}",
    ))
    db.commit()
    return {"ok": True, "status": "APPROVED"}
