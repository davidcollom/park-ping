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

- Compiled all native app classes against Android API 36.
- Passed 26 pure Java alert-decision checks, including stale data, invalid/missing waits, location accuracy, boundary conditions, cooldowns and reopening rules.
- Packaged the APK with Android SDK Build Tools 36.0.0; aligned it and verified its APK v2/v3 signatures.
- Inspected the public feed's live JSON contract and park identifiers while implementing the adapter.
- **Not yet tested on a physical Android device or emulator.** Installation, screen layouts, permission flows, notification delivery and screen-off operation need device testing on Android 13, 15 and 16.
- Current CI runs the Java rule checks, Gradle lint/build and SDK-only APK build. Device permission, notification, screen-off and layout checks remain manual.
- Android power saving can delay polling or stop the service. No exact delivery interval is promised.
- Queue times are posted estimates and can change before you arrive. `lastUpdated` is conservatively used for freshness; a provider retaining older timestamps for unchanged data can suppress otherwise useful alerts.
- The UI keeps the previous successful snapshot when a refresh fails. Its age labels remain visible; the monitor never sends notifications from a cached snapshot.
- Proximity is straight-line distance, not a park footpath route or walking-time estimate.
- No map/navigation screen, push-notification backend, Play Store publication, automatic ride detection or cross-device sync in this version.

## CI and tagged APKs

Pushes and pull requests run the alert tests and build a development APK. Every pushed tag builds a versioned APK, verifies its signature, uploads an Actions artifact and attaches the APK and checksum to a GitHub Release.

For example, push a version tag after committing your changes:

```bash
git tag v0.1.1
git push origin v0.1.1
```

Tag releases are testing builds by default. Without a stable signing keystore configured, builds can have different signing certificates and Android may require uninstalling the previous version before installing the new one; uninstalling removes locally saved rules and favourites. Configure all four repository Actions secrets below before distributing builds that need reliable in-place updates. Never commit a private release signing key.

| Optional Actions secret | Value |
| --- | --- |
| `PARKPING_KEYSTORE_BASE64` | Base64-encoded contents of a persistent signing keystore |
| `PARKPING_KEYSTORE_PASSWORD` | Keystore password |
| `PARKPING_KEY_ALIAS` | Signing key alias |
| `PARKPING_KEY_PASSWORD` | Key password |

Configure all four together under Settings → Secrets and variables → Actions. Builds without them publish `park-ping-development.apk`; builds with them publish `park-ping.apk`. Tag names become the APK version name; CI assigns an increasing Android version code. Releases are marked as prereleases while phone testing is outstanding.

For key creation and exact upload commands, see [SIGNING.md](../SIGNING.md).

## Build and maintain

Java 17 and Android Studio / Android SDK are required. Application ID: `uk.co.collom.parkping`. Minimum Android version: Android 8.0 (API 26); target/compile SDK: 36.

Standard build:

```bash
./test.sh
./gradlew assembleDebug lintDebug
```

If a wrapper is not available, install Gradle 8.11.1 and run `gradle lintDebug assembleDebug`. Open the project folder in Android Studio to manage SDK setup and run on a connected device.

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
