#!/usr/bin/env python3
"""Pre-load period history into an invite code, encrypted exactly the way the app does it.

    python deploy/seed_invite.py deploy/private/her_periods.csv [--code NY...] [--nickname 宝贝] [--note "..."]

CSV columns: start (YYYY-MM-DD), days (period length; empty = still ongoing).
The server only ever receives ciphertext. Re-running with the same code is safe: record ids
are derived from the code and start date, so the app merges instead of duplicating.
"""
import argparse
import base64
import csv
import datetime as dt
import hashlib
import json
import os
import secrets
import sys
import time
import urllib.error
import urllib.request
import uuid

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
EPOCH = dt.date(1970, 1, 1)


def normalize(code: str) -> str:
    c = "".join(ch for ch in code.upper() if ch.isalnum())
    if len(c) != 14 or not c.startswith("NY") or any(ch not in ALPHABET for ch in c[2:]):
        sys.exit(f"bad invite code: {code}")
    return c


def display(c: str) -> str:
    return f"{c[:2]}-{c[2:6]}-{c[6:10]}-{c[10:]}"


def keys(account: str, password: str):
    salt = ("nuanyue:salt:" + account).encode()
    material = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 120_000, dklen=64)
    space = hashlib.sha256(("nuanyue:space:" + account).encode()).hexdigest()
    return space, material[:32], material[32:].hex()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("csv")
    ap.add_argument("--code")
    ap.add_argument("--nickname", default="")
    ap.add_argument("--note", default="")
    ap.add_argument("--server", default="https://leafy.fxxkopenai.com")
    a = ap.parse_args()

    code = normalize(a.code) if a.code else "NY" + "".join(secrets.choice(ALPHABET) for _ in range(12))
    account = "inv-" + code.lower()
    space, enc_key, auth = keys(account, code)

    now = int(time.time() * 1000)
    periods = []
    for row in csv.DictReader(open(a.csv, encoding="utf-8")):
        start = dt.date.fromisoformat(row["start"].strip())
        days = (row.get("days") or "").strip()
        start_day = (start - EPOCH).days
        periods.append({
            "id": str(uuid.uuid5(uuid.NAMESPACE_URL, f"nuanyue:{code}:{start.isoformat()}")),
            "startDay": start_day,
            "endDay": start_day + int(days) - 1 if days else None,
            "updatedAt": now,
            "deleted": False,
        })
    periods.sort(key=lambda p: p["startDay"])
    backup = {
        "version": 1,
        "exportedAt": now,
        "periods": periods,
        "records": [],
        "profile": {"nickname": a.nickname, "loveNote": a.note},
    }
    plain = json.dumps(backup, ensure_ascii=False).encode()
    iv = os.urandom(12)
    blob = base64.b64encode(iv + AESGCM(enc_key).encrypt(iv, plain, None)).decode()

    req = urllib.request.Request(
        f"{a.server}/api/backup/{space}", method="PUT",
        data=json.dumps({"blob": blob, "updatedAt": now}).encode(),
        headers={"Content-Type": "application/json", "X-Auth": auth},
    )
    try:
        urllib.request.urlopen(req, timeout=20).read()
    except urllib.error.HTTPError as e:
        sys.exit(f"upload failed: {e.code} {e.read().decode()}")

    # Read it back and decrypt, to prove the round trip.
    got = json.loads(urllib.request.urlopen(urllib.request.Request(
        f"{a.server}/api/backup/{space}", headers={"X-Auth": auth}), timeout=20).read())
    raw = base64.b64decode(got["blob"])
    back = json.loads(AESGCM(enc_key).decrypt(raw[:12], raw[12:], None))
    assert len(back["periods"]) == len(periods)
    print(f"uploaded {len(periods)} periods")
    print(f"INVITE CODE: {display(code)}")


if __name__ == "__main__":
    main()
