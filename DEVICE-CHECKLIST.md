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
- On a small screen (320 × 480 dp) at 200% font scale, scroll from setup and Park mode controls to the rides on every tab; verify navigation, the support link and alert editing remain reachable without clipped text.
- With TalkBack, check park and slider labels, selected tabs and monitoring announcements; verify touch targets and contrast in both themes.

Physical device checks have not yet been performed for version 0.1.0.
