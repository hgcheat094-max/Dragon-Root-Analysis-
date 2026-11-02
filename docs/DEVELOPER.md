# Developer Guide

## Layout
```
app/src/main/java/com/dragonroot/app/
  model/      enums + value types (CompatStatus, EvidenceGrade, Reading, DeviceProfile …)
  device/     DeviceInfoCollector (+ BuildSnapshot, PropReader, FileProbe seams for tests)
  analysis/   KernelParser, EvidenceGrader, FirmwareLogic, CompatibilityEngine,
              BootImageParser, StreamScanner (SHA-256 + header + AVB footer), BootImageAnalyzer
  data/       Room entities/DAO/database, DataStore settings, BuiltInCatalog
  update/     SignatureVerifier, ReleaseImporter, ReleaseClient, Rechecker, UpdateService/Worker/Scheduler, Notifier
  ui/         Compose theme, components, navigation, view models, screens
backend/      FastAPI app, SQL migration, signing tools, tests
```
Single Gradle module with package-per-feature (simpler/faster than multi-module). Manual DI via `AppContainer`.

## Compatibility rules (`CompatibilityEngine`)
* **VERIFIED** — exact model + build + kernel record, ≥1 grade A–C evidence, reviewed within 180 days. Grade-D-only → EXPERIMENTAL (downgraded). Stale → PARTIALLY_VERIFIED. Conflict with documented prerequisites → PARTIALLY_VERIFIED.
* **UNSUPPORTED** — a documented prerequisite definitely fails *and* the project source is grade A–C (or an exact record says so with credible evidence).
* **EXPERIMENTAL** — every documented prerequisite matches, no exact record.
* **UNKNOWN** — everything else. Records for other builds are never applied (a warning names them).
* Latest-firmware-first: `FirmwareLogic.latestFor` considers **official** records only. If none exists, every result carries a "cannot be confirmed as current" warning; if the device build differs from the latest, another warning is added and `isOutdated()` flags older records.

## Automatic rechecking (`Rechecker`)
After each successful signed import: re-evaluate every project, compare with the stored snapshot, and report (a) a newer official build for the model and (b) status changes ("Magisk: Unknown → Experimental"). A notification is shown only if the user enabled it and granted permission. An update alone never produces a positive status — only rules + evidence do.

## Release format
Envelope: `{"payload": "<JSON text>", "signature": "<base64 DER ECDSA P-256/SHA-256 over payload UTF-8 bytes>"}`.
Payload keys (all arrays optional): `devices, firmware, kernelProfiles, advisories, researchProjects, compatibility, evidence`, plus `version` (int, must increase), `createdAt`, `source`, `changelog`, `isSample`. Dates are `YYYY-MM-DD`. See `app/src/main/assets/sample_release.json` (fake data) for field names. The app verifies the signature **before** parsing; a build without `dragonroot.updatePublicKey` rejects all releases. Version `0` is reserved for debug sample import.

## API (all JSON; errors: `{"error": {"code", "message", "details?"}}`)
Public (read-only, rate limited):
| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/devices` | `manufacturer`, `q`, `limit`, `offset` |
| GET | `/api/v1/devices/{id}` | also resolves historical aliases |
| GET | `/api/v1/firmware` | `manufacturer`, `model`, `region`, `official` |
| GET | `/api/v1/firmware/latest` | `model` (required), `region`; official only; 404 if not established |
| GET | `/api/v1/kernels` | `model` |
| GET | `/api/v1/compatibility` | `project_id`, `model`; `needs_review` computed |
| GET | `/api/v1/research` | project/method metadata |
| GET | `/api/v1/updates`, `/api/v1/updates/latest` | release history; latest signed envelope |
| POST | `/api/v1/device/analyze` | `{model, build_number, kernel_release?, android_api?, region?}` — no identifiers accepted |
| POST | `/api/v1/boot-image/analyze` | `{sha256, …}` — hash lookup only, file never uploaded |

Admin (`Authorization: Bearer <token>`; roles from `ADMIN_TOKENS`):
`POST /admin/v1/devices` (maintainer) · `POST /admin/v1/firmware` (maintainer; `official` requires grade A) ·
`POST /admin/v1/compatibility/{id}/review` (maintainer, reviewer; VERIFIED/UNSUPPORTED need grade A–C evidence; audit-logged) ·
`POST /admin/v1/releases` (maintainer; signature verified with `UPDATE_PUBLIC_KEY`).

## Testing
Unit tests (`app/src/test`): kernel parsing, evidence grading, firmware selection/comparison, compatibility rules, device collection with mock profiles (including the sample iQOO Z9x input, as a fixture only), boot image parsing, SHA-256, signature verification. Instrumented UI test: `app/src/androidTest`. Backend: `pytest` in `backend/`.
Real-device testing only on devices you own; do not claim support for a model until evidence is documented.

## Adding a project or evidence
Add rows via a signed release: `researchProjects` (ranges as stated by the source, `sourceGrade`, `lastReviewed`), `evidence` (title, url, grade, dates), and `compatibility` records keyed by exact `deviceModel` + `buildNumber` + `kernelRelease`, with `evidenceIds` listing evidence ids.
