import json
import os
from pathlib import Path

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.models import Base, ContentRecord, ContentVersion

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql+psycopg://civil:tree@localhost:5432/civil_tree")
CONTENT_PATH = Path(os.getenv("CONTENT_PATH", "../android/app/src/main/assets/civil_content.json"))

engine = create_engine(DATABASE_URL, pool_pre_ping=True)
Base.metadata.create_all(engine)
content = json.loads(CONTENT_PATH.read_text(encoding="utf-8"))

with Session(engine) as db:
    for payload in content:
        key = payload["articleKey"]
        record = db.scalar(select(ContentRecord).where(ContentRecord.article_key == key))
        if record is None:
            record = ContentRecord(
                article_key=key,
                current_version=1,
                publication_status="AI_DRAFT",
                is_premium=not (payload["courseId"] == 1 or key == "190"),
            )
            db.add(record)
            db.flush()
            db.add(ContentVersion(
                record_id=record.id,
                version=1,
                payload_json=json.dumps(payload, ensure_ascii=False),
                source_json=json.dumps(payload.get("citations", []), ensure_ascii=False),
                review_status="AI_DRAFT",
                author="pipeline",
            ))
    db.commit()

print(f"seeded {len(content)} content records as AI_DRAFT")
