#!/bin/bash
echo "Starting Eureka Server..."
(cd EurekaServer && ./mvnw spring-boot:run > ../eureka.log 2>&1) &
sleep 15

echo "Starting Config Server..."
(cd Config-Server && ./mvnw spring-boot:run > ../config.log 2>&1) &
sleep 15

services=("api-gateway" "AuthService" "user-service" "startup-service" "InvestmentService" "TeamService" "MessagingService" "NotificationService" "PaymentService")

for service in "${services[@]}"; do
  echo "Starting $service..."
  (cd "$service" && ./mvnw spring-boot:run > "../$service.log" 2>&1) &
done

echo "All services started in background."
