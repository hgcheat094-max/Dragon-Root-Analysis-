import json
from datetime import date

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from .. import models as m
from .. import schemas as s
from ..config import settings
from ..db import get_db
from ..security import payload_version, require_role, verify_release_signature

router = APIRouter(prefix="/admin/v1", tags=["admin"])


@router.post("/devices", response_model=s.DeviceOut, status_code=201, summary="Add a device without an APK release")
def add_device(body: s.DeviceIn, role: str = Depends(require_role("maintainer")), db: Session = Depends(get_db)):
    dup = db.scalars(select(m.Device).where(m.Device.manufacturer == body.manufacturer,
                                            m.Device.model_number == body.model_number)).first()
    if dup or db.get(m.Device, body.id):
        raise HTTPException(409, "A device with this id or manufacturer/model number already exists")
    row = m.Device(**{**body.model_dump(mode="json"), "last_verified": date.today()})
    db.add(row)
    db.commit()
    return row


@router.post("/firmware", response_model=s.FirmwareOut, status_code=201)
def add_firmware(body: s.FirmwareIn, role: str = Depends(require_role("maintainer")), db: Session = Depends(get_db)):
    if body.official and body.source_grade != "A":
        raise HTTPException(422, "Records marked official must be backed by grade A (official manufacturer) sources")
    if db.get(m.Firmware, body.id):
        raise HTTPException(409, "Firmware id already exists")
    row = m.Firmware(**body.model_dump(mode="json"), last_checked=date.today())
    row.release_date = body.release_date
    db.add(row)
    db.commit()
    return row


@router.post("/compatibility/{record_id}/review", summary="Change a compatibility record's status with an audit trail")
def review_record(record_id: str, body: s.ReviewIn, role: str = Depends(require_role("maintainer", "reviewer")),
                  db: Session = Depends(get_db)):
    rec = db.get(m.CompatibilityRecord, record_id)
    if rec is None:
        raise HTTPException(404, "Record not found")
    if body.status in ("VERIFIED", "UNSUPPORTED"):
        ids = [i.strip() for i in rec.evidence_ids.split(",") if i.strip()]
        grades = [e.grade for e in db.scalars(select(m.EvidenceSource).where(m.EvidenceSource.id.in_(ids))).all()] if ids else []
        if not any(g != "D" for g in grades):
            raise HTTPException(422, f"{body.status} requires at least one grade A–C evidence source")
    db.add(m.VerificationHistory(record_id=rec.id, reviewer_role=role, old_status=rec.status, new_status=body.status, note=body.note))
    rec.status, rec.last_reviewed = body.status, date.today()
    db.commit()
    return {"id": rec.id, "status": rec.status, "last_reviewed": rec.last_reviewed.isoformat()}


@router.post("/releases", status_code=201, summary="Publish a signed database release")
def publish_release(body: s.ReleaseEnvelope, role: str = Depends(require_role("maintainer")), db: Session = Depends(get_db)):
    if not settings.update_public_key:
        raise HTTPException(400, "UPDATE_PUBLIC_KEY is not configured; refusing to store unauthenticated releases")
    if not verify_release_signature(body.payload, body.signature, settings.update_public_key):
        raise HTTPException(400, "Signature verification failed")
    try:
        data = json.loads(body.payload)
        version = payload_version(body.payload)
    except (ValueError, KeyError, TypeError):
        raise HTTPException(400, "Payload must be JSON with an integer 'version'")
    if version <= 0:
        raise HTTPException(400, "Release version must be positive (version 0 is reserved for sample data)")
    latest = db.scalars(select(m.DatabaseRelease).order_by(m.DatabaseRelease.version.desc())).first()
    if latest and version <= latest.version:
        raise HTTPException(409, f"Version must be greater than the latest release ({latest.version})")
    db.add(m.DatabaseRelease(version=version, source=data.get("source"), changelog=data.get("changelog"),
                             payload=body.payload, signature=body.signature))
    db.add(m.UpdateHistory(version=version, action="published", actor_role=role))
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(409, "Release version already exists")
    return {"version": version}
