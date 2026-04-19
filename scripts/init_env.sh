#!/usr/bin/env bash
# Source this (don't execute): . scripts/init_env.sh
# Sets JAVA_HOME, ANDROID_HOME, and PATH for Cola Music builds.
SDK_ROOT="${SDK_ROOT:-$HOME/android-sdk}"
export JAVA_HOME="${SDK_ROOT}/jdk17"
export ANDROID_HOME="${SDK_ROOT}"
export ANDROID_SDK_ROOT="${SDK_ROOT}"
export PATH="${JAVA_HOME}/bin:${SDK_ROOT}/cmdline-tools/latest/bin:${SDK_ROOT}/platform-tools:${PATH}"
