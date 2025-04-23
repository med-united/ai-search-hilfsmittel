#!/bin/bash
sudo mkdir -p /opt/ai-search/config
git pull
mvn clean package -Dquarkus.package.type=fast-jar -DskipTests
sudo cp -r target/quarkus-app/* /opt/ai-search/
sudo cp src/main/resources/application.properties /opt/ai-search/config/
sudo cp linux-service/run.sh /opt/ai-search/
sudo cp linux-service/aisearch.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable aisearch.service
sudo systemctl start aisearch
sudo systemctl status aisearch
