#!/bin/sh

LAUNCHER_HOME=$(cd `dirname $0`; pwd)

LOGGING_CONFIG="-Djava.util.logging.config.file=config/logging.properties"
CLASSPATH="bin/bootstrap.jar"
java "$LOGGING_CONFIG" -Dlauncher.home=$LAUNCHER_HOME -classpath "$CLASSPATH" com.dinstone.launcher.Launcher stop
