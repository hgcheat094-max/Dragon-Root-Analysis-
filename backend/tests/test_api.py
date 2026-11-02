import base64
import json
from datetime import date

import pytest
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
from fastapi import FastAPI
from fastapi.testclient import TestClient

from tests.conftest import KEY

from app import models as m
from app.db import SessionLocal
from app.main import app
from app.ratelimit import RateLimitMiddleware

MAINT = {"Authorization": "Bearer tok-maintainer"}
REV = {"Authorization": "Bearer tok-reviewer"}


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c


def sign(payload: str) -> dict:
    sig = KEY.sign(payload.encode(), ec.ECDSA(hashes.SHA256()))
    return {"payload": payload, "signature": base64.b64encode(sig).decode()}


def test_health(client):
    assert client.get("/healthz").json() == {"status": "ok"}


def test_empty_catalog_and_structured_404(client):
    assert client.get("/api/v1/devices").json() == []
    r = client.get("/api/v1/devices/nope")
    assert r.status_code == 404 and r.json()["error"]["code"] == "not_found"


def test_admin_requires_token_and_role(client):
    body = {"id": "sample-dev", "manufacturer": "IQOO", "marketing_name": "SAMPLE Phone", "model_number": "SAMPLE-1"}
    assert client.post("/admin/v1/devices", json=body).status_code == 401
    assert client.post("/admin/v1/devices", json=body, headers=REV).status_code == 403
    assert client.post("/admin/v1/devices", json=body, headers=MAINT).status_code == 201
    dup = client.post("/admin/v1/devices", json={**body, "id": "sample-dev-2"}, headers=MAINT)
    assert dup.status_code == 409  # same manufacturer + model number
    assert client.get("/api/v1/devices", params={"manufacturer": "iqoo"}).json()[0]["id"] == "sample-dev"


def test_validation_errors_are_structured(client):
    r = client.post("/admin/v1/devices", json={"id": "X", "manufacturer": "OTHER"}, headers=MAINT)
    assert r.status_code == 422 and r.json()["error"]["code"] == "validation_error"
    assert client.post("/api/v1/boot-image/analyze", json={"sha256": "xyz"}).status_code == 422


def test_latest_firmware_is_official_only(client):
    assert client.get("/api/v1/firmware/latest", params={"model": "SAMPLE Phone"}).status_code == 404
    base = {"manufacturer": "IQOO", "device_model": "SAMPLE Phone", "region": "SAMPLE"}
    bad = client.post("/admin/v1/firmware", headers=MAINT, json={**base, "id": "fw-bad", "build_number": "B0", "official": True, "source_grade": "C"})
    assert bad.status_code == 422
    ok = client.post("/admin/v1/firmware", headers=MAINT, json={**base, "id": "fw-1", "build_number": "BUILD_1", "official": True,
                                                                  "source_grade": "A", "release_date": "2026-01-01"})
    assert ok.status_code == 201
    client.post("/admin/v1/firmware", headers=MAINT, json={**base, "id": "fw-3p", "build_number": "THIRD_PARTY_NEWER",
                                                            "official": False, "source_grade": "D", "release_date": "2026-06-01"})
    latest = client.get("/api/v1/firmware/latest", params={"model": "sample phone"}).json()
    assert latest["build_number"] == "BUILD_1"


def test_device_analyze_defaults_to_unknown(client):
    with SessionLocal() as db:
        db.add(m.ResearchProject(id="proj", name="Proj", source_grade="B"))
        db.commit()
    r = client.post("/api/v1/device/analyze", json={"model": "SAMPLE Phone", "build_number": "BUILD_1", "kernel_release": "5.10.1"}).json()
    assert r["is_latest_official"] is True
    assert r["compatibility"][0]["status"] == "UNKNOWN"


def test_verified_needs_credible_evidence(client):
    with SessionLocal() as db:
        db.add(m.EvidenceSource(id="ev-d", title="repost", grade="D"))
        db.add(m.CompatibilityRecord(id="rec-1", project_id="proj", device_model="SAMPLE Phone", build_number="BUILD_1",
                                     kernel_release="5.10.1", status="VERIFIED", evidence_ids="ev-d", last_reviewed=date.today()))
        db.commit()
    r = client.post("/api/v1/device/analyze", json={"model": "SAMPLE Phone", "build_number": "BUILD_1", "kernel_release": "5.10.1"}).json()
    assert r["compatibility"][0]["status"] == "UNKNOWN"
    deny = client.post("/admin/v1/compatibility/rec-1/review", headers=REV, json={"status": "VERIFIED"})
    assert deny.status_code == 422
    allow = client.post("/admin/v1/compatibility/rec-1/review", headers=REV, json={"status": "EXPERIMENTAL", "note": "weak evidence"})
    assert allow.status_code == 200


def test_boot_image_hash_unmatched(client):
    r = client.post("/api/v1/boot-image/analyze", json={"sha256": "a" * 64}).json()
    assert r["matched"] is False


def test_signed_release_flow(client):
    payload = json.dumps({"version": 1, "source": "test", "changelog": "first"})
    assert client.post("/admin/v1/releases", headers=MAINT, json={"payload": payload, "signature": "AAAA"}).status_code == 400
    assert client.post("/admin/v1/releases", headers=MAINT, json=sign(payload)).status_code == 201
    assert client.post("/admin/v1/releases", headers=MAINT, json=sign(payload)).status_code == 409
    env = client.get("/api/v1/updates/latest").json()
    assert env["payload"] == payload
    assert client.get("/api/v1/updates").json()[0]["version"] == 1


def test_version_zero_reserved_for_samples(client):
    assert client.post("/admin/v1/releases", headers=MAINT, json=sign(json.dumps({"version": 0}))).status_code == 400


def test_rate_limit():
    tiny = FastAPI()
    tiny.add_middleware(RateLimitMiddleware, per_minute=3)

    @tiny.get("/x")
    def x():
        return {}

    with TestClient(tiny) as c:
        codes = [c.get("/x").status_code for _ in range(5)]
    assert codes == [200, 200, 200, 429, 429]
