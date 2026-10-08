# Google Play privacy declarations — review worksheet

Prepared from the Android source for the current signed APK, version 0.0.1. This is a review worksheet, not a completed Play Console submission. Recheck the shipped binary, the current Play Console questions, and ThemeParks.wiki's current privacy practices before submitting. The app does not yet have a published hosted privacy policy URL, and the provider/data-safety answers remain unresolved until the review is complete.

## Data safety

| Console topic | Source-based finding for review |
| --- | --- |
| Location | The app accesses approximate or precise device location only after permission is granted and the user starts Park mode. It is processed on-device to calculate ride distances and evaluate alerts; the app does not transmit or persist coordinates. Do not describe this as location collected by Park Ping based on the current code. |
| Other data sent by the app | HTTPS requests to `api.themeparks.wiki` contain the selected park ID and request public ride/queue data. The API necessarily sees the connection's IP address and ordinary request metadata. The app does not operate a backend or send an account ID, advertising ID, alert rules, favourites, notification history, or coordinates. |
| Third-party processing | Confirm ThemeParks.wiki's current privacy notice and whether/how it retains request logs, including IP addresses, before finalizing the Console's collection/sharing answers. Do not infer the provider's retention from Park Ping's code. If the provider's handling or Play's definitions require a data type to be declared, update the Console answers and policy accordingly. |
| SDKs and tracking | No analytics, advertising, account, or crash-reporting SDK is present in the app source/dependencies. No app-level tracking or account data is sent. |
| On-device data and retention | Private preferences contain selected park, ride IDs for favourites and alert rules, ridden-date markers, and alert cooldown timestamps. Cached public feed JSON is stored in app cache. Settings persist until app data is cleared/uninstalled; cache is replaced on successful refresh or can be cleared by Android/the user. `allowBackup` is false. |
| Security / deletion | API traffic uses HTTPS. App data is stored in Android app-private storage, with no developer-operated account or server copy. Users can remove alert rules in-app or clear app data/uninstall. |

## Location and foreground-service review

- Manifest requests fine/coarse location, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_LOCATION`; it does **not** request `ACCESS_BACKGROUND_LOCATION`.
- A visible-activity user action starts the location foreground service after the permission/explanation flow. It continues checking with the screen off and displays an ongoing notification with a Stop action.
- The session ends on Stop, app park change, service termination, or its 12-hour limit. There is no boot receiver or automatic restart.
- Android's location foreground-service permission model is distinct from `ACCESS_BACKGROUND_LOCATION`. However, Park mode continues to access location while the app UI is not visible. Review the current Play background-location and foreground-service policies and the Play Console declarations together; do not assume this use is exempt merely because the Android manifest lacks background permission.
- For review evidence, capture a release-build demonstration of the user's start action and explanation, the ongoing notification/Stop action, stopping the session, and the 12-hour session limit. Recheck all permission-denial, settings-revocation, and notification-channel flows on supported Android versions.

## References and remaining checks

- [Google Play privacy policy and Data safety requirements](https://support.google.com/googleplay/android-developer/answer/9859455)
- [Google Play location permissions policy](https://support.google.com/googleplay/android-developer/answer/9799150)
- [Park Ping privacy policy draft](https://github.com/davidcollom/park-ping/blob/main/docs/privacy-policy.html) — public hosted URL still pending
- Public support channel: [Park Ping issue tracker](https://github.com/davidcollom/park-ping/issues/new)

Before publication, verify the developer name and support URL in the Play Console, confirm the policy URL is publicly accessible without sign-in, verify the provider's current data practices, and ensure the declarations match the exact release binary.
