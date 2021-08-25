#!/bin/bash
source $HOME/.bash_profile
mvn clean package

kill -9 $(lsof -t -i:8888)
echo "Killed process running on port 8888"

java -jar target/olx-scrapper-1.0-SNAPSHOT.jar