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
5. Tag the exact reviewed commit: `git tag -s v3.0.20 -m "PaperPhoneLite 3.0.20"`
   and then push that tag. If `v3.0.20` already exists, do not move it: release a
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

The initial submission is already open. Update these existing items rather than
creating duplicates:

- RFP: `fdroid/rfp#4291`
- fdroiddata merge request: `fdroid/fdroiddata!46295`

The merge-request branch must contain only the latest PaperPhoneLite build
entry. The current candidate is `3.0.20` (`versionCode 30020`) at immutable
source commit `a28748330c833358a8fc9f5f30c53784e12a20db`. Its signed GitHub build has
passed installation and runtime testing; F-Droid will independently reproduce
and sign the unsigned build.

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
3. Link the current fdroiddata merge request and source release, and notify the
   maintainers when a tested replacement build is pushed.
4. Address any reported licensing, dependency, tracker, build, or metadata
   problems in the existing merge request.

## 7. Update fdroiddata

1. Use the existing fork and branch `619dev/fdroiddata:com.fm619.paperphonelite`.
2. Rebase it on current `fdroid/fdroiddata:master` when required by maintainers.
3. Copy `fdroid/com.fm619.paperphonelite.yml` to
   `metadata/com.fm619.paperphonelite.yml` in that clone.
4. Verify that public tag `v3.0.20` resolves to immutable commit
   `a28748330c833358a8fc9f5f30c53784e12a20db`.
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
7. Commit only the metadata file, push the existing branch, and update
   `fdroid/fdroiddata!46295`. Monitor its CI and reviewer comments.

## 8. Future releases

For every release, update `package.json`, `android/app/build.gradle`, visible
in-app version text, and changelog together. Increase `versionCode`, commit,
then create a matching immutable `v<versionName>` tag. The metadata's tag-based
update check can then discover new versions.

F-Droid signs its APK independently. Users switching between a GitHub-signed
build and the F-Droid build must uninstall first unless reproducible-build
signing is separately configured and accepted.
