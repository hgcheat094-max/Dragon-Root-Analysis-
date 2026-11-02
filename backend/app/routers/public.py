from datetime import date, timedelta
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from .. import models as m
from .. import schemas as s
from ..config import settings
from ..db import get_db

router = APIRouter(prefix="/api/v1")

Page = dict(limit=Query(50, ge=1, le=200), offset=Query(0, ge=0, le=100_000))


def _stale(d: Optional[date]) -> bool:
    return d is None or d < date.today() - timedelta(days=settings.stale_after_days)


def _model_filter(col_name, col_number, model: str):
    low = model.lower()
    return or_(func.lower(col_name) == low, func.lower(col_number) == low)


@router.get("/devices", response_model=list[s.DeviceOut], summary="Searchable Vivo/iQOO device catalog")
def list_devices(manufacturer: Optional[str] = Query(None, pattern="^(VIVO|IQOO|vivo|iqoo)$"),
                 q: Optional[str] = Query(None, max_length=64),
                 limit: int = Page["limit"], offset: int = Page["offset"], db: Session = Depends(get_db)):
    stmt = select(m.Device).order_by(m.Device.marketing_name)
    if manufacturer:
        stmt = stmt.where(m.Device.manufacturer == manufacturer.upper())
    if q:
        like = f"%{q.lower()}%"
        stmt = stmt.where(or_(func.lower(m.Device.marketing_name).like(like), func.lower(m.Device.model_number).like(like),
                              func.lower(m.Device.codename).like(like)))
    return db.scalars(stmt.limit(limit).offset(offset)).all()


@router.get("/devices/{device_id}", response_model=s.DeviceOut)
def get_device(device_id: str, db: Session = Depends(get_db)):
    d = db.get(m.Device, device_id)
    if d is None:  # fall back to historical identifiers
        alias = db.scalars(select(m.DeviceAlias).where(func.lower(m.DeviceAlias.alias) == device_id.lower())).first()
        d = db.get(m.Device, alias.device_id) if alias else None
    if d is None:
        raise HTTPException(404, "Device not found")
    return d


@router.get("/firmware", response_model=list[s.FirmwareOut], summary="Firmware records with source and verification labels")
def list_firmware(manufacturer: Optional[str] = Query(None, pattern="^(VIVO|IQOO|vivo|iqoo)$"),
                  model: Optional[str] = Query(None, max_length=128), region: Optional[str] = Query(None, max_length=32),
                  official: Optional[bool] = None, limit: int = Page["limit"], offset: int = Page["offset"],
                  db: Session = Depends(get_db)):
    stmt = select(m.Firmware).order_by(m.Firmware.release_date.desc())
    if manufacturer:
        stmt = stmt.where(m.Firmware.manufacturer == manufacturer.upper())
    if model:
        stmt = stmt.where(_model_filter(m.Firmware.device_model, m.Firmware.model_number, model))
    if region:
        stmt = stmt.where(func.lower(m.Firmware.region) == region.lower())
    if official is not None:
        stmt = stmt.where(m.Firmware.official == official)
    return db.scalars(stmt.limit(limit).offset(offset)).all()


@router.get("/firmware/latest", response_model=s.FirmwareOut, summary="Latest OFFICIAL firmware for an exact model (and region)")
def latest_firmware(model: str = Query(..., min_length=1, max_length=128), region: Optional[str] = Query(None, max_length=32),
                    db: Session = Depends(get_db)):
    row = _latest(db, model, region)
    if row is None:
        raise HTTPException(404, "No official firmware record is established for this model/region")
    return row


def _latest(db: Session, model: str, region: Optional[str]):
    stmt = select(m.Firmware).where(m.Firmware.official.is_(True),
                                    _model_filter(m.Firmware.device_model, m.Firmware.model_number, model))
    if region:
        stmt = stmt.where(func.lower(m.Firmware.region) == region.lower())
    stmt = stmt.order_by(m.Firmware.release_date.desc(), m.Firmware.security_patch.desc())
    return db.scalars(stmt).first()


@router.get("/kernels", response_model=list[s.KernelOut])
def list_kernels(model: Optional[str] = Query(None, max_length=128), limit: int = Page["limit"], offset: int = Page["offset"],
                 db: Session = Depends(get_db)):
    stmt = select(m.KernelProfile).order_by(m.KernelProfile.last_checked.desc())
    if model:
        stmt = stmt.where(func.lower(m.KernelProfile.device_model) == model.lower())
    return db.scalars(stmt.limit(limit).offset(offset)).all()


@router.get("/compatibility", response_model=list[s.CompatOut], summary="Compatibility records (exact device/build/kernel)")
def list_compat(project_id: Optional[str] = Query(None, max_length=64), model: Optional[str] = Query(None, max_length=128),
                limit: int = Page["limit"], offset: int = Page["offset"], db: Session = Depends(get_db)):
    stmt = select(m.CompatibilityRecord)
    if project_id:
        stmt = stmt.where(m.CompatibilityRecord.project_id == project_id)
    if model:
        stmt = stmt.where(func.lower(m.CompatibilityRecord.device_model) == model.lower())
    out = []
    for r in db.scalars(stmt.limit(limit).offset(offset)).all():
        item = s.CompatOut.model_validate(r)
        item.needs_review = _stale(r.last_reviewed)
        out.append(item)
    return out


@router.get("/research", response_model=list[s.ResearchOut], summary="Documented projects and research methods (metadata only)")
def list_research(limit: int = Page["limit"], offset: int = Page["offset"], db: Session = Depends(get_db)):
    out = []
    for r in db.scalars(select(m.ResearchProject).order_by(m.ResearchProject.name).limit(limit).offset(offset)).all():
        item = s.ResearchOut.model_validate(r)
        item.needs_review = _stale(r.last_reviewed)
        out.append(item)
    return out


@router.get("/updates", response_model=list[s.UpdateMetaOut], summary="Database release history (metadata)")
def list_updates(limit: int = Query(20, ge=1, le=100), db: Session = Depends(get_db)):
    return db.scalars(select(m.DatabaseRelease).order_by(m.DatabaseRelease.version.desc()).limit(limit)).all()


@router.get("/updates/latest", summary="Latest signed release envelope {payload, signature}")
def latest_update(db: Session = Depends(get_db)):
    r = db.scalars(select(m.DatabaseRelease).order_by(m.DatabaseRelease.version.desc())).first()
    if r is None:
        raise HTTPException(404, "No database release has been published")
    return {"payload": r.payload, "signature": r.signature}


@router.post("/device/analyze", summary="Server-side lookup for a device build (no identifiers accepted)")
def analyze_device(body: s.DeviceAnalyzeIn, db: Session = Depends(get_db)):
    latest = _latest(db, body.model, body.region)
    known = db.scalars(select(m.Firmware).where(_model_filter(m.Firmware.device_model, m.Firmware.model_number, body.model),
                                                func.lower(m.Firmware.build_number) == body.build_number.lower())).first()
    results = []
    for proj in db.scalars(select(m.ResearchProject)).all():
        status, note, rec_id = "UNKNOWN", "No verified evidence for this exact device, firmware and kernel combination.", None
        if body.kernel_release:
            rec = db.scalars(select(m.CompatibilityRecord).where(
                m.CompatibilityRecord.project_id == proj.id,
                func.lower(m.CompatibilityRecord.device_model) == body.model.lower(),
                func.lower(m.CompatibilityRecord.build_number) == body.build_number.lower(),
                func.lower(m.CompatibilityRecord.kernel_release) == body.kernel_release.lower())).first()
            if rec:
                rec_id, status, note = rec.id, rec.status, rec.summary or "Exact-build record."
                ids = [i.strip() for i in rec.evidence_ids.split(",") if i.strip()]
                grades = [e.grade for e in db.scalars(select(m.EvidenceSource).where(m.EvidenceSource.id.in_(ids))).all()] if ids else []
                strong = any(g != "D" for g in grades)
                if status in ("VERIFIED", "UNSUPPORTED") and not strong:
                    status, note = "UNKNOWN", "Record lacks grade A–C evidence; treated as unknown."
                elif status == "VERIFIED" and _stale(rec.last_reviewed):
                    status, note = "PARTIALLY_VERIFIED", "Record needs review (stale)."
        results.append({"project_id": proj.id, "status": status, "matched_record_id": rec_id, "note": note})
    return {
        "build_known": known is not None,
        "latest_official": s.FirmwareOut.model_validate(latest) if latest else None,
        "is_latest_official": None if latest is None else latest.build_number.lower() == body.build_number.lower(),
        "compatibility": results,
        "note": "Prerequisite matching (Android/kernel ranges, bootloader) is performed on the device. "
                "Evidence for other builds is never applied.",
    }


@router.post("/boot-image/analyze", summary="Match a boot image hash against official hashes (file is never uploaded)")
def analyze_boot_image(body: s.BootImageAnalyzeIn, db: Session = Depends(get_db)):
    rows = db.scalars(select(m.Firmware).where(func.lower(m.Firmware.boot_sha256) == body.sha256.lower(),
                                               m.Firmware.official.is_(True))).all()
    if not rows:
        return {"matched": False, "builds": [],
                "note": "No trusted exact-build source lists this hash, so the image is not claimed to belong to any device."}
    return {"matched": True, "builds": [{"device_model": r.device_model, "region": r.region, "build_number": r.build_number} for r in rows],
            "note": "Hash matches an official record. Always confirm the model, region and build before any use."}
