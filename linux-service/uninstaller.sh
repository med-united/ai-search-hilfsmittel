#!/bin/bash
sudo systemctl stop aisearch
sudo systemctl disable aisearch.service
sudo rm -R /opt/ai-search
sudo rm /etc/systemd/system/aisearch.service
echo "aisearch successfully uninstalled"
