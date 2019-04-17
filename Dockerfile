FROM alpine:edge
MAINTAINER dastec.one
RUN apk add --no-cache openjdk8

ENV JAR templogger-0.0.1-SNAPSHOT.jar
ENV TARGET target/$JAR
ENV INSTDIR /opt/templogger
ENV JARPATH $INSTDIR/$TARGET

CMD mkdir $INSTDIR
COPY $TARGET $INSTDIR/


ENTRYPOINT ["/usr/bin/java"]
CMD ["-jar", "/opt/templogger/templogger-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080