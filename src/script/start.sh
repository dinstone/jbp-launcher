#!/bin/sh

LAUNCHER_HOME=$(cd `dirname $0`; pwd)

#java -server -Xss256k -Xms4g -Xmx4g -Xmn1g -XX:PermSize=128m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/jvm.hprof -XX:+PrintClassHistogram -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC -cp bin/bootstrap.jar com.dinstone.launcher.Launcher start > /dev/null  2>&1 &
#JAVA_GC="-XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/jvm.hprof -XX:+PrintClassHistogram -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC"

JAVA_OPTS="-server -Xss256k -Xmn500m -Xms1g -Xmx1g -XX:PermSize=64m -XX:MaxPermSize=64m -XX:+UseConcMarkSweepGC -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"

#JMX_OPTS="-Djava.rmi.server.hostname=watchserver -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
#JPDA_OPTS="-agentlib:jdwp=transport=dt_socket,address=8888,server=y,suspend=y"

LOGGING_CONFIG="-Djava.util.logging.config.file=$LAUNCHER_HOME/config/logging.properties"

CLASSPATH="$LAUNCHER_HOME/bin/bootstrap.jar"

LAUNCHER_OUT="$LAUNCHER_HOME/logs/launcher.out"

nohup java $JAVA_OPTS $JMX_OPTS $JPDA_OPTS $LOGGING_CONFIG -Dlauncher.home=$LAUNCHER_HOME -classpath $CLASSPATH com.dinstone.launcher.Launcher start > $LAUNCHER_OUT 2>&1 &