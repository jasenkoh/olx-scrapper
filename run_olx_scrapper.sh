#!/bin/bash
source $HOME/.bash_profile

echo "Pull latest changes"
git fetch
git pull

echo "Generate new jar file"
mvn clean package

kill -9 $(lsof -t -i:8888)
echo "Killed process running on port 8888"

echo "Starting application..."
nohup java -jar target/olx-scrapper-1.0-SNAPSHOT.jar &
