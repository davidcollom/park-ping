# Release changelog

Pushing a tag runs the Android release workflow and publishes its APK and checksum on [GitHub Releases](https://github.com/davidcollom/park-ping/releases).

The workflow adds GitHub-generated release notes covering merged pull requests, contributors and the comparison link. Categories are configured in `.github/release.yml`: label feature PRs `enhancement` and fixes `bug`; unlabelled changes appear in Other changes. Only merged changes belong to a release; open launch issues are not completion evidence.

Installation and signing guidance remain above the changelog. Releases stay marked as Android test prereleases until launch readiness has been verified.

A rerun refreshes the generated release body and replaces attached build assets for the same tag. Manually edited notes are replaced, so retain durable release guidance in the workflow or this repository. Use a new version tag for a new update; do not move a published tag.

The generated changelog is release metadata, not a claim that phone testing or Play Console steps have passed. See [the launch tracker](https://github.com/davidcollom/park-ping/issues/11).
