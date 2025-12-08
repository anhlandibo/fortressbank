FROM eclipse-temurin:17-jdk-focal as builder
WORKDIR /app
# Copy parent POM first
COPY pom.xml .
COPY shared-kernel shared-kernel/
COPY notification-service notification-service/
WORKDIR /app/account-service
RUN mvn clean package -DskipTests -f /app/pom.xml -pl :account-service -am

FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=builder /app/account-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]