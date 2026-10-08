# Live data and independent-brand review

Reviewed 8 October 2026, before public or commercial distribution. This records the provider and app documentation checked; it is not legal advice.

## ThemeParks.wiki data

Park Ping requests live park data from `https://api.themeparks.wiki/v1`. The provider's [JavaScript SDK documentation](https://github.com/ThemeParks/ThemeParks_JavaScript#client-options) describes live wait-time access, optional API keys, and an identifying `User-Agent`. Its [rate-limit guidance](https://github.com/ThemeParks/ThemeParks_JavaScript#rate-limits) says requests are metered per minute and describes server-reported limits; the [Python SDK documentation](https://github.com/ThemeParks/ThemeParks_Python#client-options) says API keys provide higher limits and access beyond the free tier. These are API-client documents, not a data licence.

The provider material reviewed does not establish whether third-party apps may republish the feed, whether commercial use is permitted, what attribution is required, or a fixed polling interval for this use. No explicit permission or applicable data-use licence could be verified from those materials. Do not infer permission from the API being publicly reachable or from SDK source-code licences.

### Park Ping's current request and outage behavior

- The app refreshes live data about every two minutes while its screen is open or Park mode is active. An open screen and Park mode can each poll; the Refresh button is also user-triggered. Park metadata is cached for up to 24 hours.
- Requests identify themselves as `ParkPing/0.1 (personal Android prototype)`. The app does not send an API key or currently inspect provider rate-limit headers.
- On a non-success response or network failure, the app retains the last successful data for display where available. Park mode pauses alerts for cached data and adds up to ten minutes of backoff to its polling delay after repeated failures; without cached data, loading fails. Displayed wait ages remain visible and stale data is not eligible for alerts.
- The provider documentation reviewed describes server-side rate limits but no fixed public polling ceiling. The current two-minute interval is Park Ping's behavior, not a provider-approved limit. Re-check current provider guidance and honor any applicable quota before release.

### Distribution decision and follow-up

**Public/commercial distribution is not cleared by this review.** Before publishing, ask ThemeParks.wiki for written confirmation of permission and conditions for this app's live-data display and redistribution, including commercial use, attribution, caching, and request frequency. Confirm the applicable API-key tier/quota and update the app's request identification if needed. If suitable permission or terms cannot be confirmed, disable the live feed or move to a source whose licence explicitly allows the intended use. Keep this as a release gate; this document does not grant permission.

## Name, artwork, and disclosure

- The app and Android launcher are named **Park Ping** and use an original location-pin mascot / generic pin icon. The artwork does not use Disney/Universal characters or logos.
- Disney, Universal, and park names appear only to identify supported parks and rides. They are descriptive references, not the app name, logo, or claim of sponsorship.
- The app shows an in-app independent/unofficial disclosure, and the README and store-listing draft state that Park Ping is not affiliated with Disney, Universal, or ThemeParks.wiki. Keep that disclosure visible in the app and in any public listing; do not use third-party logos or character art.

This is a practical branding review, not trademark clearance. Recheck the final store name, icon, screenshots, and listing before submission.
