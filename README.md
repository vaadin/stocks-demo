# Vaadin and Speedment stock data demo

To create database, get <API-KEY> to Alpha Vantage for trade-data: https://www.alphavantage.co/support/#api-key

## Running MySQL, application, and database creator in three separate containers
1. ./run_docker_containers.sh <API-KEY>

## Running MySQL and application in two separate containers without recreating database
1. mvn clean package
2. docker-compose up app

## Running JVM on host machine standalone
1. Create a mysql MySQL-database
2. Uppdate ./create_database.sh with IP-adress and credentials
4. Run ./create_database.sh <API-KEY> (takes a long time, can be interrupted after a few minutes for a smaller data set)