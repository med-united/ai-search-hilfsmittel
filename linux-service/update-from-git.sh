#!/bin/bash
git pull
mvn clean package -Dquarkus.package.type=fast-jar -DskipTests
sudo systemctl stop aisearch
sudo rm -r /opt/ai-search/app/*
sudo cp target/quarkus-app/app/* /opt/ai-search/app
sudo rm -r /opt/ai-search/quarkus/*
sudo cp -r target/quarkus-app/quarkus/* /opt/ai-search/quarkus
sudo rm -r /opt/ai-search/lib/*
sudo cp -r target/quarkus-app/lib/* /opt/ai-search/lib
sudo cp sha256checksums /opt/ai-search/
sudo bash -c 'echo "$(date --iso-8601=s) $(git rev-parse --verify HEAD)" >> /opt/ai-search/update-linux-deployments.log'
sleep 5
sudo systemctl start aisearch
sudo systemctl status aisearch
