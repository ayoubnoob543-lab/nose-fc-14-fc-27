#!/usr/bin/env bash
set -Eeuo pipefail
BASE=/home/ubuntu/work/android-toolchain
mkdir -p "$BASE"
cd "$BASE"
if [ ! -x gradle-8.10.2/bin/gradle ]; then
  curl -fL --retry 2 -o gradle.zip https://services.gradle.org/distributions/gradle-8.10.2-bin.zip
  unzip -q -o gradle.zip
fi
if [ ! -x cmdline-tools/latest/bin/sdkmanager ]; then
  curl -fL --retry 2 -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  mkdir -p cmdline-tools/latest
  unzip -q -o cmdline-tools.zip -d cmdline-tools/latest
  mv cmdline-tools/latest/cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
fi
export ANDROID_HOME="$BASE/sdk"
export PATH="$BASE/cmdline-tools/latest/bin:$BASE/sdk/platform-tools:$PATH"
mkdir -p "$ANDROID_HOME"
yes | sdkmanager --licenses >/dev/null || true
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
printf 'ANDROID_HOME=%s\n' "$ANDROID_HOME"
