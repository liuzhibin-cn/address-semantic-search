#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=$DEPLOY_DIR/conf

if [ ! -d $DEPLOY_DIR/log ]; then
    mkdir $DEPLOY_DIR/log
fi

LIB_DIR=$DEPLOY_DIR/lib
LIB_JARS=`ls $LIB_DIR | grep .jar | awk '{print "'$LIB_DIR'/"$0}' | tr "\n" ":"`

echo -e "Run regression test ...\c"
java -Xmx4096m -classpath $CONF_DIR:$CONF_DIR/dic:$LIB_JARS com.rrs.rd.address.misc.RegressionTestRunTest $1
