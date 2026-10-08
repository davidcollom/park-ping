# Android release signing and key custody

## Key ownership decision

Use Google Play App Signing with Google protecting the app signing key. Keep two separate private keys under project control:

- The **GitHub APK key** signs APKs installed directly from GitHub.
- The **Play upload key** signs the AAB sent to Play. Google verifies this key, then signs APKs delivered to Play with the app signing key.

Do not reuse either key for the other purpose. GitHub APK installs and Play installs therefore have different certificates and cannot update one another in place. Moving between those distribution channels requires uninstalling first, which deletes local favourites and alert settings (`allowBackup` is disabled). Updates within either channel work when that channel keeps its key and uses a higher version code. Test Play-to-Play updates from the internal track; a bundletool-generated APK signed with the upload key is for installation/build verification, not a substitute for a Play-delivered update.

The Play app signing key is managed in Play Console and is not a GitHub Actions secret. Enrol in Play App Signing when creating the app in Play Console and choose Google-managed key custody. The Play upload key and GitHub APK key remain under project custody.

## GitHub APK key

Use a private signing keystore for GitHub APK releases so updates from one GitHub release to the next keep the same Android identity. Creating a key does not require a paid certificate or a Google Play account.

On your computer, with JDK 17 and the GitHub CLI installed, run:

```bash
keytool -genkeypair -storetype PKCS12 \
  -keystore park-ping-release.p12 -alias park-ping \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -dname "CN=Park Ping"
```

Choose a strong password at the prompt. PKCS12 uses that password for both the keystore and key. Back up the keystore and its password securely; never commit the private key or paste it into a chat. The filename alone is not the secret value: the workflow needs the complete file encoded as base64.

If needed, authenticate the GitHub CLI with `gh auth login`, then add the four secrets:

```bash
python3 -c "import base64; from pathlib import Path; print(base64.b64encode(Path('park-ping-release.p12').read_bytes()).decode())" \
  | gh secret set PARKPING_KEYSTORE_BASE64 --repo davidcollom/park-ping

gh secret set PARKPING_KEY_ALIAS --body park-ping --repo davidcollom/park-ping
gh secret set PARKPING_KEYSTORE_PASSWORD --repo davidcollom/park-ping
gh secret set PARKPING_KEY_PASSWORD --repo davidcollom/park-ping
```

Enter your chosen password at each of the final two prompts. Passwords are entered interactively rather than placed in shell history. GitHub CLI encrypts repository secrets locally before submitting them.

Alternatively, base64-encode the keystore and add these values in the repository under Settings → Secrets and variables → Actions → New repository secret.

The next tag-triggered build uses these secrets and publishes `park-ping.apk`. Keep using the same key for future builds. The original development APK has a different signature, so uninstall that version once before installing the first APK signed with your new key. Uninstalling removes local favourites and alert rules; note them first if needed. Subsequent GitHub APKs using your key can update in place, provided their version code is not lower.

## Play upload key

Create a different PKCS12 keystore for uploading AABs. Keep the file and its password private; do not check it into Git or share it with the GitHub APK signing key. Back up both the GitHub APK and Play upload keystores to at least two encrypted, access-controlled locations, with recovery instructions available to another trusted maintainer. Store passwords separately from the backup files. Never put key material or passwords in workflow logs, issue comments, or chat.

```bash
keytool -genkeypair -storetype PKCS12 \
  -keystore park-ping-play-upload.p12 -alias park-ping-upload \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -dname "CN=Park Ping Play Upload"
```

Add this separate key to repository Actions secrets using the same interactive/password-safe approach above:

| Secret | Value |
| --- | --- |
| `PARKPING_UPLOAD_KEYSTORE_BASE64` | Base64-encoded `park-ping-play-upload.p12` |
| `PARKPING_UPLOAD_KEYSTORE_PASSWORD` | Keystore password |
| `PARKPING_UPLOAD_KEY_ALIAS` | `park-ping-upload` |
| `PARKPING_UPLOAD_KEY_PASSWORD` | Key password |

For example, set the base64 value without printing it to the terminal:

```bash
python3 -c "import base64; from pathlib import Path; print(base64.b64encode(Path('park-ping-play-upload.p12').read_bytes()).decode())" \
  | gh secret set PARKPING_UPLOAD_KEYSTORE_BASE64 --repo davidcollom/park-ping
gh secret set PARKPING_UPLOAD_KEY_ALIAS --body park-ping-upload --repo davidcollom/park-ping
gh secret set PARKPING_UPLOAD_KEYSTORE_PASSWORD --repo davidcollom/park-ping
gh secret set PARKPING_UPLOAD_KEY_PASSWORD --repo davidcollom/park-ping
```

Enter passwords interactively. The tagged workflow decodes the key to a permission-restricted temporary file, signs the release AAB, verifies it with `jarsigner` and bundletool, builds a universal APK for device testing, and removes the temporary key. Without all four upload secrets, it still publishes the existing APK release and retains a clearly named **unsigned** AAB as an Actions artifact; that AAB cannot be uploaded to Play.

In Play Console, enrol the app in Play App Signing and register the public certificate for this upload key when prompted. Keep the upload keystore backed up: if it is lost or exposed, use Play Console's upload-key reset process. Google manages and protects the distinct app signing key used for Play installs. Losing the GitHub APK key prevents in-place updates of GitHub-installed APKs; Google cannot recover it, so preserve its independent encrypted backups.

All tagged GitHub/APK and Play/AAB builds use `1000 + GitHub Actions run_number` as their `versionCode`. Publish through this same tagged workflow for every track and do not upload a higher code through Play Console manually; Android requires each update's code to be greater than the installed release.

Before marking this complete, upload the signed AAB to an internal testing track, install it from Play on a test device, then publish a later tagged AAB and verify that Play updates the installed app while retaining local state. Also install the generated universal APK on a clean test device; it is signed with the upload key and is not Play App Signing's delivered APK. Physical-device installation and Play Console acceptance must be performed by a maintainer with access to those systems.

Sources: https://developer.android.com/studio/publish/app-signing and https://cli.github.com/manual/gh_secret_set
