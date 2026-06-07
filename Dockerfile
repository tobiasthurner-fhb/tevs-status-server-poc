FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -q test package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/status-server-1.0.0.jar app.jar
COPY src/main/resources/certs/status-server.p12 /app/certs/status-server.p12
COPY src/main/resources/certs/status-server-truststore.p12 /app/certs/status-server-truststore.p12

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]
