#!/usr/bin/env bash

if [[ $# -eq 0 ]]; then
    echo "You need to supply an API kep for Alpha Vantage to get trade data. Get one here: https://www.alphavantage.co/support/#api-key"
    exit
fi

export STOCKS_API_KEY=$1

mvn -q clean package

docker-compose build
docker-compose up