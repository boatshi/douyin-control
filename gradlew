#!/usr/bin/env bash
export JAVA_HOME="/d/AndroidStudio/jbr"
export ANDROID_HOME="/c/Users/Administrator/AppData/Local/Android/Sdk"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
"$JAVA_HOME/bin/java" -Dorg.gradle.appname=gradlew -cp "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
