#!/usr/bin/env bash

if [[ $# -eq 0 ]]; then
    echo "You need to supply an API kep for Alpha Vantage to get trade data. Get one here: https://www.alphavantage.co/support/#api-key"
    exit
fi

API_KEY=$1

# Clearly, the following may need to be updated to fit the MySQL setup to use
HOST="localhost"
USER="root"
PASSWORD="root"

echo "Connecting to MySQL server at ${HOST} with user ${USER} to create stocks database"

mvn -q clean package
java -jar data/target/data-1.0-SNAPSHOT.jar ${HOST} ${USER} ${PASSWORD} ${API_KEY}
