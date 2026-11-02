-- Dragon Root backend schema, v1 (PostgreSQL). Mirrors app/models.py.
BEGIN;

CREATE TABLE manufacturers (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
);
INSERT INTO manufacturers (name) VALUES ('VIVO'), ('IQOO');

CREATE TABLE devices (
    id              VARCHAR(64) PRIMARY KEY,
    manufacturer    VARCHAR(16) NOT NULL,
    marketing_name  VARCHAR(128) NOT NULL,
    model_number    VARCHAR(64) NOT NULL,
    codename        VARCHAR(64),
    region          VARCHAR(32),
    chipset         VARCHAR(64),
    launch_android  VARCHAR(16),
    update_history  TEXT,
    support_url     VARCHAR(512),
    last_verified   DATE,
    is_sample       BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_device_model UNIQUE (manufacturer, model_number)
);

CREATE TABLE device_aliases (
    id        SERIAL PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL REFERENCES devices(id),
    alias     VARCHAR(64) NOT NULL,
    CONSTRAINT uq_device_alias UNIQUE (device_id, alias)
);

CREATE TABLE firmware (
    id              VARCHAR(64) PRIMARY KEY,
    manufacturer    VARCHAR(16) NOT NULL,
    device_model    VARCHAR(128) NOT NULL,
    model_number    VARCHAR(64),
    region          VARCHAR(32),
    build_number    VARCHAR(128) NOT NULL,
    android_version VARCHAR(16),
    vendor_os       VARCHAR(64),
    security_patch  VARCHAR(16),
    release_date    DATE,
    kernel_version  VARCHAR(128),
    release_notes   TEXT,
    download_url    VARCHAR(512),
    source_type     VARCHAR(64) NOT NULL DEFAULT 'Unspecified',
    official        BOOLEAN NOT NULL DEFAULT FALSE,
    source_grade    CHAR(1) NOT NULL DEFAULT 'D' CHECK (source_grade IN ('A','B','C','D')),
    verification    VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    boot_sha256     CHAR(64),
    last_checked    DATE,
    is_sample       BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT official_needs_grade_a CHECK (NOT official OR source_grade = 'A')
);
CREATE INDEX ix_firmware_model ON firmware (lower(device_model));
CREATE INDEX ix_firmware_boot_sha ON firmware (lower(boot_sha256));

CREATE TABLE kernel_profiles (
    id                   VARCHAR(64) PRIMARY KEY,
    device_model         VARCHAR(128) NOT NULL,
    build_number         VARCHAR(128),
    kernel_release       VARCHAR(128) NOT NULL,
    kernel_build         TEXT,
    android_version      VARCHAR(16),
    security_patch       VARCHAR(16),
    known_vendor_changes TEXT,
    source               VARCHAR(256),
    source_grade         CHAR(1) NOT NULL DEFAULT 'D',
    last_checked         DATE,
    is_sample            BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE security_advisories (
    id                 VARCHAR(64) PRIMARY KEY,
    title              VARCHAR(256) NOT NULL,
    cve                VARCHAR(32),
    url                VARCHAR(512),
    published_date     DATE,
    affected_component VARCHAR(128),
    source_grade       CHAR(1) NOT NULL DEFAULT 'D',
    is_sample          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE research_projects (
    id                     VARCHAR(64) PRIMARY KEY,
    name                   VARCHAR(128) NOT NULL,
    category               VARCHAR(32) NOT NULL DEFAULT 'RESEARCH_METHOD',
    researcher             VARCHAR(128),
    source_url             VARCHAR(512),
    cve                    VARCHAR(32),
    affected_component     VARCHAR(128),
    min_api                INTEGER,
    max_api                INTEGER,
    kernel_min             VARCHAR(32),
    kernel_max             VARCHAR(32),
    architectures          VARCHAR(128),
    bootloader_requirement VARCHAR(24) NOT NULL DEFAULT 'NOT_ENTERED',
    mitigations            TEXT,
    limitations            TEXT,
    notes                  TEXT,
    source_grade           CHAR(1) NOT NULL DEFAULT 'D',
    last_reviewed          DATE,
    is_sample              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE evidence_sources (
    id            VARCHAR(64) PRIMARY KEY,
    title         VARCHAR(256) NOT NULL,
    url           VARCHAR(512),
    grade         CHAR(1) NOT NULL DEFAULT 'D' CHECK (grade IN ('A','B','C','D')),
    source_date   DATE,
    last_reviewed DATE,
    is_sample     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE compatibility_records (
    id             VARCHAR(64) PRIMARY KEY,
    project_id     VARCHAR(64) NOT NULL REFERENCES research_projects(id),
    device_model   VARCHAR(128) NOT NULL,
    region         VARCHAR(32),
    build_number   VARCHAR(128) NOT NULL,
    kernel_release VARCHAR(128) NOT NULL,
    status         VARCHAR(24) NOT NULL DEFAULT 'UNKNOWN',
    summary        TEXT,
    evidence_ids   TEXT NOT NULL DEFAULT '',
    last_reviewed  DATE,
    is_sample      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_compat_exact ON compatibility_records (project_id, lower(device_model), lower(build_number));

CREATE TABLE verification_history (
    id            SERIAL PRIMARY KEY,
    record_id     VARCHAR(64) NOT NULL,
    reviewer_role VARCHAR(32) NOT NULL,
    old_status    VARCHAR(24),
    new_status    VARCHAR(24) NOT NULL,
    note          TEXT,
    at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE database_releases (
    version    INTEGER PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    source     VARCHAR(256),
    changelog  TEXT,
    payload    TEXT NOT NULL,
    signature  TEXT NOT NULL
);

CREATE TABLE update_history (
    id         SERIAL PRIMARY KEY,
    version    INTEGER NOT NULL,
    action     VARCHAR(32) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    at         TIMESTAMP NOT NULL DEFAULT now()
);

COMMIT;
