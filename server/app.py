"""NuanYue sync server.

Stores end-to-end encrypted backups only. The phone derives two keys from the
user's password: one encrypts data locally, the other (auth token) proves
ownership. The server keeps sha256(auth token) and an opaque ciphertext; it can
never read anyone's records.
"""
import hashlib
import hmac
import json
import os
import re
import sqlite3
import threading
import time
from collections import defaultdict, deque
from contextlib import closing

from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

DATA_DIR = os.environ.get("NUANYUE_DATA", "/var/lib/nuanyue")
DIST_DIR = os.environ.get("NUANYUE_DIST", "/opt/nuanyue/web")
DB_PATH = os.path.join(DATA_DIR, "nuanyue.db")
MAX_BLOB = 8 * 1024 * 1024
HEX64 = re.compile(r"^[0-9a-f]{64}$")

app = FastAPI(title="NuanYue Sync", docs_url=None, redoc_url=None, openapi_url=None)
_lock = threading.Lock()


def db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, timeout=10)
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def init_db() -> None:
    os.makedirs(DATA_DIR, exist_ok=True)
    with closing(db()) as conn, conn:
        conn.execute(
            """CREATE TABLE IF NOT EXISTS backups (
                space TEXT PRIMARY KEY,
                auth_hash TEXT NOT NULL,
                blob TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )


init_db()

# ---- naive in-memory rate limiting (single process) ----
_hits: dict[str, deque] = defaultdict(deque)
_fails: dict[str, deque] = defaultdict(deque)


def _client_ip(request: Request) -> str:
    return request.headers.get("x-real-ip") or (request.client.host if request.client else "?")


def _throttle(bucket: dict, key: str, limit: int, window: float) -> bool:
    now = time.monotonic()
    q = bucket[key]
    while q and now - q[0] > window:
        q.popleft()
    if len(q) >= limit:
        return False
    q.append(now)
    return True


@app.middleware("http")
async def rate_limit(request: Request, call_next):
    ip = _client_ip(request)
    if not _throttle(_hits, ip, 120, 60):
        return JSONResponse({"detail": "too many requests"}, status_code=429)
    return await call_next(request)


def _check_ids(space: str, auth: str | None) -> str:
    if not HEX64.match(space):
        raise HTTPException(400, "bad space id")
    if not auth or not HEX64.match(auth):
        raise HTTPException(401, "missing auth")
    return hashlib.sha256(auth.encode()).hexdigest()


def _auth_failed(request: Request) -> None:
    ip = _client_ip(request)
    # 20 wrong passwords per 10 minutes per IP, then lock out.
    if not _throttle(_fails, ip, 20, 600):
        raise HTTPException(429, "too many failed attempts")
    raise HTTPException(403, "wrong credentials")


class PutBody(BaseModel):
    blob: str = Field(max_length=MAX_BLOB)
    updatedAt: int


@app.get("/api/health")
def health():
    return {"ok": True, "time": int(time.time() * 1000)}


@app.get("/api/version")
def version():
    path = os.path.join(DIST_DIR, "version.json")
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        raise HTTPException(404, "no release yet")


@app.get("/api/backup/{space}")
def get_backup(space: str, request: Request, x_auth: str | None = Header(default=None)):
    auth_hash = _check_ids(space, x_auth)
    with closing(db()) as conn:
        row = conn.execute("SELECT auth_hash, blob, updated_at FROM backups WHERE space = ?", (space,)).fetchone()
    if row is None:
        raise HTTPException(404, "not found")
    if not hmac.compare_digest(row[0], auth_hash):
        _auth_failed(request)
    return {"blob": row[1], "updatedAt": row[2]}


@app.put("/api/backup/{space}")
def put_backup(space: str, body: PutBody, request: Request, x_auth: str | None = Header(default=None)):
    auth_hash = _check_ids(space, x_auth)
    now = int(time.time() * 1000)
    with _lock, closing(db()) as conn, conn:
        row = conn.execute("SELECT auth_hash FROM backups WHERE space = ?", (space,)).fetchone()
        if row is None:
            conn.execute(
                "INSERT INTO backups (space, auth_hash, blob, updated_at, created_at) VALUES (?, ?, ?, ?, ?)",
                (space, auth_hash, body.blob, body.updatedAt, now),
            )
        else:
            if not hmac.compare_digest(row[0], auth_hash):
                _auth_failed(request)
            conn.execute(
                "UPDATE backups SET blob = ?, updated_at = ? WHERE space = ?",
                (body.blob, body.updatedAt, space),
            )
    return {"ok": True, "updatedAt": body.updatedAt}
