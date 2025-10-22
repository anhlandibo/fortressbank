FROM eclipse-temurin:17-jdk-focal as builder
WORKDIR /app
# Copy parent POM first
COPY pom.xml .
COPY shared-kernel shared-kernel/
COPY notification-service notification-service/
WORKDIR /app/notification-service
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests -f /app/pom.xml -pl :notification-service -am

FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=builder /app/notification-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
