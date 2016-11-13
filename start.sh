#!/usr/bin/env bash
git pull
sbt pack
chmod 777 ./target/pack/bin/main

sh stop.sh
nohup ./target/pack/bin/main &
