#!/usr/bin/env bash
git pull
sbt clean pack
chmod 777 ./target/pack/bin/main

sh stop.sh
nohup ./target/pack/bin/main &
