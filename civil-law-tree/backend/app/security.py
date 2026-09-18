from datetime import datetime, timedelta, timezone
from jose import jwt
from passlib.context import CryptContext

pwd = CryptContext(schemes=["argon2"], deprecated="auto")


def hash_password(password: str) -> str:
    return pwd.hash(password)


def verify_password(password: str, encoded: str) -> bool:
    return pwd.verify(password, encoded)


def create_token(user_id: int, role: str, secret: str, minutes: int = 60 * 24 * 7) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": str(user_id),
        "role": role,
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(minutes=minutes)).timestamp()),
    }
    return jwt.encode(payload, secret, algorithm="HS256")
