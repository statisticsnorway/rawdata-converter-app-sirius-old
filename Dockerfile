FROM adoptopenjdk/openjdk11-openj9:jdk-11.0.1.13-alpine-slim

COPY target/rawdata-converter-*.jar rawdata-converter.jar
COPY target/classes/logback*.xml /conf/

EXPOSE 8080

CMD java -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar rawdata-converter.jar
