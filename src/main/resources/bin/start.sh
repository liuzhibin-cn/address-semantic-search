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

echo -e "Starting the service ...\c"
nohup java -Ddubbo.spring.config=spring-config.xml -Ddubbp.jetty.port="8080" -Ddubbo.jetty.page=log,status,system -classpath $CONF_DIR:$LIB_JARS com.alibaba.dubbo.container.Main spring jetty > $DEPLOY_DIR/log/address-service-stdout.log 2>&1 &

# Java进程启动成功，将PID写入文件
if [[ "$?" = "0" && "$!" != "" ]]; then
    # 检查进程是否启动成功
    sleep 2
    echo ""
    COUNT=`ps -ef | grep "com.alibaba.dubbo.container.Main" | grep -v "grep" | grep "$!" | wc -l`
    if [ $COUNT -gt 0 ]; then
        echo $! > $BIN_DIR/addrmatch.pid
        echo "Service started successfully, PID: $!"
        exit 0
    fi
fi
echo "Service not started because of errors:"
cat $DEPLOY_DIR/log/address-service-stdout.log