#!/bin/bash
source $HOME/.bash_profile
/usr/local/bin/mvn clean package
java -jar target/olx-scrapper-1.0-SNAPSHOT.jar 2>> application.log &