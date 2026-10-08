# Live data and independent-brand review

Reviewed 8 October 2026. This records the official provider terms and app behavior checked; it is not legal advice.

## ThemeParks.wiki data

Park Ping requests live park data from `https://api.themeparks.wiki/v1`. The official [ThemeParks.wiki API terms](https://www.themeparks.wiki/terms), reviewed on 8 October 2026, permit free and paid consumer apps to display the data without prior permission, subject to the terms. They prohibit mirroring, re-APIing and bulk redistribution, allow short operational caching, and require free-tier use to show the visible “Powered by ThemeParks.wiki” credit; paid plans may remove this requirement. The [API overview](https://www.themeparks.wiki/api) presents the credit as a link to ThemeParks.wiki.

The [API overview](https://www.themeparks.wiki/api) says live data is cached for 60 seconds and requests more frequent than once per minute per park return the same response. The [WebSockets guidance](https://www.themeparks.wiki/api/websockets) says HTTP polling every five minutes remains acceptable; this is an acceptable pattern, not a required minimum. The [API reference](https://api.themeparks.wiki/docs/v1/) documents plan-dependent per-minute limits, and the provider's [JavaScript SDK rate-limit guidance](https://github.com/ThemeParks/ThemeParks_JavaScript#rate-limits) describes handling HTTP 429 responses with `Retry-After`. These API materials do not replace the terms. The data-use terms do not establish rights to third-party park names, logos, or character art; those are assessed separately below.

### Park Ping's current request and outage behavior

- The screen and Park mode can independently request refreshes, and the screen has a manual Refresh button; a shared lock and persisted per-park gate allow no more than one refresh of the same park every five minutes across both callers. Switching parks can fetch the newly selected park immediately, unless a provider-wide HTTP 429 backoff is active.
- Park and live-response cache files are used only for five minutes and expired files are discarded on the next load. A throttled cached snapshot is marked cached, and Park mode never sends alerts from it. Once the five-minute cache expires, a failed refresh returns an error rather than an offline snapshot; expired data is not retained for outage display. Alert freshness and location checks remain local.
- Requests identify themselves as `ParkPing/0.1 (personal Android prototype)` and do not send an API key. A 429 response is handled using its `Retry-After` value (including HTTP-date form), with provider-wide backoff persisted across process restarts and shared by every park; malformed or missing values fall back to the five-minute minimum.
- Location coordinates are not sent to the provider. Requests include park IDs and expose ordinary network metadata such as the user's IP address. The provider may observe or log request metadata; its logging and retention practices were not verified by this review.

### Distribution decision and follow-up

**Documented distribution basis:** the reviewed terms permit free or paid consumer-app display without prior permission, provided their conditions are followed. Park Ping displays the data within the app, does not expose a feed or bulk export, uses a five-minute per-park refresh/cache limit shared by the UI and Park mode, honors 429 backoff, and shows linked attribution. Recheck the official terms and actual shipping behavior before release and whenever the terms or use changes. If the app's use expands to rehosting, re-APIing or bulk redistribution, this review does not authorize that use.

## Name, artwork, and disclosure

- The app and Android launcher are named **Park Ping** and use the original location-pin mascot with adaptive and monochrome launcher artwork. Notifications use a separate generic bell glyph. The artwork does not use Disney/Universal characters or logos.
- Disney, Universal, and park names appear only to identify supported parks and rides. They are descriptive references, not the app name, logo, or claim of sponsorship.
- The app shows an in-app independent/unofficial disclosure, and the README and store-listing draft state that Park Ping is not affiliated with Disney, Universal, or ThemeParks.wiki. Keep that disclosure visible in the app and in any public listing; do not use third-party logos or character art.

This is a practical branding review, not trademark clearance. Recheck the final store name, icon, screenshots, and listing before submission.
