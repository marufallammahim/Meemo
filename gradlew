#!/bin/sh
#
# Gradle start up script for POSIX generated for Meemo Android project.
#
APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1 ; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname="gradlew" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
