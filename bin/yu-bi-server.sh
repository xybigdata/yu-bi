#!/bin/bash

# yu-bi
# <p>
# Copyright 2021
# <p>
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# http://www.apache.org/licenses/LICENSE-2.0
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

BASE_DIR=$(cd "$(dirname "$0")/.."; pwd -P)

echo "working dir ${BASE_DIR}"

cd "${BASE_DIR}"

CLASS_PATH="${BASE_DIR}/lib/*"

START_CLASS="datart.DatartServerApplication"
JAVA_OPENS_ARGS=(
    "--add-opens=java.base/java.lang=ALL-UNNAMED"
)

datart_status(){
    result=`ps -ef | grep -v grep | grep "${BASE_DIR}/lib" | grep 'DatartServerApplication' | awk {'print $2'} | wc -l`

    if [[ $result -eq 0 ]]; then
        return 0
    else
        return 1
    fi
    }

datart_start(){
    source ~/.bashrc
    datart_status >/dev/null 2>&1
    if [[ $? -eq 0 ]]; then

        nohup  java -server -Xms2G -Xmx2G "${JAVA_OPENS_ARGS[@]}" -Dspring.profiles.active=config -Dfile.encoding=UTF-8 -cp "${CLASS_PATH}" "${START_CLASS}" &

    else
        echo ""
        PID=`ps -ef | grep -v grep | grep "${BASE_DIR}/lib" | grep 'DatartServerApplication' | awk {'print $2'}`
        echo "yu-bi is running now. PID is ${PID} "
    fi
}


datart_stop(){
    datart_status >/dev/null 2>&1
    if [[ $? -eq 0 ]]; then
        echo ""
        echo "yu-bi is not running."
        echo ""
    else
         ps -ef | grep -v grep | grep "$BASE_DIR/lib" | grep 'DatartServerApplication' | awk {'print $2'} | xargs kill -9

    fi
}


case $1 in
    start )
        echo ""
        echo "yu-bi starting..."
        echo ""
        datart_start
    ;;

    stop )
        echo ""

        echo "yu-bi stopping..."

        echo ""
        datart_stop
    ;;

    restart )
        echo "yu-bi restarting..."
        datart_stop
        echo ""
        datart_start
        echo "yu-bi is starting..."

    ;;

    status )
        datart_status>/dev/null 2>&1
        if [[ $? -eq 0 ]]; then
            echo ""
            echo "yu-bi is not running."
            echo ""
        else
            echo ""
            PID=`ps -ef | grep -v grep | grep "${BASE_DIR}/lib" | grep 'DatartServerApplication' | awk {'print $2'}`
            echo "yu-bi is running. PID is ${PID}"
            echo ""
        fi
     ;;

    * )
        echo "Usage: yu-bi-server.sh (start|stop|status|restart)"

esac
