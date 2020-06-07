#!/bin/sh

#===========================================================================================
# Java Environment Setting
#===========================================================================================
error_exit ()
{
    echo "ERROR: $1 !!"
    exit 1
}

[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)!"

export JAVA_HOME
export JAVA="$JAVA_HOME/bin/java"
export BASE_DIR=$(dirname $0)/..

#===========================================================================================
# JVM Configuration
#===========================================================================================
JAVA_OPT="${JAVA_OPT} -Dserver.port=$SERVER_PORT"
JAVA_OPT="${JAVA_OPT} -server -Xms1536m -Xmx1536m -Xmn320m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m"
#JAVA_OPT="${JAVA_OPT} -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=70 -XX:+CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8  -XX:-UseParNewGC"
JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:G1ReservePercent=25 -XX:InitiatingHeapOccupancyPercent=30 -XX:SoftRefLRUPolicyMSPerMB=0 -XX:SurvivorRatio=8"
#JAVA_OPT="${JAVA_OPT} -verbose:gc -Xloggc:${GC_LOG_DIR}/rmq_srv_gc_%p_%t.log -XX:+PrintGCDetails"
#JAVA_OPT="${JAVA_OPT} -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=30m"
JAVA_OPT="${JAVA_OPT} -XX:+OmitStackTraceInFastThrow"
#JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -XX:+AlwaysPreTouch"
JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages"
JAVA_OPT="${JAVA_OPT} -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${BASE_DIR}/lib"
#JAVA_OPT="${JAVA_OPT} -Djava.rmi.server.hostname=10.66.1.107"
#JAVA_OPT="${JAVA_OPT} -Dcom.sun.management.jmxremote=true"
#JAVA_OPT="${JAVA_OPT} -Dcom.sun.management.jmxremote.ssl=false"
#JAVA_OPT="${JAVA_OPT} -Dcom.sun.management.jmxremote.authenticate=false"
#JAVA_OPT="${JAVA_OPT} -Dcom.sun.management.jmxremote.port=10802"
#JAVA_OPT="${JAVA_OPT} -Xdebug -Xrunjdwp:transport=dt_socket,address=9555,server=y,suspend=n"
#JAVA_OPT="${JAVA_OPT} ${JAVA_OPT_EXT}"
#JAVA_OPT="${JAVA_OPT} -jar ${CLASSPATH}"

java ${JAVA_OPT} -jar LinkTracking-1.0.0.jar
