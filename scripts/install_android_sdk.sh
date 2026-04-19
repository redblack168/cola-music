#!/usr/bin/env bash
# Install JDK 17 (Temurin) + Android command-line tools + SDK packages into ~/android-sdk.
# No sudo. Idempotent. Re-run safely.
set -euo pipefail

SDK_ROOT="${SDK_ROOT:-$HOME/android-sdk}"
JDK_VER="17.0.13+11"
JDK_TAG="jdk-17.0.13%2B11"
JDK_FILE_TAG="17.0.13_11"
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/${JDK_TAG}/OpenJDK17U-jdk_x64_linux_hotspot_${JDK_FILE_TAG}.tar.gz"
JDK_SHA256="https://github.com/adoptium/temurin17-binaries/releases/download/${JDK_TAG}/OpenJDK17U-jdk_x64_linux_hotspot_${JDK_FILE_TAG}.tar.gz.sha256.txt"

CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDLINE_SHA256="2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258"

mkdir -p "${SDK_ROOT}"
cd "${SDK_ROOT}"

# --- JDK 17 ---
if [ ! -x "${SDK_ROOT}/jdk17/bin/javac" ]; then
  echo "[jdk] downloading Temurin JDK ${JDK_VER}..."
  curl -fL -o jdk.tar.gz "${JDK_URL}"
  curl -fL -o jdk.tar.gz.sha256.txt "${JDK_SHA256}"
  ( cd "${SDK_ROOT}" && awk '{print $1"  jdk.tar.gz"}' jdk.tar.gz.sha256.txt | sha256sum -c - )
  rm -rf jdk17 jdk-*
  tar -xzf jdk.tar.gz
  mv jdk-* jdk17
  rm jdk.tar.gz jdk.tar.gz.sha256.txt
  echo "[jdk] installed at ${SDK_ROOT}/jdk17"
else
  echo "[jdk] already installed at ${SDK_ROOT}/jdk17"
fi

export JAVA_HOME="${SDK_ROOT}/jdk17"
export PATH="${JAVA_HOME}/bin:${PATH}"

# --- Android cmdline-tools ---
if [ ! -x "${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "[sdk] downloading Android cmdline-tools..."
  curl -fL -o cmdline-tools.zip "${CMDLINE_URL}"
  echo "${CMDLINE_SHA256}  cmdline-tools.zip" | sha256sum -c -
  rm -rf cmdline-tools
  mkdir -p cmdline-tools
  unzip -q cmdline-tools.zip -d cmdline-tools
  mv cmdline-tools/cmdline-tools cmdline-tools/latest
  rm cmdline-tools.zip
  echo "[sdk] cmdline-tools installed"
else
  echo "[sdk] cmdline-tools already installed"
fi

export ANDROID_HOME="${SDK_ROOT}"
export ANDROID_SDK_ROOT="${SDK_ROOT}"
export PATH="${SDK_ROOT}/cmdline-tools/latest/bin:${SDK_ROOT}/platform-tools:${PATH}"

# --- Accept licenses + install packages ---
echo "[sdk] accepting licenses..."
yes | sdkmanager --licenses >/dev/null 2>&1 || true

echo "[sdk] installing platform-tools, platforms;android-34, build-tools;34.0.0..."
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null

echo "[done] JDK 17 + Android SDK ready under ${SDK_ROOT}"
echo "Source scripts/init_env.sh to use them in a shell."
