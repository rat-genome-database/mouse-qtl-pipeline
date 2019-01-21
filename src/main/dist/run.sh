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
java -Dspring.config=$APPDIR/../properties/default_db.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/$APPNAME.jar "$@" > cron.log 2>&1

mailx -s "[$SERVER] MouseQtl pipeline complete" $ELIST < $APPDIR/logs/detail.log
