# Developing Park Ping

Technical reference for contributors. For the visitor-facing overview, see [the README](../README.md). Signing instructions remain in [SIGNING.md](../SIGNING.md).

## Implemented

- Native Nearby, Favourites and My alerts screens, with light and dark appearances.
- Live standby waits and ride coordinates from the public ThemeParks.wiki API.
- Magic Kingdom, EPCOT, Hollywood Studios, Animal Kingdom, both Disneyland Paris parks, Universal Studios Florida, Islands of Adventure and Epic Universe.
- Per-ride queue thresholds, straight-line distance limits, optional ridden-today exclusion and reopening alerts.
- Ridden state resets by the park's local date, rather than the phone's timezone.
- Local persistence of favourites, rules and notification cooldowns.
- User-started location foreground service with a persistent notification and stop action. No all-the-time location permission, automatic boot start, accounts or app backend.
- Fetching approximately every two minutes; bounded network timeouts and additional retry backoff.
- Missing waits are never interpreted as zero. Cached/offline feeds cannot trigger alerts.
- Alerts require an operating ride, a feed timestamp within ten minutes, a location fix within two minutes and reported accuracy of 100 metres or better.
- Threshold notifications on entering a matching state, with a persisted cooldown; no repeated alerts while a ride continuously qualifies.
- Optional reopening notification after an observed DOWN → OPERATING transition. This respects distance, ridden filtering and cooldown, and can trigger above the queue threshold. A normal CLOSED → OPERATING opening does not count as a reopening.
- Sessions stop after twelve hours; OS/service termination requires manually restarting Park mode.

## Validation and limitations

- `./test.sh` passed 26 pure Java alert-decision checks, including stale data, invalid/missing waits, location accuracy, boundary conditions, cooldowns and reopening rules.
- `./build-apk.sh` built against Android API 36 and Build Tools 36.0.0; `apksigner` verified the APK's v2 and v3 signatures.
- Local Gradle lint, debug build and AAB build have not been verified: this environment could not resolve Android Gradle Plugin 8.10.1 from its configured repositories.
- Pull-request CI run 37786100939 passed Gradle lint/debug build, release AAB creation with a disposable job-generated test key, bundletool validation, JAR signature verification and smoke-test artifact publication. The disposable key is not a Play upload key.
- Inspected the public feed's live JSON contract and park identifiers while implementing the adapter.
- **Not yet tested on a physical Android device.** Installation, screen layouts, permission flows, notification delivery and screen-off operation need device testing on Android 13, 15 and 16.
- On trusted tagged releases with Play upload-key secrets configured, CI is configured to validate the signed AAB, generate and verify an upload-key-signed universal APK, and install/launch it on an Android API 36 emulator. That emulated APK does not verify the Google Play app-signing certificate or Play-delivered update path; this remains pending.
- Android power saving can delay polling or stop the service. No exact delivery interval is promised.
- Queue times are posted estimates and can change before you arrive. `lastUpdated` is conservatively used for freshness; a provider retaining older timestamps for unchanged data can suppress otherwise useful alerts.
- The UI can retain the already displayed snapshot when refresh fails. Provider files expire after five minutes; an empty or expired cache cannot supply a fallback. The monitor never sends alerts from disk or failed-request fallback snapshots. Normal refresh cadence is per park; HTTP 429 backoff is shared.
- Proximity is straight-line distance, not a park footpath route or walking-time estimate.
- No map/navigation screen, push-notification backend, Play Store publication, automatic ride detection or cross-device sync in this version.

## CI and tagged APKs

Pushes and pull requests run the alert tests and build a development APK. Every pushed tag builds a versioned APK and Gradle release AAB, validates the AAB with bundletool, uploads an Actions artifact and attaches the APK and checksum to a GitHub Release. When the separate Play upload-key secrets are configured, CI signs the AAB, verifies its signature, builds a universal APK with bundletool for device checks, and attaches the Play-ready AAB to the release. Without those secrets, the APK release still succeeds and CI retains an explicitly unsigned AAB artifact that cannot be uploaded to Play.

For example, push a version tag after committing your changes:

```bash
git tag v0.1.1
git push origin v0.1.1
```

Tag releases are testing builds by default. Without a stable GitHub APK signing keystore configured, APK builds can have different signing certificates and Android may require uninstalling the previous version before installing the new one; uninstalling removes locally saved rules and favourites. Configure all four repository Actions secrets below before distributing APKs that need reliable in-place updates. Never commit a private release signing key.

| Optional Actions secret | Value |
| --- | --- |
| `PARKPING_KEYSTORE_BASE64` | Base64-encoded contents of a persistent signing keystore |
| `PARKPING_KEYSTORE_PASSWORD` | Keystore password |
| `PARKPING_KEY_ALIAS` | Signing key alias |
| `PARKPING_KEY_PASSWORD` | Key password |

Configure all four together under Settings → Secrets and variables → Actions. Builds without them publish `park-ping-development.apk`; builds with them publish `park-ping.apk`.

Configure the separate `PARKPING_UPLOAD_*` secrets documented in [SIGNING.md](../SIGNING.md) to sign the AAB for Play Console. The Play upload key is not the app signing key: Google Play App Signing uses a Google-managed app signing key for Play installs. GitHub APKs use a separate certificate, so switching between GitHub APKs and Play installs requires uninstalling; only updates within the same channel are compatible. The same tag workflow supplies both formats with `versionCode = 1000 + GitHub Actions run_number`; keep all Play tracks on that sequence and do not upload a higher code manually. Releases remain prereleases while phone testing is outstanding.

With upload secrets configured, tagged CI validates the signed AAB, builds a universal APK with bundletool, and installs/launches it on an Android API 36 emulator. Download the generated `park-ping-play-universal.apk` Actions artifact for optional physical-device checks. Then upload `park-ping-play.aab` to the Play internal testing track, install from Play, and verify a subsequent tagged release updates in place while preserving app state. The universal APK is upload-key-signed and does not test Play's app-signing certificate/update path. Play Console enrollment, track acceptance, and physical-device checks require maintainer access and are not performed by CI.

## Build and maintain

Java 17 and Android Studio / Android SDK are required. Application ID: `uk.co.collom.parkping`. Minimum Android version: Android 8.0 (API 26); target/compile SDK: 36; Android Gradle Plugin: 8.10.1; Build Tools: 36.0.0.

Standard local build and unsigned release bundle:

```bash
./test.sh
./gradlew assembleDebug lintDebug
./gradlew bundleRelease
```

Set `PARKPING_VERSION_CODE` and `PARKPING_VERSION_NAME` to override local bundle metadata. Local `bundleRelease` output is unsigned unless all four `PARKPING_UPLOAD_*` values are supplied; do not upload an unsigned bundle to Play.

If a wrapper is not available, install Gradle 8.11.1 and run `gradle assembleDebug lintDebug`. Open the project folder in Android Studio to manage SDK setup and run on a connected device.

SDK-only fallback (Python 3 is used only for resource packaging):

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./test.sh
./build-apk.sh
```

Install SDK platform `android-36` and Build Tools `36.0.0` first. The fallback generates a development signing key in `.local-signing/` (git-ignored); keep the key to produce installable updates with the same signature. Production releases require a separate release signing setup. The generated output is `app/build/manual/park-ping-0.1.0.apk`.

Code structure:

| File | Purpose |
| --- | --- |
| `MainActivity.java` | Native screens, alert editor and permission flows |
| `MonitorService.java` | Park session, fresh location checks and notifications |
| `AlertEngine.java` | Pure, independently testable alert decisions and distance calculation |
| `ParkApi.java` | HTTPS adapter, entity/live joins and offline cache |
| `Store.java` | Local rules, favourites, daily ride markers and cooldowns |
| `Models.java` | Ride models and supported parks |

## Privacy and data

Location is only used locally to calculate ride distances; coordinates are not sent to the data provider. Network requests contain park IDs and normal network metadata such as your IP address. No analytics or third-party tracking SDKs are included. Local preferences and cached feeds are excluded from Android backup. The Android permission prompt and Park mode explanation precede active location monitoring.

Ride data: https://themeparks.wiki — please retain attribution. This is an unofficial app and is not affiliated with Disney, Universal or ThemeParks.wiki. Review the provider's current terms before public/commercial distribution.

## Support and report triage

Visitors can reach the public [bug report and feature request forms](https://github.com/davidcollom/park-ping/issues/new/choose) from the app, README and listing draft. GitHub issues are public. Ask for app and Android versions, selected park, a timestamp and the expected behaviour, but never request signing secrets, precise location history or unnecessary personal details.

During testing, review new reports at least weekly and before preparing a test release. Confirm the affected versions and park, reproduce against the current test build where possible, link duplicates, and prioritise crashes, incorrect alerts, data loss and privacy concerns before usability issues and feature requests.

After launch, continue reviewing reports at least weekly. Check confirmed bugs against the current supported release, prioritise safety, privacy, data-loss and core alert failures, and use feature requests to inform the roadmap. Keep the report open if more information is needed; close it with a brief explanation when fixed, declined or no longer reproducible. The project does not promise an individual response time.

The combined suite also includes 10 provider-throttle and eight per-park/shared-backoff checks. Full Android packaging and signed-bundle validation are required on the final PR head; local pure-Java results alone are not release evidence.

A successful online snapshot can be shared between browsing and monitoring for under five minutes, retaining provider timestamps. Sharing does not convert a disk/offline fallback into live data. Eight response-sharing regression checks cover monitor starvation, expiry, rate limits and invalidation.
