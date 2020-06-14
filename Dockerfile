# Builder
FROM maven:3-jdk-8-slim AS builder

RUN mkdir /opt/linkTracking
WORKDIR /opt/linkTracking
#RUN rm -rf /usr/share/maven/conf/settings.xml
COPY . ./
#COPY sh/settings.xml /usr/share/maven/conf/
RUN mvn -Ppackage-all -DskipTests clean install -U

# App
FROM openjdk:8-jre-slim

RUN mkdir /opt/linkTracking
COPY --from=builder ./opt/linkTracking/sh /opt/linkTracking
COPY --from=builder ./opt/linkTracking/target /opt/linkTracking
WORKDIR /opt/linkTracking
RUN chmod 777 run.sh
CMD "./run.sh"

