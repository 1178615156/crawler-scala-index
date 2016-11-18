#!/usr/bin/env bash
sh stop.sh
git pull
sbt clean pack
chmod 777 ./target/pack/bin/main

nohup ./target/pack/bin/main &
