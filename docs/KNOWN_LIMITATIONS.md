# Known Limitations

1. **Not compiled or run in the environment that generated this project.** There was no Android SDK, Gradle or network access, so the Kotlin sources, Compose UI and Gradle build are unbuilt and untested here. Expect to fix a few compile errors on first build (open in Android Studio). The Python signing tool was verified against Java's ECDSA verifier; the backend tests were written but could not be run (FastAPI was not installed).
2. **No real data is bundled.** Firmware, kernel, advisory and compatibility records are empty until you publish signed releases from official sources. Only four catalog stubs (Magisk, KernelSU, KernelSU Next, APatch) ship, with source links but **no** supported-version/kernel/architecture data — enter those from each project's official docs. Verify the links before relying on them (KernelSU Next in particular).
3. **No automatic firmware crawler.** The spec says to respect site terms and not assume downloads exist; ingestion is left to maintainers (admin API / signed releases). The app therefore cannot yet "check the latest firmware" by itself; it uses the latest *official record in the database*, and says plainly when none exists.
4. **Vendor property names are community-observed.** Funtouch/OriginOS version (`ro.vivo.os.*`), bootloader state (`ro.boot.*`) and the build-number mapping may differ per model; confidence is labelled and values fall back to "Unavailable". Validate on real devices with owners' permission before claiming support for a model.
5. **Android restricts app access.** `/proc/version`, SELinux state and some properties are often unreadable. Root detection only checks for `su` binaries and is a weak signal.
6. **Region cannot be read reliably** from an app, so region-specific matching shows a warning rather than a result.
7. **Boot images:** headers v0–v4 and vendor_boot v3/v4 headers only; vendor ramdisk tables, compressed/vendor-specific containers and signature contents are not decoded. AVB footer detection is a magic-number check.
8. **Prerequisite matching** is performed on-device; the server's `/device/analyze` does exact-record lookup only.
9. **Rate limiting** is in-process and per instance; use a gateway for production. Admin auth is static bearer tokens with two roles — replace with your identity provider for real deployments.
10. **Database cache** uses destructive Room migration (it is re-downloadable); a Room schema export is configured in `app/schemas/`.
11. **Accessibility:** colours are always paired with text labels and controls have content descriptions, but TalkBack/large-font passes on real devices are still to do.
12. **No flashing, patching, repacking or exploit execution exist or are planned in this release.**
