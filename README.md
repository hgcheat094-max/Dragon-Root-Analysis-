# Dragon Root

**Vivo & iQOO Device Intelligence, Kernel Analysis and Temporary-Root Compatibility Research**

A read-only Android research app plus an evidence-database backend. It identifies the device and firmware,
analyzes kernel and boot-image metadata, and reports — with sources — what is *documented* about root-management
projects (Magisk, KernelSU, KernelSU Next, APatch) for the **exact** device, build and kernel.

It never executes exploits, installs root tools, patches or flashes images, or bypasses a locked bootloader.
With no evidence the answer is **UNKNOWN**.

* Android: Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, WorkManager. **minSdk 26 (Android 8.0)**, target/compile 35.
* Backend: FastAPI, SQLAlchemy, PostgreSQL (SQLite for tests).

## Build the APK

Requirements: JDK 17, Android SDK (platform 35), internet access for Gradle dependencies.

```bash
# one-time: create the Gradle wrapper (the wrapper JAR is binary and not included)
gradle wrapper --gradle-version 8.9        # or just open the folder in Android Studio (Ladybug+)

./gradlew :app:testDebugUnitTest           # unit tests
./gradlew :app:assembleDebug               # -> app/build/outputs/apk/debug/app-debug.apk
```

Install: `adb install app/build/outputs/apk/debug/app-debug.apk`

No Gradle installed? Push this folder to GitHub: `.github/workflows/android.yml` runs the tests and uploads the debug APK as an artifact.

### Release build (signed)
Create `keystore.properties` in the project root (never commit it):
```
storeFile=/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```
then `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
Without `keystore.properties` the release APK is produced unsigned.

### Connect the app to your update server (optional)
Add to `~/.gradle/gradle.properties` or `gradle.properties`:
```
dragonroot.updateBaseUrl=https://your-host.example
dragonroot.updatePublicKey=<base64 X.509 EC P-256 public key from backend/tools/make_keys.py>
```
Without both values the app is offline-only: no network sync, and database releases cannot be imported.
In **debug** builds, *Update History → Import SAMPLE data* loads clearly fake demo records.

## Run the backend

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

python tools/make_keys.py release-signing-key.pem          # prints the public key
export DATABASE_URL=postgresql+psycopg://user:pass@localhost/dragonroot
psql "$DATABASE_URL" -f migrations/001_initial.sql         # or let SQLite auto-create for dev
export ADMIN_TOKENS="long-random-token:maintainer,other-token:reviewer"
export UPDATE_PUBLIC_KEY=<printed public key>
uvicorn app.main:app --host 0.0.0.0 --port 8000            # put HTTPS (reverse proxy) in front
pytest                                                      # backend tests
```
Interactive API docs: `/docs`. Publish a release:
```bash
python tools/sign_release.py release-signing-key.pem payload.json > envelope.json
curl -H "Authorization: Bearer long-random-token" -H "Content-Type: application/json" \
     --data @envelope.json https://your-host.example/admin/v1/releases
```

## Documentation
* `docs/USER_GUIDE.md` — using the app
* `docs/DEVELOPER.md` — architecture, rules engine, API reference, data format
* `docs/PRIVACY_POLICY_TEMPLATE.md` — template to adapt before publishing
* `docs/KNOWN_LIMITATIONS.md` — read this first
