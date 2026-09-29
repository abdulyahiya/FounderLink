# ── Build Stage ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy POMs for all modules
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

# Copy source code for all modules
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

# Compile and package all JARs
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -q

# ── Production Runtime Stage ──────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install curl for health checking
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy built JAR artifacts
COPY --from=build /app/EurekaServer/target/*.jar /app/EurekaServer.jar
COPY --from=build /app/api-gateway/target/*.jar /app/api-gateway.jar
COPY --from=build /app/AuthService/target/*.jar /app/AuthService.jar
COPY --from=build /app/user-service/target/*.jar /app/user-service.jar
COPY --from=build /app/startup-service/target/*.jar /app/startup-service.jar
COPY --from=build /app/InvestmentService/target/*.jar /app/InvestmentService.jar
COPY --from=build /app/TeamService/target/*.jar /app/TeamService.jar
COPY --from=build /app/MessagingService/target/*.jar /app/MessagingService.jar
COPY --from=build /app/NotificationService/target/*.jar /app/NotificationService.jar
COPY --from=build /app/PaymentService/target/*.jar /app/PaymentService.jar

# Copy and setup entrypoint
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
