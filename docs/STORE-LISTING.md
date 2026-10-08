# Google Play listing copy and assets

The copy below is prepared for release-candidate review; it is not published or yet verified against a Play Console listing. No native app screenshots have been captured. The README images remain clearly labelled design mock-ups and must not be submitted as app screenshots.

## App name

Park Ping

## Short description

Nearby ride alerts for your park day. Set queue and distance limits.

## Full description

Save an alert for a ride with a maximum posted wait and straight-line distance, then start Park mode. Park Ping can notify you when fresh ride data meets that ride’s alert conditions, or when a nearby ride reopens after temporary downtime.

Favourite rides separately, mark rides you’ve already done today, and set a cooldown as the minimum delay between alerts. Cooldown does not guarantee repeat notifications: a ride that stays eligible will not trigger repeated threshold alerts. Pause or stop Park mode whenever you like.

The app currently supports Magic Kingdom, EPCOT, Hollywood Studios and Animal Kingdom; Disneyland Paris and Disney Adventure World; and Universal Studios Florida, Islands of Adventure and Epic Universe. Live ride data is supplied by ThemeParks.wiki; availability depends on its coverage.

Your location is used on your device to calculate ride distances; your coordinates are not sent to the data provider. Starting Park mode requires location permission and notifications. Network requests expose normal network information, such as your IP address.

Wait times are posted estimates, not guarantees, and may change before you arrive. Distances are straight-line, not walking routes. Alert delivery depends on fresh provider data, permissions, location accuracy, network availability and Android power settings.

Park Ping is an independent app. It is not affiliated with Disney, Universal or ThemeParks.wiki.

## Listing limits and assets

General Play listing requirements checked 8 October 2026 against [Google Play Console Help](https://support.google.com/googleplay/android-developer/answer/9866151):

- App name: up to 30 characters; short description: up to 80; full description: up to 4,000.
- Phone screenshots: at least 2; JPEG or 24-bit PNG; each side 320–3,840 pixels, with the longer side no more than twice the shorter side. Use portrait captures around 1080 × 1920 pixels where practical.
- App icon: 512 × 512 pixels. Feature graphic: 1024 × 500 pixels.

These are general published limits, not a check of this app’s Play Console listing. Confirm the current requirements and required fields in the app’s Console before upload.

## Before publication

- Capture the signed release candidate on an emulator and a physical Android phone; neither device capture nor physical-device checks have been performed in this environment.
- Capture representative Nearby, favourites and alert-controls screens, plus a test notification that is visibly identified as an example. Do not imply that the example is a live ride alert.
- Check light and dark appearances, increased font size and the smallest supported screen. Replace README mock-ups only when real captures accurately represent the release candidate.
- Review all screenshots against the installed release candidate and the current Console asset requirements before upload.
- Review and approve the candidate mascot identity with Dave before publication (issue #1). The Play icon and feature graphic are exported in `docs/store-assets/`; editable SVG layouts are in `docs/brand/`.
- Confirm the 512 × 512 app icon and 1024 × 500 feature graphic against current Play Console requirements when uploading.
- Add the actual support contact and published privacy-policy URL (issues #5 and #10), and complete compatibility, signed AAB and Play testing work (issues #3, #4, #6 and #9).
- Recheck the copy against release behavior, supported parks and the current Console limits.

See [the launch tracker](https://github.com/davidcollom/park-ping/issues/11). No Play listing has been published.
