# ── Build Stage ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy all POM files for dependency caching
COPY pom.xml .
COPY EurekaServer/pom.xml ./EurekaServer/
COPY Config-Server/pom.xml ./Config-Server/
COPY api-gateway/pom.xml ./api-gateway/
COPY AuthService/pom.xml ./AuthService/
COPY user-service/pom.xml ./user-service/
COPY startup-service/pom.xml ./startup-service/
COPY InvestmentService/pom.xml ./InvestmentService/
COPY TeamService/pom.xml ./TeamService/
COPY MessagingService/pom.xml ./MessagingService/
COPY NotificationService/pom.xml ./NotificationService/
COPY PaymentService/pom.xml ./PaymentService/
COPY founderlink-server/pom.xml ./founderlink-server/

# Copy all source directories
COPY EurekaServer/src ./EurekaServer/src
COPY Config-Server/src ./Config-Server/src
COPY api-gateway/src ./api-gateway/src
COPY AuthService/src ./AuthService/src
COPY user-service/src ./user-service/src
COPY startup-service/src ./startup-service/src
COPY InvestmentService/src ./InvestmentService/src
COPY TeamService/src ./TeamService/src
COPY MessagingService/src ./MessagingService/src
COPY NotificationService/src ./NotificationService/src
COPY PaymentService/src ./PaymentService/src
COPY founderlink-server/src ./founderlink-server/src

# Compile and package unified founderlink-server
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl founderlink-server -am -q

# ── Production Runtime Stage ──────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install curl for health checking
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy built unified server JAR
COPY --from=build /app/founderlink-server/target/*.jar /app/founderlink-server.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-Xms64m", "-Xmx256m", "-jar", "/app/founderlink-server.jar"]
