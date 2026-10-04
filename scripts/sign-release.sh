#!/bin/sh
set -eu

# Preserve Gradle's ZIP layout so F-Droid can copy the release signature.
: "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD in the signing environment}"
: "${KEY_PASSWORD:?Set KEY_PASSWORD in the signing environment}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to the Android SDK directory}"

unsigned_apk=${1:?Usage: scripts/sign-release.sh unsigned.apk signed.apk}
signed_apk=${2:?Usage: scripts/sign-release.sh unsigned.apk signed.apk}
keystore=${PPL_RELEASE_KEYSTORE:-paperphone-release.keystore}
apksigner="$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner"

mkdir -p "$(dirname "$signed_apk")"
"$apksigner" sign --alignment-preserved true --v1-signing-enabled false \
    --ks "$keystore" --ks-key-alias paperphone \
    --ks-pass env:KEYSTORE_PASSWORD --key-pass env:KEY_PASSWORD \
    --out "$signed_apk" "$unsigned_apk"
"$apksigner" verify --verbose --print-certs "$signed_apk"
