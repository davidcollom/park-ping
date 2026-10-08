# Android compatibility checks

Run the permission and notification checks on Android 13 (API 33), Android 15 (API 35) and Android 16 (API 36). Also verify install and basic browsing on the minimum supported Android 8.0 (API 26).

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

Physical device checks have not yet been performed for this API 36 target build.
