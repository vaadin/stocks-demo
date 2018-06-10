#!/usr/bin/env bash

if [[ $# -eq 0 ]]; then
    # This key is a short string, so easier to supply via command line
    echo "You need to supply an API key for Alpha Vantage to get trade data. Get one here: https://www.alphavantage.co/support/#api-key"
    echo "Please rerun this script with the obtained key as a parameter."
    exit
fi

function trialinfo {
    echo "A free trial license can be obtained from the initializer page: https://www.speedment.com/initializer/"
    echo "Please put the obtained license key in a file named ${SPEEDMENT_LICENSE_FILE}"
    exit
}

# This key is long, so we keep it in a file
SPEEDMENT_LICENSE_FILE=license.txt
USER_LICENSE=~/.speedment/.licenses

if [[ ! -s ${SPEEDMENT_LICENSE_FILE} ]] && [[ -s ${USER_LICENSE} ]]; then
    tail -1 ${USER_LICENSE} > ${SPEEDMENT_LICENSE_FILE}
    echo "Trying to use the Speedment license found in ${USER_LICENSE}"
    echo "To use any other license for this demo, please modify the contents of file ${SPEEDMENT_LICENSE_FILE}"
fi

if [[ ! -s ${SPEEDMENT_LICENSE_FILE} ]]; then
    echo "A Speedment Enterprise license is needed to run this application."
    trialinfo
fi

if [[ $(wc -c < ${SPEEDMENT_LICENSE_FILE}) -lt 100 ]]; then
    echo "The Speedment Enterprise license in ${SPEEDMENT_LICENSE_FILE} seems to be broken. Please replace it."
    trialinfo
fi

if [[ ! $(wc -l < ${SPEEDMENT_LICENSE_FILE}) -eq 1 ]]; then
    echo "The Speedment Enterprise license in ${SPEEDMENT_LICENSE_FILE} shall only have a single license on a single line. Please update the file."
    trialinfo
fi


export SPEEDMENT_LICENSE=$(cat ${SPEEDMENT_LICENSE_FILE})
export STOCKS_API_KEY=$1

mvn -q clean package

docker-compose build
docker-compose up