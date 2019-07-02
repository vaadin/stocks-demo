FROM jetty:9.4.18-jre11

ENV STOCKS_MYSQL_HOST db

ENV JAVA_OPTIONS "-XX:MaxDirectMemorySize=50g"

COPY license.txt /var/lib/jetty/webapps/.license
COPY target/application-1.0-SNAPSHOT.war /var/lib/jetty/webapps/ROOT.war
