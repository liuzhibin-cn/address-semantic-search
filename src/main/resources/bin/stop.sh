#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`

if [ ! -f $BIN_DIR/address-semantic-search.pid ]; then
    echo "The service PID file address-semantic-search.pid not exists"
    exit 0
fi

PID=`cat $BIN_DIR/address-semantic-search.pid`
if [ -z "$PID" ]; then
    echo "Service process $PID not found"
    rm -rf $BIN_DIR/address-semantic-search.pid
    exit 0
fi

# 检查进程是否启动成功
COUNT=`ps -ef | grep "com.alibaba.dubbo.container.Main" | grep -v "grep" | grep "$PID" | wc -l`
if [ 0 == $COUNT ]; then
    echo "Service process $PID not found"
    rm -rf $BIN_DIR/address-semantic-search.pid
    exit 0
fi

kill -9 $PID
rm -rf $BIN_DIR/address-semantic-search.pid
echo "OK! Service process $PID has been stopped!"
