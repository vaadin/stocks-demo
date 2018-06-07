FROM jetty:9.4.9

ENV STOCKS_MYSQL_HOST db

ENV JAVA_OPTIONS "-XX:MaxDirectMemorySize=20g"

COPY target/application-1.0-SNAPSHOT.war /var/lib/jetty/webapps/ROOT.war
