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

# This number controls the size of the final database
EXTRAPOLATE_INTERVAL=60

echo "Connecting to ${HOST} with used ${USER}, will extrapolate data to get points every ${EXTRAPOLATE_INTERVAL} seconds."

mvn -q package exec:java -Dexec.mainClass=com.vaadin.demo.stockdata.backend.setup.DatabaseCreator -Dexec.args="${HOST} ${USER} ${PASSWORD} ${EXTRAPOLATE_INTERVAL} ${API_KEY}"