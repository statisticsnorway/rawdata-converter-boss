FROM adoptopenjdk/openjdk14-openj9:alpine-slim

RUN apk --no-cache add curl

COPY target/rawdata-converter-*.jar boss.jar

EXPOSE 8080

CMD java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dcom.sun.management.jmxremote --enable-preview -jar boss.jar
