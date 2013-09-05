LOGGING_CONFIG="-Djava.util.logging.config.file=config/logging.properties"
CLASSPATH="bin/bootstrap.jar"
java "$LOGGING_CONFIG" -classpath "$CLASSPATH" com.dinstone.launcher.Launcher stop
