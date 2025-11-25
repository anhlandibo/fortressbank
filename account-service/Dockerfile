FROM eclipse-temurin:21-jdk-focal as builder
WORKDIR /app
# Copy parent POM first
COPY pom.xml .
COPY shared-kernel shared-kernel/
COPY account-service account-service/
WORKDIR /app/account-service
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests -f /app/pom.xml -pl :account-service -am

FROM eclipse-temurin:21-jre-focal
WORKDIR /app
COPY --from=builder /app/account-service/target/*.jar app.jar
EXPOSE 4001
ENTRYPOINT ["java", "-jar", "app.jar"]
