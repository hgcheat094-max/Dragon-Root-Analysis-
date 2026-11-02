"""SQLAlchemy tables. Mirrors migrations/001_initial.sql."""
from __future__ import annotations

from datetime import date, datetime
from typing import Optional

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from .db import Base


class Manufacturer(Base):
    __tablename__ = "manufacturers"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(32), unique=True)  # VIVO | IQOO


class Device(Base):
    __tablename__ = "devices"
    __table_args__ = (UniqueConstraint("manufacturer", "model_number", name="uq_device_model"),)
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    manufacturer: Mapped[str] = mapped_column(String(16))
    marketing_name: Mapped[str] = mapped_column(String(128))
    model_number: Mapped[str] = mapped_column(String(64))
    codename: Mapped[Optional[str]] = mapped_column(String(64))
    region: Mapped[Optional[str]] = mapped_column(String(32))
    chipset: Mapped[Optional[str]] = mapped_column(String(64))
    launch_android: Mapped[Optional[str]] = mapped_column(String(16))
    update_history: Mapped[Optional[str]] = mapped_column(Text)
    support_url: Mapped[Optional[str]] = mapped_column(String(512))
    last_verified: Mapped[Optional[date]] = mapped_column(Date)
    is_sample: Mapped[bool] = mapped_column(Boolean, default=False)


class DeviceAlias(Base):
    """Historical model identifiers are preserved here, never overwritten."""
    __tablename__ = "device_aliases"
    __table_args__ = (UniqueConstraint("device_id", "alias", name="uq_device_alias"),)
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(ForeignKey("devices.id"))
    alias: Mapped[str] = mapped_column(String(64))


class Firmware(Base):
    __tablename__ = "firmware"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    manufacturer: Mapped[str] = mapped_column(String(16))
    device_model: Mapped[str] = mapped_column(String(128))
    model_number: Mapped[Optional[str]] = mapped_column(String(64))
    region: Mapped[Optional[str]] = mapped_column(String(32))
    build_number: Mapped[str] = mapped_column(String(128))
    android_version: Mapped[Optional[str]] = mapped_column(String(16))
    vendor_os: Mapped[Optional[str]] = mapped_column(String(64))
    security_patch: Mapped[Optional[str]] = mapped_column(String(16))
    release_date: Mapped[Optional[date]] = mapped_column(Date)
    kernel_version: Mapped[Optional[str]] = mapped_column(String(128))
    release_notes: Mapped[Optional[str]] = mapped_column(Text)
    download_url: Mapped[Optional[str]] = mapped_column(String(512))
    source_type: Mapped[str] = mapped_column(String(64), default="Unspecified")
    official: Mapped[bool] = mapped_column(Boolean, default=False)
    source_grade: Mapped[str] = mapped_column(String(1), default="D")
    verification: Mapped[str] = mapped_column(String(16), default="UNVERIFIED")
    boot_sha256: Mapped[Optional[str]] = mapped_column(String(64))  # official boot image hash, if published
    last_checked: Mapped[Optional[date]] = mapped_column(Date)
    is_sample: Mapped[bool] = mapped_column(Boolean, default=False)


class KernelProfile(Base):
    __tablename__ = "kernel_profiles"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    device_model: Mapped[str] = mapped_column(String(128))
    build_number: Mapped[Optional[str]] = mapped_column(String(128))
    kernel_release: Mapped[str] = mapped_column(String(128))
    kernel_build: Mapped[Optional[str]] = mapped_column(Text)
    android_version: Mapped[Optional[str]] = mapped_column(String(16))
    security_patch: Mapped[Optional[str]] = mapped_column(String(16))
    known_vendor_changes: Mapped[Optional[str]] = mapped_column(Text)
    source: Mapped[Optional[str]] = mapped_column(String(256))
    source_grade: Mapped[str] = mapped_column(String(1), default="D")
    last_checked: Mapped[Optional[date]] = mapped_column(Date)
    is_sample: Mapped[bool] = mapped_column(Boolean, default=False)


class SecurityAdvisory(Base):
    __tablename__ = "security_advisories"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    title: Mapped[str] = mapped_column(String(256))
    cve: Mapped[Optional[str]] = mapped_column(String(32))
    url: Mapped[Optional[str]] = mapped_column(String(512))
    published_date: Mapped[Optional[date]] = mapped_column(Date)
    affected_component: Mapped[Optional[str]] = mapped_column(String(128))
    source_grade: Mapped[str] = mapped_column(String(1), default="D")
    is_sample: Mapped[bool] = mapped_column(Boolean, default=False)


class ResearchProject(Base):
    __tablename__ = "research_projects"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    name: Mapped[str] = mapped_column(String(128))
    category: Mapped[str] = mapped_column(String(32), default="RESEARCH_METHOD")
    researcher: Mapped[Optional[str]] = mapped_column(String(128))
    source_url: Mapped[Optional[str]] = mapped_column(String(512))
    cve: Mapped[Optional[str]] = mapped_column(String(32))
    affected_component: Mapped[Optional[str]] = mapped_column(String(128))
    min_api: Mapped[Optional[int]] = mapped_column(Integer)
    max_api: Mapped[Optional[int]] = mapped_column(Integer)
    kernel_min: Mapped[Optional[str]] = mapped_column(String(32))
    kernel_max: Mapped[Optional[str]] = mapped_column(String(32))
    architectures: Mapped[Optional[str]] = mapped_column(String(128))
    bootloader_requirement: Mapped[str] = mapped_column(String(24), default="NOT_ENTERED")
    mitigations: Mapped[Optional[str]] = mapped_column(Text)
    limitations: Mapped[Optional[str]] = mapped_column(Text)
    notes: Mapped[Optional[str]] = mapped_column(Text)
    source_grade: Mapped[str] = mapped_column(String(1), default="D")
    last_reviewed: Mapped[Optional[date]] = mapped_column(Date)
    is_sample: Mapped[bool] = mapped_column(Boolean, default=False)


class EvidenceSource(Base):
    __tablename__ = "evidence_sources"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    title: Mapped[str] = mapped_column(String(256))
    url: Mapped[Optional[str]] = mapped_column(String(512))
    grade: Mapped[str] = mapped_column(String(1), default="D")
    source_date: Mapped[Optional[date]] = mapped_column(Date)
    last_reviewed: Mapped[Optional[date]] = mapped_column(Date)
    is_sample: Mapped[bool] = mapped_column(Boolean, default=False)


class CompatibilityRecord(Base):
    __tablename__ = "compatibility_records"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    project_id: Mapped[str] = mapped_column(ForeignKey("research_projects.id"))
    device_model: Mapped[str] = mapped_column(String(128))
    region: Mapped[Optional[str]] = mapped_column(String(32))
    build_number: Mapped[str] = mapped_column(String(128))
    kernel_release: Mapped[str] = mapped_column(String(128))
    status: Mapped[str] = mapped_column(String(24), default="UNKNOWN")
    summary: Mapped[Optional[str]] = mapped_column(Text)
    evidence_ids: Mapped[str] = mapped_column(Text, default="")  # comma-separated evidence_sources ids
    last_reviewed: Mapped[Optional[date]] = mapped_column(Date)
    is_sample: Mapped[bool] = mapped_column(Boolean, default=False)


class VerificationHistory(Base):
    """Review trail: every change to a compatibility record's status is logged."""
    __tablename__ = "verification_history"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    record_id: Mapped[str] = mapped_column(String(64))
    reviewer_role: Mapped[str] = mapped_column(String(32))
    old_status: Mapped[Optional[str]] = mapped_column(String(24))
    new_status: Mapped[str] = mapped_column(String(24))
    note: Mapped[Optional[str]] = mapped_column(Text)
    at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class DatabaseRelease(Base):
    """A signed release envelope: payload text + detached ECDSA signature."""
    __tablename__ = "database_releases"
    version: Mapped[int] = mapped_column(Integer, primary_key=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    source: Mapped[Optional[str]] = mapped_column(String(256))
    changelog: Mapped[Optional[str]] = mapped_column(Text)
    payload: Mapped[str] = mapped_column(Text)
    signature: Mapped[str] = mapped_column(Text)


class UpdateHistory(Base):
    __tablename__ = "update_history"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    version: Mapped[int] = mapped_column(Integer)
    action: Mapped[str] = mapped_column(String(32))
    actor_role: Mapped[str] = mapped_column(String(32))
    at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
