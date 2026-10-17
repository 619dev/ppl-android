# F-Droid submission checklist

This directory is staging material. The final metadata file belongs in the
`fdroiddata` repository as `metadata/com.fm619.paperphonelite.yml`.

## 1. Publish a clean source repository

1. Change the exposed keystore passwords immediately. Earlier builds used
   fallback passwords in `android/app/build.gradle`; assume that key is no
   longer secret if the source was ever shared.
2. Move `paperphone-release.keystore` to a private backup outside this source
   tree. It is ignored by Git, but keeping it beside the source invites an
   accidental leak.
3. Initialize Git if necessary, commit all required source files (including
   `package-lock.json`), and publish the repository at
   `https://github.com/619dev/ppl-android`.
4. Confirm that generated directories and secrets are absent with
   `git status --ignored` and GitHub's web UI. Never commit `node_modules`,
   `dist`, Android build output, `local.properties`, or a keystore.
5. Tag the exact reviewed commit: `git tag -s v3.1.0 -m "PaperPhoneLite 3.1.0"`
   and then push that tag. If `v3.1.0` already exists, do not move it: release a
   new version/code and tag instead.

## 2. Verify the source build

From a fresh clone of the tag, with Node.js/npm, JDK 21 and Android SDK set up:

```sh
npm ci
npm run build
npx cap sync android --deployment
cd android
./gradlew clean assembleRelease
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

## 4. Current submission status

The initial submission `fdroid/fdroiddata!46295` was merged. Future releases
must use a new update merge request based on current `fdroid/fdroiddata:master`;
do not modify the merged submission or open a duplicate RFP.

- Original RFP: `fdroid/rfp#4291`
- Merged initial submission: `fdroid/fdroiddata!46295`

The update branch must contain only the latest PaperPhoneLite build entry.
The current version is `3.1.0` (`versionCode 30100`), using the immutable
`v3.1.0` source tag. Publish the signed GitHub release before submitting the
metadata update. F-Droid independently validates reproducibility and signing.

## 5. Submission routes

F-Droid currently provides two routes for a new app:

- **Request For Packaging (RFP):** open an issue in
  `https://gitlab.com/fdroid/rfp/-/issues/new` using the official `Default`
  template. This triggers an automated review and puts the app in the packaging
  queue, but does not guarantee that somebody will package it. A pre-filled
  draft for this app is available at `fdroid/RFP.md`.
- **Direct fdroiddata merge request:** if you are packaging the app yourself,
  submit the completed build metadata directly. This is the faster route once
  the build has been reproduced successfully. An RFP issue is not a required
  prerequisite for this route.

For a first submission, opening the RFP is useful even if a direct merge
request will follow: it runs the issuebot checks and leaves a public record of
the initial review. Link the RFP from the later merge request and close it when
the app is accepted.

## 6. Update the RFP

1. Update `fdroid/rfp#4291`; do not open a duplicate RFP.
2. Keep its application ID and repository information aligned with
   `fdroid/RFP.md`.
3. Link the current fdroiddata merge request and source release, when relevant to the original request.
4. Address any reported licensing, dependency, tracker, build, or metadata
   problems in the existing merge request.

## 7. Update fdroiddata

1. Use the existing fork and branch `619dev/fdroiddata:com.fm619.paperphonelite`.
2. Rebase it on current `fdroid/fdroiddata:master` when required by maintainers.
3. Copy `fdroid/com.fm619.paperphonelite.yml` to
   `metadata/com.fm619.paperphonelite.yml` in that clone.
4. Verify that public tag `v3.1.0` resolves to the same immutable commit
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
7. Commit only the metadata file, push the existing branch, and open a version-update merge request against
   `fdroid/fdroiddata:master`. Monitor its CI and reviewer comments.

## 8. Future releases

For every release, update `package.json`, `android/app/build.gradle`, visible
in-app version text, and changelog together. Increase `versionCode`, commit,
then create a matching immutable `v<versionName>` tag. The metadata's tag-based
update check can then discover new versions.

F-Droid signs its APK independently. Users switching between a GitHub-signed
build and the F-Droid build must uninstall first unless reproducible-build
signing is separately configured and accepted.

## 9. Preserve the release APK layout during signing

When signing an unsigned Gradle APK for GitHub, use:

```sh
scripts/sign-release.sh android/app/build/outputs/apk/release/app-release-unsigned.apk release/3.1.0/app-release.apk
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
