#!/usr/bin/env bash
set -euo pipefail

# Repairs the incomplete Gradle Wrapper JAR using a real Gradle 8.9 installation.
# Install Gradle 8.9 and JDK 17 first, then run this script from the repository root.
if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: A system Gradle installation is required to regenerate the wrapper." >&2
  echo "Install Gradle 8.9, then rerun: bash scripts/repair-gradle-wrapper.sh" >&2
  exit 1
fi

if ! command -v jar >/dev/null 2>&1; then
  echo "ERROR: Java JDK tools (jar) were not found. Install/configure JDK 17." >&2
  exit 1
fi

rm -f gradle/wrapper/gradle-wrapper.jar
gradle wrapper --gradle-version 8.9 --distribution-type bin
test -s gradle/wrapper/gradle-wrapper.jar
if ! jar tf gradle/wrapper/gradle-wrapper.jar | grep -q 'org/gradle/wrapper/GradleWrapperMain.class'; then
  echo "ERROR: Generated wrapper JAR is missing GradleWrapperMain." >&2
  exit 1
fi
chmod +x gradlew
./gradlew --version
echo "Gradle Wrapper repair completed."
