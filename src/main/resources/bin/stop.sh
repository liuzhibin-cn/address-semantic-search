#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`

if [ ! -f $BIN_DIR/pid.sock ]; then
    echo "The PID file pid.sock not exists"
    exit 0
fi

PID=`cat $BIN_DIR/pid.sock`
if [ -z "$PID" ]; then
    echo "Process $PID not found"
    rm -rf $BIN_DIR/pid.sock
    exit 0
fi

# 检查进程是否启动成功
COUNT=`ps -ef | grep "com.alibaba.dubbo.container.Main" | grep -v "grep" | grep "$PID" | wc -l`
if [ 0 == $COUNT ]; then
    echo "Process $PID not found"
    rm -rf $BIN_DIR/pid.sock
    exit 0
fi

kill -9 $PID
rm -rf $BIN_DIR/pid.sock
echo "OK! PID: $PID"
