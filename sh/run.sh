#!/bin/sh
if [ $SERVER_PORT = "8002" ];
then
    sh server.sh
else
    sh client.sh
fi

