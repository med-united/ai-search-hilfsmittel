#!/bin/sh

export QUARKUS_CONFIG_LOCATIONS=config/application.properties
exec /usr/bin/java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5035 -jar quarkus-run.jar
