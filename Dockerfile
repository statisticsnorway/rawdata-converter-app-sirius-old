FROM openjdk:14-alpine

RUN apk --no-cache add curl

COPY target/rawdata-converter-*.jar rawdata-converter.jar
COPY target/classes/logback*.xml /conf/

EXPOSE 8080

CMD java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} --enable-preview -jar rawdata-converter.jar
