#!/usr/bin/env bash
# shell script to run mouse qtl pipeline
. /etc/profile

APPNAME=MouseQtl
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

ELIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
    ELIST="$ELIST,rgd.pipelines@mcw.edu"
fi

cd $APPDIR
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
export MOUSE_QTL_OPTS="$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@" > cron.log 2>&1

mailx -s "[$SERVER] MouseQtl pipeline complete" $ELIST < $APPDIR/logs/core.log
