# F-Droid submission checklist

This directory is staging material. The final metadata file belongs in the
`fdroiddata` repository as `metadata/com.fm619.paperphonelite.yml`.

## 1. Publish a clean source repository

1. Keep keystores and signing passwords in private local storage. Earlier source
   versions contained fallback passwords; never reintroduce them or publish
   signing configuration. The current release retains the established certificate.
2. Commit source, lockfile, Fastlane descriptions and version metadata. Exclude
   `node_modules`, `dist`, Android build outputs, `local.properties` and keystores.
3. Build and validate the exact source revision before creating a release tag.
   The release source tag is `v3.1.7`; never move a published tag. Future releases
   require a new version/code and tag. Later documentation commits do not change
   the source used by this released APK.

## 2. Verify the source build

From a fresh clone of the tag, with Node.js/npm, JDK 21 and Android SDK set up:

```sh
npm ci
npm run test:security
npm run test:i18n
npm run test:media-cache
npm run build
npx cap sync android --deployment
cd android
env -u KEYSTORE_PASSWORD -u KEY_PASSWORD ./gradlew clean assembleRelease lintRelease
```

With no keystore/password variables, the expected result is
`android/app/build/outputs/apk/release/app-release-unsigned.apk`. Inspect it
with `apkanalyzer manifest print ...` and test a separately signed copy on an
arm64-v8a emulator or device. The release intentionally contains only
`arm64-v8a` native libraries. R8 minification is enabled with keep rules for
Capacitor and native Tor/transport entry points. Resource shrinking stays disabled.

## 3. Add store listing assets

The repository contains `fastlane/metadata/android/en-US` and `zh-CN` listing
text after this preparation. Before submission, add at least two real phone
screenshots under each locale's `images/phoneScreenshots/` directory. Capture
them from this exact release without private server addresses, real account
names, private message content, QR codes, or notification topics. Clearly
fictional demo identities and content are acceptable.

## 4. Release and F-Droid status

Android 3.1.7 uses version code 30107 and the source tag `v3.1.7`.
The [GitHub Release](https://github.com/619dev/ppl-android/releases/tag/v3.1.7)
is the distribution location for `app-release.apk` and `SHA256SUMS`.

Initial inclusion [MR !46295](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46295)
was merged. The previous [MR !57182](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57182)
is closed, and the official metadata includes 3.1.5.

## 5. Automatic version updates

The official metadata uses `AutoUpdateMode: Version` and
`UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$`. F-Droid's update check
reads `versionName` and `versionCode` from the tagged Android Gradle project
and generates the next build entry from the current recipe. Publish the matching
signed APK under the configured `Binaries` URL. Check the official metadata
and build results for the bot-generated update and independent reproduction.

The YAML in this directory mirrors the confirmed F-Droid recipe. Keep the
historical builds, allowed certificate, binary URL, and scanner settings when
refreshing the mirror. The original request is recorded in [RFP.md](RFP.md).

## 6. Preserve the release APK layout during signing

When signing an unsigned Gradle APK for GitHub, use:

```sh
scripts/sign-release.sh android/app/build/outputs/apk/release/app-release-unsigned.apk release/3.1.7/app-release.apk
```

Set `ANDROID_SDK_ROOT`, `KEYSTORE_PASSWORD` and `KEY_PASSWORD` in the local
signing environment. The script uses Android Build Tools 35.0.0 and preserves
ZIP alignment with `--alignment-preserved true`; v2/v3 signing is sufficient
for this app's minimum Android version. Never store signing passwords in source.

Before uploading, copy its signature onto the independent F-Droid unsigned APK
using `apksigcopier copy`, then verify that APK with `apksigner verify`.
The resulting APK must verify and match the signed release byte for byte.
Default apksigner realignment can break this check even when every extracted
file and the original unsigned APK match.

To check signing layout locally, run from this repository's root:

```sh
apksigcopier copy release/3.1.7/app-release.apk android/app/build/outputs/apk/release/app-release-unsigned.apk /tmp/ppl-signature-copy.apk
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify --verbose /tmp/ppl-signature-copy.apk
cmp release/3.1.7/app-release.apk /tmp/ppl-signature-copy.apk
```

Repeat with the independent F-Droid unsigned artifact when available. Do not
report CI reproducibility as passed based only on the local unsigned artifact.
