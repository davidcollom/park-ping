#!/usr/bin/env bash
# Dependency-free SDK build, useful where Gradle repositories are unavailable.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK directory}"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK 17 directory}"
build_tools="$ANDROID_HOME/build-tools/36.0.0"
android_jar="$ANDROID_HOME/platforms/android-36/android.jar"
output="$PWD/app/build/manual"
version_name="${PARKPING_VERSION_NAME:-0.1.0}"
version_code="${PARKPING_VERSION_CODE:-1}"
apk_name="${PARKPING_APK_NAME:-park-ping-0.1.0.apk}"
[[ "$version_code" =~ ^[1-9][0-9]*$ ]] && (( version_code <= 2100000000 )) || {
    echo 'PARKPING_VERSION_CODE must be an Android-compatible positive integer.' >&2; exit 1;
}
[[ "$apk_name" =~ ^[a-zA-Z0-9._-]+\.apk$ ]] || {
    echo 'PARKPING_APK_NAME must be a plain APK filename.' >&2; exit 1;
}
rm -rf "$output"
mkdir -p "$output/gen" "$output/classes" "$output/dex"
python3 - "$output/AndroidManifest.xml" <<'PY'
import sys
from pathlib import Path
manifest = Path('app/src/main/AndroidManifest.xml').read_text()
manifest = manifest.replace('<manifest ', '<manifest package="uk.co.collom.parkping" ', 1)
Path(sys.argv[1]).write_text(manifest)
PY
"$build_tools/aapt2" compile --dir app/src/main/res -o "$output/resources.zip"
"$build_tools/aapt2" link -o "$output/resources.apk" --manifest "$output/AndroidManifest.xml" \
    -I "$android_jar" --java "$output/gen" --min-sdk-version 26 --target-sdk-version 36 \
    --version-code "$version_code" --version-name "$version_name" "$output/resources.zip"
"$JAVA_HOME/bin/javac" --release 17 -classpath "$android_jar" -d "$output/classes" \
    app/src/main/java/uk/co/collom/parkping/*.java "$output/gen/uk/co/collom/parkping/R.java"
"$JAVA_HOME/bin/jar" --create --file "$output/classes.jar" -C "$output/classes" .
"$build_tools/d8" --release --min-api 26 --lib "$android_jar" --output "$output/dex" "$output/classes.jar"
python3 - "$output" <<'PY'
import sys
from pathlib import Path
from zipfile import ZipFile
output = Path(sys.argv[1])
with ZipFile(output / 'resources.apk') as source, ZipFile(output / 'unsigned.apk', 'w') as dest:
    for info in source.infolist():
        dest.writestr(info, source.read(info.filename))
    for dex in sorted((output / 'dex').glob('*.dex')):
        dest.write(dex, dex.name)
PY
"$build_tools/zipalign" -f -p 4 "$output/unsigned.apk" "$output/aligned.apk"
# Local builds default to a development key. CI releases supply a persistent key via secrets.
key_dir="${PARKPING_KEY_DIR:-$PWD/.local-signing}"
keystore="${PARKPING_KEYSTORE_FILE:-$key_dir/debug.keystore}"
if [[ -z "${PARKPING_KEYSTORE_FILE:-}" && ! -f "$keystore" ]]; then
    mkdir -p "$key_dir"
    "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$key_dir/debug.keystore" \
        -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 \
        -validity 10000 -dname 'CN=Android Debug,O=Android,C=GB'
fi
export PARKPING_KEYSTORE_PASSWORD="${PARKPING_KEYSTORE_PASSWORD:-android}"
export PARKPING_KEY_PASSWORD="${PARKPING_KEY_PASSWORD:-android}"
"$build_tools/apksigner" sign --ks "$keystore" --ks-key-alias "${PARKPING_KEY_ALIAS:-androiddebugkey}" \
    --ks-pass env:PARKPING_KEYSTORE_PASSWORD --key-pass env:PARKPING_KEY_PASSWORD \
    --out "$output/$apk_name" "$output/aligned.apk"
"$build_tools/apksigner" verify --verbose "$output/$apk_name"
printf 'APK: %s\n' "$output/$apk_name"
