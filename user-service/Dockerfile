FROM eclipse-temurin:17-jdk-focal as builder
WORKDIR /app
# Copy parent POM first
COPY pom.xml .
COPY shared-kernel shared-kernel/
COPY user-service user-service/
WORKDIR /app/user-service
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests -f /app/pom.xml -pl :user-service -am

FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=builder /app/user-service/target/*.jar app.jar
EXPOSE 4000
ENTRYPOINT ["java", "-jar", "app.jar"]
