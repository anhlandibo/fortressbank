FROM eclipse-temurin:21-jdk as maven-base
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY . .
RUN mvn install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=maven-base /app/config-server/target/*.jar app.jar
COPY --from=maven-base /app/config-server/src/main/resources/configurations ./configurations
ENTRYPOINT ["java", "-jar", "app.jar"]