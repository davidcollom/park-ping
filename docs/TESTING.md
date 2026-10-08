# Physical-device testing

## Status

**Physical-device testing is outstanding.** `adb` is not installed in the environment where this record was prepared, so the APK could not be installed on or tested with a physical device. The procedures below are not results: do not treat any device-dependent behavior as verified until the result fields are completed on a real device.

Contributes to #6. Keep issue #6 open until reproducible physical-device results and remaining limitations are recorded; this documentation alone does not complete its acceptance work.

The implementation and automated checks provide expected behavior, not proof of Android OS or device behavior. In particular, the app uses a user-started location foreground service, polls about every two minutes, stops after 12 hours, and does not restart automatically after OS/service termination. Alerts are suppressed for cached feeds, queue data older than 10 minutes, location fixes older than two minutes, or reported accuracy worse than 100 metres. Distances are straight-line. Ridden-today state uses the selected park's timezone.

## Record each test run

Complete this information for every device/build tested:

| Field | Result |
| --- | --- |
| Date and tester | Not run |
| APK version name / version code | Not run |
| APK SHA-256 and signing-certificate SHA-256 | Not run |
| Device model | Not run |
| Android version / security update | Not run |
| Selected park and local timezone | Not run |
| Network and location conditions | Not run |

For APK identity, use `sha256sum park-ping.apk` (Linux) or `shasum -a 256 park-ping.apk` (macOS), and `apksigner verify --print-certs park-ping.apk`. Keep signing keys and passwords private; record only the certificate fingerprint.

## Prerequisite for injected scenarios

This repository does not include an on-device fixture harness or a configurable provider endpoint. Network disable/reconnect can be tested against the normal APK, but deterministic provider failures, missing/stale data, and reopening transitions require a separately prepared, reproducible test build/harness. Until that prerequisite is available, leave those injected cases **Not run**; do not substitute the home sample notification button. That button sends sample data directly and does not exercise `ParkApi.load`, `MonitorService.poll`, or `AlertEngine.evaluate`.

The harness must supply controlled responses at the provider boundary while retaining the real parsing, cache fallback, polling, alert evaluation, and Android notification path. Do not call the notification API directly or weaken the release APK's HTTPS checks. Record the harness source revision, setup/run/reset commands, test APK identity, fixture files and SHA-256 hashes, response order and timing, and initial cache/rule/cooldown state so another tester can repeat the case. Clearly label results from this test build separately from results on the unmodified release APK.

Use matching ride IDs in `/entity/{parkId}/children` and `/entity/{parkId}/live` fixtures. Include ride coordinates in `children`, and `entityType: ATTRACTION`, `status`, `queue.STANDBY.waitTime`, and `lastUpdated` in `liveData`. Keep location fresh, accurate, and within the configured radius, and keep ridden/cooldown state eligible except when testing those gates. For each case, reset state and record the exact response sequence:

- **Offline/reconnect and provider failure:** Seed a successful response/cache, fail subsequent requests (record whether transport failure or a specified non-200 HTTP response), then restore success. Repeat from an empty provider cache without clearing saved rules; verify fallback, alert suppression, and recovery.
- **Missing/stale data:** Disable reopening alerts to isolate threshold eligibility. Return separate otherwise-eligible fixtures with `waitTime` null or absent, `lastUpdated` absent, and `lastUpdated` more than 10 minutes before the device clock. Record the device clock and generated timestamps; then return fresh data to check recovery.
- **Reopening:** With reopening enabled, wait for a poll to consume fresh `DOWN` data, then return fresh `OPERATING` data for the same ride. Reset state and repeat with `CLOSED` → `OPERATING` as the negative control. Keep the wait above the configured threshold so a threshold crossing cannot be mistaken for a reopening alert.

## Test procedures and results

For every row, record **Pass**, **Fail**, or **Not run**, along with the device/build, steps, observed result, and any issue link. Repeat permission and power tests on each supported Android version and at least one device with a different manufacturer's battery-management behavior.

| Area | Reproducible procedure | Result |
| --- | --- | --- |
| Same-key upgrade and persistence | Install the previous APK signed with the persistent release key. Save a favourite, alert rule, and cooldown state. Install the newer APK over it (do not uninstall), launch it, and verify the park selection, favourite, alert settings, and state remain. Confirm the package version changed and the signing certificate fingerprint is identical. | Not run |
| Notification permission | On Android 13+, install fresh and test both denying and allowing the notification prompt. Also disable and re-enable notifications in system Settings after granting. On earlier Android versions, use Settings to disable/enable app notifications. Confirm the app explains why Park mode cannot start when required notifications are unavailable, and verify test and ride alerts when allowed. | Not run |
| Location permission and precision | Test location denied, approximate-only, and precise access. Verify browsing still works without location; Park mode either explains the missing permission or starts only when access is sufficient. With approximate location, confirm the status/alerts reflect the reported accuracy and that no alert is sent above the 100 m accuracy limit. Revoke access while monitoring and record the service and UI behavior. | Not run |
| Location freshness and distance | In a safe, open outdoor location, start Park mode and record the displayed fix age/accuracy and a known ride location. Compare the reported distance with an independent straight-line calculation from the same coordinates; do not compare it with walking distance. Repeat indoors or after disabling location/provider access and verify an absent or older-than-two-minute fix does not produce alerts. Record any uncertainty from GPS drift. | Not run |
| Screen-off and power saving | Start monitoring with a fresh fix and live data; lock the screen for at least 30 minutes. Repeat with the manufacturer's battery saver enabled and with Android Battery Saver/Doze conditions. Record check/alert timestamps, foreground-notification persistence, delays, and whether the OS stops the service. Do not infer a guaranteed two-minute delivery interval from nominal polling. | Not run |
| Process/service termination and stop action | While monitoring, use the notification's **Stop Park mode** action and verify monitoring stops and the ongoing notification is removed. Separately, reproduce an OS/process termination without force-stopping the package; verify whether monitoring ends and that it does not restart automatically. Force-stop only as a separately labelled case because Android may block background starts until the app is opened again. | Not run |
| Twelve-hour limit | Leave Park mode active for a full 12-hour session with the screen on/off as practical. Verify the session ends, the status reports the 12-hour limit, and the ongoing notification is removed. Record interruptions or device reboots. | Not run |
| Battery consumption | Run a representative park session of at least two hours with the screen off for most of it. Record start/end time, battery percentage and (if available) Android's per-app energy/charge usage, plus location mode, signal, and battery saver settings. Compare with a similar-duration idle baseline on the same device; report the method and avoid treating percentage change as precise app-only consumption. | Not run |
| Offline, reconnect, missing/stale data, provider failure | Load a park online, then disable network access. Verify cached data is labelled and cannot trigger alerts; restore connectivity and verify live updates resume. For controlled failures and missing/stale data, use the fixture prerequisite and response sequences above, including the no-cache case. Record status, retry/recovery time, and any crash; leave injected cases Not run if the harness is unavailable. | Not run |
| Cooldown and duplicate alerts | Configure a ride with a short cooldown and observe a real qualifying queue transition. Verify only one threshold alert is sent while it remains eligible, a continuously qualifying ride does not repeatedly alert, and another alert is possible only after the configured cooldown and a new qualifying transition. Record notification timestamps. | Not run |
| Ridden-today timezone rollover | Mark a ride ridden with skip-ridden enabled. Verify it is excluded for the selected park's local date, remains excluded across phone timezone changes that do not change the park date, and becomes eligible after the park-local date changes. Record the park, its timezone, local timestamps, and result. | Not run |
| Reopening rules | Enable reopening alerts. Observe a real `DOWN` → `OPERATING` transition or use the fixture prerequisite and response sequence above; verify one eligible alert, subject to distance, freshness, ridden state, and cooldown. Verify a normal `CLOSED` → `OPERATING` opening does not count as a reopening. Leave injected cases Not run if the harness is unavailable. | Not run |

## Results available in this repository

On 2026-10-08, `./test.sh` passed all 26 automated alert-decision checks. They exercise pure Java alert rules; they do not validate APK installation, Android permissions, notifications, GPS, foreground services, Doze, battery consumption, or provider behavior on a device. `adb` was not installed in that test environment, so no physical-device observations were made.

No device crashes, missed alerts, duplicate alerts, or battery measurements can be reported until the procedures above are run. Device-specific behavior and any remaining limitations are therefore **unverified**, not passed. Update this section with dated findings and link blocking defects to their issue or fix.
