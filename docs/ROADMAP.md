# Google Play launch

The [launch tracker #11](https://github.com/davidcollom/park-ping/issues/11) and its linked GitHub issues are the source of truth. This document is an index, not a separate backlog.

| Work | Issue |
| --- | --- |
| Mascot and Android/store icon assets | #1 |
| Native screenshots and store copy | #2 |
| API 36 and Android compatibility | #3 |
| Signed AAB and Play App Signing | #4 |
| Privacy, Data safety and location review | #5 |
| Device, GPS and battery testing | #6 |
| Onboarding and accessibility | #7 |
| Live data terms and independent branding | #8 |
| Play Console tests and production access | #9 |
| Feedback and support | #10 |

The milestone is **Google Play launch**. No due date is set. The repository workflow `launch-milestone.yml` idempotently creates that milestone and attaches issues #1–#11. It runs when its configuration is first pushed to main, and can be run manually if needed.

## Release order

First review the brand and prepare the candidate (API upgrade, AAB, privacy and UX). Then validate it on real devices and capture accurate screenshots. Upload to Play testing, satisfy any account-specific closed testing requirement and resolve feedback before applying for production access.

Current release: signed APK v0.0.1. It is a test release, not a Play Store build.

## Policy references checked on 8 October 2026

- [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878): new submissions require API 36 or higher.
- [Testing for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465): accounts created after 13 November 2023 require at least 12 continuously opted-in closed testers for 14 days before applying for production access.
- [Prepare for review](https://support.google.com/googleplay/android-developer/answer/9859455): privacy and app-content declarations.
- [Location policy](https://support.google.com/googleplay/android-developer/answer/9799150): foreground-service and background-location review.

Check the current Console requirements again before submission.
