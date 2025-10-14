FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .

RUN mvn clean install -Dmaven.test.skip=true

FROM openjdk:21-jdk AS runner

WORKDIR /app

COPY --from=builder /app/user-service/target/*.jar app.jar

EXPOSE 4000

ENTRYPOINT ["java", "-jar", "app.jar"]