# Google Play listing draft

Draft copy for review before publication. This app is currently a test build; the listing and native screenshots must be checked against the release candidate.

## App name

Park Ping

## Short description

Nearby ride alerts for your park day. Choose your queue and distance limits.

## Full description

A shorter queue. Just around the corner.

Park Ping helps you keep an eye on your favourite rides while you enjoy your park day. Choose a ride, set your maximum posted queue and pick a nearby distance radius. Switch on Park mode and get a notification when the conditions match.

Save favourites, mark rides you’ve already done today and choose how long to wait between pings. You can also enable alerts when a nearby ride reopens after temporary downtime.

The first version supports selected Disney and Universal parks in Orlando and Disneyland Paris. Live ride data comes from ThemeParks.wiki, and availability depends on its coverage.

Your location is processed on your phone to calculate ride distances. Coordinates are not sent to the park-data provider. Park mode is started by you and can be paused or stopped from the app or its ongoing notification. Network requests still expose normal network information such as your IP address.

Queue times are posted estimates and can change. Distance is a straight-line radius, not a walking route. Notifications need fresh data, location access and notification permission; Android battery settings can affect delivery.

Park Ping is an independent app and is not affiliated with Disney, Universal or ThemeParks.wiki.

## Before this goes live

- Replace concept images with verified screenshots of the native release candidate: issue #2.
- Finish adaptive icons and Play graphics: issue #1.
- Add the actual support contact and published privacy-policy URL: issues #5 and #10.
- Verify descriptions against shipping behaviour, park names and current Console limits.
- Resolve the live-data distribution gate in [the data and branding review](DATA-AND-BRAND-REVIEW.md) before public release.
- Complete compatibility, signed AAB and Play testing work: issues #3, #4, #6 and #9.

See [the launch tracker](https://github.com/davidcollom/park-ping/issues/11). No Play listing has been published.
