# Privacy Policy (template — adapt before publishing)

**Effective date:** [DATE] · **Controller / contact:** [NAME, EMAIL] · **Data deletion requests:** [EMAIL]

## What the app does with data
* **Device facts** (model, build, kernel, etc.) are read on your phone and displayed to you. They are stored only in local app storage and are **not uploaded**.
* **Boot image files** you select are processed locally. They are never uploaded.
* **Database sync** (if your build has an update source configured) downloads signed public data. The request contains no device identifiers, account data, or file contents; the server may log your IP address and a `User-Agent` of `DragonRoot/<version>` for security and rate limiting ([RETENTION PERIOD]).
* **Server-side lookups** (`/device/analyze`, `/boot-image/analyze`) are only made if a feature explicitly says so. They accept build facts or a file hash — never IMEI, serial number, account IDs, contacts, messages or files.

## Permissions
* Internet / network state — database sync.
* Notifications (optional, requested only if you enable it) — tells you when compatibility status changes.
No contacts, messages, call logs, location, camera, microphone or storage-wide permissions are requested. Files are opened through the system file picker.

## Analytics
None are included. If you add analytics, disclose them here and obtain consent first.

## Your choices
* Turn off background sync or restrict it to Wi-Fi in Settings.
* *Settings → Delete all local data* removes all stored records and settings. Uninstalling also removes them.
* Contact [EMAIL] to ask about any server-side logs.

## Security
HTTPS with certificate validation; database releases are verified with an ECDSA signature before import; nothing downloaded is executed.

## Changes
[Describe how updates to this policy are announced.]
