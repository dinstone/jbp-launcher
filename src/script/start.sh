#java -server -Xss256k -Xms4g -Xmx4g -Xmn1g -XX:PermSize=128m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/jvmlog/jvm.hprof -XX:+PrintClassHistogram -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC -cp bin/bootstrap.jar cn.citic21.startup.Bootstrap start > /dev/null  2>&1 &

#JAVA_GC="-XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/jvmlog/jvm.hprof -XX:+PrintClassHistogram -Xloggc:/tmp/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC"

JAVA_OPTS="-server -Xss256k -Xms1g -Xmx1g -XX:PermSize=128m -XX:MaxPermSize=128m -Djava.net.preferIPv4Stack=true"

#JPDA_OPTS="-agentlib:jdwp=transport=dt_socket,address=8888,server=y,suspend=y"

LOGGING_CONFIG="-Djava.util.logging.config.file=config/logging.properties"

CLASSPATH="bin/bootstrap.jar"

LAUNCHER_OUT="logs/launcher.out"

nohup java $JAVA_OPTS $JPDA_OPTS $LOGGING_CONFIG -classpath $CLASSPATH com.dinstone.launcher.Launcher start > $LAUNCHER_OUT 2>&1 &
