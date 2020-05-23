#!/bin/sh
docker rm -f $(docker ps -a | grep "link" | awk '{print $1}')
docker rmi link
docker run -d --net=host --cpuset-cpus="0,1" -e "SERVER_PORT=8000" --name client1 link
docker run -d --net=host --cpuset-cpus="2,3" -e "SERVER_PORT=8001" --name client2 link
docker run -d --net=host --cpuset-cpus="4" -e "SERVER_PORT=8002" --name server link
docker ps -a
