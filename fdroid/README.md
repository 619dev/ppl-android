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
5. Tag the exact reviewed commit: `git tag -s v3.0.10 -m "PaperPhoneLite 3.0.10"`
   and then push that tag. If `v3.0.10` already exists, do not move it: release a
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
emulator or device. Also run `./gradlew lintRelease`.

## 3. Add store listing assets

The repository contains `fastlane/metadata/android/en-US` and `zh-CN` listing
text after this preparation. Before submission, add at least two real phone
screenshots under each locale's `images/phoneScreenshots/` directory. Capture
them from this exact release without private server addresses, account names,
message content, QR codes, or notification topics.

## 4. Submit to fdroiddata

1. Create a GitLab account, fork `https://gitlab.com/fdroid/fdroiddata`, and
   clone your fork.
2. Create a branch named `com.fm619.paperphonelite` from current `master`.
3. Copy `fdroid/com.fm619.paperphonelite.yml` to
   `metadata/com.fm619.paperphonelite.yml` in that clone.
4. Verify that the public `v3.0.10` tag exists and resolves to the intended
   source. Replace `commit: v3.0.10` with the immutable full commit hash if the
   F-Droid reviewer requests it.
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
7. Commit only the metadata file with message `New App: PaperPhoneLite`, push
   the branch, and open a merge request against `fdroid/fdroiddata:master`.
   Complete its new-app checklist and monitor both CI and reviewer comments.

## 5. Future releases

For every release, update `package.json`, `android/app/build.gradle`, visible
in-app version text, and changelog together. Increase `versionCode`, commit,
then create a matching immutable `v<versionName>` tag. The metadata's tag-based
update check can then discover new versions.

F-Droid signs its APK independently. Users switching between a GitHub-signed
build and the F-Droid build must uninstall first unless reproducible-build
signing is separately configured and accepted.
