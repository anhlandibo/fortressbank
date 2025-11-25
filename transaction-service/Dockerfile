FROM maven:3.9-eclipse-temurin-21-alpine as builder
WORKDIR /build

# Copy parent POM and install to local repo
COPY pom.xml ./
RUN mvn -N install -DskipTests

# Copy and build shared-kernel
COPY shared-kernel/pom.xml ./shared-kernel/
COPY shared-kernel/src ./shared-kernel/src
WORKDIR /build/shared-kernel
RUN mvn clean install -DskipTests

# Copy transaction-service POM and download dependencies
WORKDIR /build/transaction-service
COPY transaction-service/pom.xml ./
RUN mvn dependency:go-offline -DskipTests || true

# Copy source and build
COPY transaction-service/src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/transaction-service/target/*.jar app.jar
EXPOSE 4003
ENTRYPOINT ["java", "-jar", "app.jar"]
