# First device check

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
- CI installs and launches `park-ping-play-universal.apk` on an Android API 35 emulator. Install the artifact on a physical clean device too; verify launch and basic browsing. This upload-key-signed APK does not test updates to a Play-delivered installation.
- Enrol in Play App Signing, upload `park-ping-play.aab` to internal testing and install from Play. Publish a later tagged AAB and verify that the Play installation updates in place and retains favourites and alert settings.

Physical device checks, Play internal-track acceptance, and Play-to-Play update checks are pending for version 0.1.0.
