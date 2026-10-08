# Android compatibility checks

Run permission and notification checks on Android 13 (API 33), Android 15 (API 35) and Android 16 (API 36). Also verify install and basic browsing on the minimum supported Android 8.0 (API 26).

- Install and launch the APK; confirm the chosen park loads rides and queue ages.
- Save a favourite and alert, close/reopen the app and confirm they persist.
- Send a test notification from the alert editor. Verify Android shows the labelled sample alert.
- Deny location and verify ride browsing still works. Grant precise location and notification access, then start Park mode.
- Verify the ongoing notification appears and its Stop action stops monitoring.
- Switch parks while monitoring and verify Park mode stops; explicitly restart for the new park.
- Mark a ride ridden and verify the exclusion respects the park's local date.
- On a real park visit, verify distances against known locations and check threshold crossings and reopening notifications.
- Leave the screen off for at least ten minutes and observe notification/foreground-service behaviour under your phone's battery settings.
- Disable network access and verify stale/cached labels and suppression of real alerts.
- Test increased Android font size and both light/dark appearances for clipping.
- On Android 15 and 16, check that status/navigation bars and display cutouts do not cover controls, including with gesture and three-button navigation.
- On Android 13, 15 and 16, test granting and denying precise/approximate location and notification permission, then confirm Park mode only starts when the required access is available.
- On Android 13, 15 and 16, confirm the ongoing location notification is visible, ride alerts are delivered when allowed, and stopping from the notification ends Park mode.
- With the Play upload key configured, CI installs and launches `park-ping-play-universal.apk` on an Android API 36 emulator. Install the artifact on a physical clean device too; verify launch and basic browsing. This upload-key-signed APK does not test updates to a Play-delivered installation.
- Enrol in Play App Signing, upload `park-ping-play.aab` to internal testing and install from Play. Publish a later tagged AAB and verify that the Play installation updates in place and retains favourites and alert settings.

- Pull requests build, bundletool-validate and artifact a signed smoke AAB using a disposable key generated in the job; it is not a Play upload.
- Physical device checks, Play internal-track acceptance, and Play-to-Play update checks are pending. Do not close issue #4 until these checks are complete.

- At 320 × 480 dp and 200% font scale, verify controls and support links remain reachable by scrolling.
- With TalkBack, check labels, selected tabs and monitoring announcements; verify both themes.
