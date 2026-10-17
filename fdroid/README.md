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
   The release source tag is `v3.1.1`; never move a published tag. Future releases
   require a new version/code and tag. Later documentation commits do not change
   the source used by this released APK.

## 2. Verify the source build

From a fresh clone of the tag, with Node.js/npm, JDK 21 and Android SDK set up:

```sh
npm ci
npm run test:security
npm run test:i18n
npm run build
npx cap sync android --deployment
cd android
env -u KEYSTORE_PASSWORD -u KEY_PASSWORD ./gradlew clean assembleRelease lintRelease
```

With no keystore/password variables, the expected result is
`android/app/build/outputs/apk/release/app-release-unsigned.apk`. Inspect it
with `apkanalyzer manifest print ...` and test a separately signed copy on an
arm64-v8a emulator or device. The release intentionally contains only
`arm64-v8a` native libraries; R8 and resource shrinking are disabled because
the 3.0.19 configuration caused a runtime startup crash. Also run
`./gradlew lintRelease`.

## 3. Add store listing assets

The repository contains `fastlane/metadata/android/en-US` and `zh-CN` listing
text after this preparation. Before submission, add at least two real phone
screenshots under each locale's `images/phoneScreenshots/` directory. Capture
them from this exact release without private server addresses, real account
names, private message content, QR codes, or notification topics. Clearly
fictional demo identities and content are acceptable.

## 4. Current release and submission

As of 2026-10-17:

- [GitHub 3.1.1 Release](https://github.com/619dev/ppl-android/releases/tag/v3.1.1)
  contains the signed `app-release.apk` and `SHA256SUMS`.
- Initial inclusion [MR !46295](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46295)
  was merged; the original RFP is `fdroid/rfp#4291`.
- Version update [MR !52845](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/52845)
  uses `619dev/fdroiddata:codex/paperphonelite-3.1.0`, based on upstream master.
  Its sole changed file is `metadata/com.fm619.paperphonelite.yml`, retaining
  only 3.1.1 (`versionCode 30101`) with source tag `v3.1.1`.
- Local client build, security/localization tests, Android Release build, lint,
  APK signing and signature-copy equality checks passed. Native-device runtime
  acceptance is pending. Consult MR CI for independent F-Droid reproduction;
  the local signature-copy check alone does not establish that reproduction.

Release APK SHA-256:

```text
See SHA256SUMS in the matching GitHub Release.
```

Allowed signing certificate SHA-256:

```text
39c14b942f92520723292a32759f5644638e4123ece96ed8fce5177545c39952
```

## 5. Version updates after inclusion

Use a version-update MR against `fdroid/fdroiddata:master`; do not open a new
application request or modify the merged initial MR. Keep only the latest build
entry in this project's submitted recipe. Preserve the allowed signing key,
`Binaries` URL and narrowly scoped scanner exception unless an actual build
change requires review.

## 6. Original packaging request

[`RFP.md`](RFP.md) records the original request and links the current release
and update MR. It is a historical reference, not a new submission template.
Do not reopen a duplicate RFP for a version update.

## 7. Update fdroiddata

1. Use fork `619dev/fdroiddata`; the 3.1.1 branch is
   `codex/paperphonelite-3.1.0`.
2. Start each new update branch from current `fdroid/fdroiddata:master`.
3. Copy `fdroid/com.fm619.paperphonelite.yml` to
   `metadata/com.fm619.paperphonelite.yml` in that clone.
4. Verify that public tag `v3.1.1` resolves to the same immutable commit
   recorded by the release and metadata review.
5. Run the official checks in the F-Droid buildserver container:

```sh
fdroid readmeta
fdroid rewritemeta com.fm619.paperphonelite
fdroid lint com.fm619.paperphonelite
fdroid checkupdates --allow-dirty com.fm619.paperphonelite
fdroid build com.fm619.paperphonelite
```

6. Fix every error. Review any scanner exception: the included `scanignore`
   is limited to Capacitor's source template archive and must not be broadened
   merely to silence findings.
7. Commit only the metadata file, push the update branch, and use the matching update MR
   against `fdroid/fdroiddata:master` (3.1.1: !52845). Monitor CI and reviewer comments.

## 8. Future releases

For every release, update `package.json`, `android/app/build.gradle`, visible
in-app version text, and changelog together. Increase `versionCode`, commit,
then create a matching immutable `v<versionName>` tag. The metadata's tag-based
update check can then discover new versions.

This recipe enables reproducible-build signing with `Binaries` and
`AllowedAPKSigningKeys`. Cross-channel upgrades require matching certificates
on the final distributed APKs; verify them rather than assuming compatibility.

## 9. Preserve the release APK layout during signing

When signing an unsigned Gradle APK for GitHub, use:

```sh
scripts/sign-release.sh android/app/build/outputs/apk/release/app-release-unsigned.apk release/3.1.1/app-release.apk
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
apksigcopier copy release/3.1.1/app-release.apk android/app/build/outputs/apk/release/app-release-unsigned.apk /tmp/ppl-signature-copy.apk
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify --verbose /tmp/ppl-signature-copy.apk
cmp release/3.1.1/app-release.apk /tmp/ppl-signature-copy.apk
```

Repeat with the independent F-Droid unsigned artifact when available. Do not
report CI reproducibility as passed based only on the local unsigned artifact.
