#!/usr/bin/env bash
# Build + publish Family Space AAB to Google Play internal testing (Linux)
# Prerequisites: deploy/play-store-credentials.json, keystore.properties (see docs/play-store-automation.md)

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

CREDS_PATH="$PROJECT_ROOT/deploy/play-store-credentials.json"
KEYSTORE_PATH="$PROJECT_ROOT/keystore.properties"

if [[ ! -f "$CREDS_PATH" ]]; then
  echo ""
  echo "Fichier manquant : deploy/play-store-credentials.json"
  echo "Suivez le guide : docs/play-store-automation.md"
  echo ""
  exit 1
fi

if [[ ! -f "$KEYSTORE_PATH" ]]; then
  echo "keystore.properties manquant — impossible de signer l'AAB release."
  exit 1
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  if command -v java >/dev/null 2>&1; then
    JAVA_BIN="$(readlink -f "$(command -v java)")"
    export JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
  fi
fi

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  for candidate in "$HOME/Android/Sdk" "$HOME/android-sdk"; do
    if [[ -d "$candidate" ]]; then
      export ANDROID_HOME="$candidate"
      break
    fi
  done
fi

export GRADLE_OPTS="${GRADLE_OPTS:-} -Xmx2g -XX:MaxMetaspaceSize=512m"

echo "==> Build AAB release..."
./gradlew :androidApp:bundleRelease --no-daemon

echo "==> Publish to Play Store (internal track)..."
./gradlew :androidApp:publishReleaseBundle --no-daemon

AAB="$PROJECT_ROOT/androidApp/build/outputs/bundle/release/androidApp-release.aab"
echo ""
echo "PUBLISH_OK"
echo "AAB local : $AAB"
echo "Piste     : internal (test interne)"
echo ""
