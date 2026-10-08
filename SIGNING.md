# Persistent APK signing

Use a private signing keystore for GitHub APK releases so updates keep the same Android identity. Creating a key does not require a paid certificate or a Google Play account.

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

The next tag-triggered build uses these secrets and publishes `park-ping.apk`. Keep using the same key for future builds. The original development APK has a different signature, so uninstall that version once before installing the first APK signed with your new key. Uninstalling removes local favourites and alert rules; note them first if needed. Subsequent builds using your key can update in place, provided their version code is not lower.

Sources: https://developer.android.com/studio/publish/app-signing and https://cli.github.com/manual/gh_secret_set
