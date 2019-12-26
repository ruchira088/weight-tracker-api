#!/usr/bin/env bash

read -r -d '' EXECUTE << EOM
  cd /home/ubuntu/Data && \
  docker-compose down && \
  sudo rm -rf /home/ubuntu/Data/redis /home/ubuntu/Data/postgresql && \
  mkdir redis postgresql && \
  chmod -R 777 /home/ubuntu/Data/redis /home/ubuntu/Data/postgresql && \
  docker-compose up -d && \
  docker ps | grep data_
EOM

ssh ubuntu@k8s.ruchij.com "$EXECUTE"
