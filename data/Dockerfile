FROM openjdk:11-jre

COPY target/data-1.0-SNAPSHOT.jar /usr/share/stocks/db.jar

ENTRYPOINT ["/usr/bin/java", \
    "--add-modules", \
    "java.xml.bind,java.activation", \
    "-jar", "/usr/share/stocks/db.jar"]
