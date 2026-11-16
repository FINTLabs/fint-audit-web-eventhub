FROM gradle:6.8.3-jdk11 as builder
COPY . .
RUN gradle --no-daemon build

FROM eclipse-temurin:11-jre
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-audit-web-eventhub-*.jar /data/fint-audit-web-eventhub.jar
ENTRYPOINT ["java", "-jar", "/data/fint-audit-web-eventhub.jar"]
