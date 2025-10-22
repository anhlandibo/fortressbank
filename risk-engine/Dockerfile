FROM eclipse-temurin:21-jdk as maven-base
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Copy all modules first to satisfy Maven's multi-module build
COPY . .
RUN mvn install -DskipTests

FROM eclipse-temurin:21-jre as risk-engine
WORKDIR /app
COPY --from=maven-base /app/risk-engine/target/*.jar app.jar
EXPOSE 6000
ENTRYPOINT ["java", "-jar", "app.jar"]
