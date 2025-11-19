FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /build

# Install Maven
RUN apk add --no-cache maven

# Copy parent POM first (for dependency resolution)
COPY pom.xml ./pom.xml

# Copy shared-kernel POM and source
COPY shared-kernel/pom.xml ./shared-kernel/pom.xml
COPY shared-kernel/src ./shared-kernel/src

# Copy transaction-service POM (for dependency caching)
COPY transaction-service/pom.xml ./transaction-service/pom.xml

# Download dependencies (cached if POM unchanged)
WORKDIR /build/transaction-service
RUN mvn dependency:go-offline -DskipTests

# Build shared-kernel first
WORKDIR /build/shared-kernel
RUN mvn clean install -DskipTests

# Copy transaction-service source and build
WORKDIR /build/transaction-service
COPY transaction-service/src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/transaction-service/target/*.jar app.jar
EXPOSE 4003
ENTRYPOINT ["java", "-jar", "app.jar"]
