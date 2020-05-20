#!/bin/sh
docker rm -f $(docker ps -a | grep "link" | awk '{print $1}')
docker rmi link
#外部打包
mvn -Ppackage-all -DskipTests clean install -U
docker build -t link .
docker run -d --net=host -e "SERVER_PORT=8000" --name client1 link
docker run -d --net=host -e "SERVER_PORT=8001" --name client2 link
docker run -d --net=host -p 8003:8003 -p 8004:8004 -e "SERVER_PORT=8002" --name server link
docker ps -a
