#!/usr/bin/env bash
# Build + publish Family Space AAB to Google Play internal testing (Linux)
# Prerequisites: deploy/play-store-credentials.json, keystore.properties (see docs/play-store-automation.md)

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

CREDS_PATH="${PLAY_STORE_JSON:-$PROJECT_ROOT/deploy/play-store-credentials.json}"
for candidate in \
  "$CREDS_PATH" \
  "$PROJECT_ROOT/deploy/play-store-credentials.json" \
  "$PROJECT_ROOT/secrets/play-store-credentials.json" \
  "$PROJECT_ROOT/secrets/play-store-uploader-service-account.json"; do
  if [[ -f "$candidate" ]]; then
    CREDS_PATH="$candidate"
    export PLAY_STORE_JSON="$candidate"
    break
  fi
done
KEYSTORE_PATH="$PROJECT_ROOT/keystore.properties"
if [[ ! -f "$KEYSTORE_PATH" && -f "$PROJECT_ROOT/secrets/keystore.properties" ]]; then
  KEYSTORE_PATH="$PROJECT_ROOT/secrets/keystore.properties"
fi

if [[ ! -f "$CREDS_PATH" ]]; then
  echo ""
  echo "Fichier manquant : credentials Play Store (deploy/ ou secrets/)"
  echo "Ou définissez PLAY_STORE_JSON=/chemin/vers/le-json"
  echo ""
  exit 1
fi

# Gradle Play plugin lit deploy/play-store-credentials.json
mkdir -p "$PROJECT_ROOT/deploy"
if [[ "$CREDS_PATH" != "$PROJECT_ROOT/deploy/play-store-credentials.json" ]]; then
  ln -sf "$CREDS_PATH" "$PROJECT_ROOT/deploy/play-store-credentials.json"
fi

if [[ ! -f "$KEYSTORE_PATH" ]]; then
  echo "keystore.properties manquant — impossible de signer l'AAB release."
  exit 1
fi

if [[ ! -f "$PROJECT_ROOT/keystore_familly" && -f "$PROJECT_ROOT/secrets/keystore_familly" ]]; then
  cp "$PROJECT_ROOT/secrets/keystore_familly" "$PROJECT_ROOT/keystore_familly"
fi
if [[ ! -f "$PROJECT_ROOT/keystore.properties" && "$KEYSTORE_PATH" != "$PROJECT_ROOT/keystore.properties" ]]; then
  cp "$KEYSTORE_PATH" "$PROJECT_ROOT/keystore.properties"
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

export GRADLE_OPTS="${GRADLE_OPTS:-} -Xmx4g -XX:MaxMetaspaceSize=512m"

echo "==> Build + publish to Play Store (internal track)..."
./gradlew :androidApp:publishReleaseBundle

AAB="$PROJECT_ROOT/androidApp/build/outputs/bundle/release/androidApp-release.aab"
echo ""
echo "PUBLISH_OK"
echo "AAB local : $AAB"
echo "Piste     : internal (test interne)"
echo ""
