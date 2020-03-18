FROM gradle:6.2.2-jdk8 as builder
COPY . .
RUN gradle build

FROM gcr.io/distroless/java:8
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-audit-web-eventhub-*.jar /data/fint-audit-web-eventhub.jar
CMD ["/data/fint-audit-web-eventhub.jar"]
