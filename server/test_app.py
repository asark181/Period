import os
import tempfile

os.environ["NUANYUE_DATA"] = tempfile.mkdtemp()
os.environ["NUANYUE_DIST"] = tempfile.mkdtemp()

from fastapi.testclient import TestClient  # noqa: E402

import app as server  # noqa: E402

c = TestClient(server.app)
SPACE = "a" * 64
AUTH = "b" * 64


def test_roundtrip_and_auth():
    assert c.get(f"/api/backup/{SPACE}", headers={"X-Auth": AUTH}).status_code == 404
    r = c.put(f"/api/backup/{SPACE}", json={"blob": "cipher", "updatedAt": 5}, headers={"X-Auth": AUTH})
    assert r.status_code == 200
    r = c.get(f"/api/backup/{SPACE}", headers={"X-Auth": AUTH})
    assert r.json() == {"blob": "cipher", "updatedAt": 5}
    assert c.get(f"/api/backup/{SPACE}", headers={"X-Auth": "c" * 64}).status_code == 403
    assert c.put(f"/api/backup/{SPACE}", json={"blob": "evil", "updatedAt": 9}, headers={"X-Auth": "c" * 64}).status_code == 403
    assert c.get(f"/api/backup/{SPACE}", headers={"X-Auth": AUTH}).json()["blob"] == "cipher"


def test_validation():
    assert c.get("/api/backup/xyz", headers={"X-Auth": AUTH}).status_code == 400
    assert c.get(f"/api/backup/{SPACE}").status_code == 401
    assert c.get("/api/version").status_code == 404
    assert c.get("/api/health").json()["ok"] is True
