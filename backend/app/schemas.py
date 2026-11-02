from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, HttpUrl

SHA256_RE = r"^[0-9a-fA-F]{64}$"


class ORM(BaseModel):
    model_config = ConfigDict(from_attributes=True)


class DeviceOut(ORM):
    id: str
    manufacturer: str
    marketing_name: str
    model_number: str
    codename: Optional[str] = None
    region: Optional[str] = None
    chipset: Optional[str] = None
    launch_android: Optional[str] = None
    update_history: Optional[str] = None
    support_url: Optional[str] = None
    last_verified: Optional[date] = None
    is_sample: bool = False


class FirmwareOut(ORM):
    id: str
    manufacturer: str
    device_model: str
    model_number: Optional[str] = None
    region: Optional[str] = None
    build_number: str
    android_version: Optional[str] = None
    vendor_os: Optional[str] = None
    security_patch: Optional[str] = None
    release_date: Optional[date] = None
    kernel_version: Optional[str] = None
    release_notes: Optional[str] = None
    download_url: Optional[str] = None
    source_type: str
    official: bool
    source_grade: str
    verification: str
    last_checked: Optional[date] = None
    is_sample: bool = False


class KernelOut(ORM):
    id: str
    device_model: str
    build_number: Optional[str] = None
    kernel_release: str
    android_version: Optional[str] = None
    security_patch: Optional[str] = None
    known_vendor_changes: Optional[str] = None
    source: Optional[str] = None
    source_grade: str
    last_checked: Optional[date] = None
    is_sample: bool = False


class ResearchOut(ORM):
    id: str
    name: str
    category: str
    researcher: Optional[str] = None
    source_url: Optional[str] = None
    cve: Optional[str] = None
    affected_component: Optional[str] = None
    min_api: Optional[int] = None
    max_api: Optional[int] = None
    kernel_min: Optional[str] = None
    kernel_max: Optional[str] = None
    architectures: Optional[str] = None
    bootloader_requirement: str
    mitigations: Optional[str] = None
    limitations: Optional[str] = None
    notes: Optional[str] = None
    source_grade: str
    last_reviewed: Optional[date] = None
    needs_review: bool = False
    is_sample: bool = False


class CompatOut(ORM):
    id: str
    project_id: str
    device_model: str
    region: Optional[str] = None
    build_number: str
    kernel_release: str
    status: str
    summary: Optional[str] = None
    evidence_ids: str
    last_reviewed: Optional[date] = None
    needs_review: bool = False
    is_sample: bool = False


class UpdateMetaOut(ORM):
    version: int
    created_at: datetime
    source: Optional[str] = None
    changelog: Optional[str] = None


class ReleaseEnvelope(BaseModel):
    payload: str = Field(min_length=2, max_length=10_000_000)
    signature: str = Field(min_length=4, max_length=2048)


class DeviceIn(BaseModel):
    id: str = Field(pattern=r"^[a-z0-9][a-z0-9._-]{1,62}$")
    manufacturer: str = Field(pattern=r"^(VIVO|IQOO)$")
    marketing_name: str = Field(min_length=2, max_length=128)
    model_number: str = Field(min_length=2, max_length=64)
    codename: Optional[str] = Field(default=None, max_length=64)
    region: Optional[str] = Field(default=None, max_length=32)
    chipset: Optional[str] = Field(default=None, max_length=64)
    support_url: Optional[HttpUrl] = None


class FirmwareIn(BaseModel):
    id: str = Field(pattern=r"^[a-z0-9][a-z0-9._-]{1,62}$")
    manufacturer: str = Field(pattern=r"^(VIVO|IQOO)$")
    device_model: str = Field(min_length=2, max_length=128)
    model_number: Optional[str] = Field(default=None, max_length=64)
    region: Optional[str] = Field(default=None, max_length=32)
    build_number: str = Field(min_length=2, max_length=128)
    android_version: Optional[str] = Field(default=None, max_length=16)
    vendor_os: Optional[str] = Field(default=None, max_length=64)
    security_patch: Optional[str] = Field(default=None, pattern=r"^\d{4}-\d{2}-\d{2}$")
    release_date: Optional[date] = None
    kernel_version: Optional[str] = Field(default=None, max_length=128)
    release_notes: Optional[str] = Field(default=None, max_length=20_000)
    download_url: Optional[HttpUrl] = None
    source_type: str = Field(default="Unspecified", max_length=64)
    official: bool = False
    source_grade: str = Field(default="D", pattern=r"^[ABCD]$")
    boot_sha256: Optional[str] = Field(default=None, pattern=SHA256_RE)


class DeviceAnalyzeIn(BaseModel):
    """Only technical build facts. No device identifiers (IMEI, serial, account, etc.) are accepted."""
    model: str = Field(min_length=1, max_length=128)
    build_number: str = Field(min_length=1, max_length=128)
    kernel_release: Optional[str] = Field(default=None, max_length=128)
    android_api: Optional[int] = Field(default=None, ge=1, le=100)
    region: Optional[str] = Field(default=None, max_length=32)


class BootImageAnalyzeIn(BaseModel):
    """Hash and header metadata only — the image file itself is never uploaded."""
    sha256: str = Field(pattern=SHA256_RE)
    format: Optional[str] = Field(default=None, max_length=64)
    header_version: Optional[int] = Field(default=None, ge=0, le=10)
    size: Optional[int] = Field(default=None, ge=0)


class ReviewIn(BaseModel):
    status: str = Field(pattern=r"^(VERIFIED|PARTIALLY_VERIFIED|EXPERIMENTAL|UNSUPPORTED|UNKNOWN)$")
    note: Optional[str] = Field(default=None, max_length=2000)
