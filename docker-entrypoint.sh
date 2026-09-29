#!/bin/bash
set -e

echo "=========================================="
echo " Starting FounderLink Cloud Production Stack"
echo "=========================================="

# Parse DATABASE_URL if available
if [ -n "$DATABASE_URL" ]; then
    echo "[Init] Configuring PostgreSQL from DATABASE_URL..."
    CLEAN_URL=$(echo "$DATABASE_URL" | sed -e 's/^postgresql:\/\///' -e 's/^postgres:\/\///')
    
    DB_USER_PASS=$(echo "$CLEAN_URL" | cut -d'@' -f1)
    export DB_USERNAME=${DB_USERNAME:-$(echo "$DB_USER_PASS" | cut -d':' -f1)}
    export DB_PASSWORD=${DB_PASSWORD:-$(echo "$DB_USER_PASS" | cut -d':' -f2)}
    
    DB_HOST_PORT_DB=$(echo "$CLEAN_URL" | cut -d'@' -f2)
    export DB_HOST=${DB_HOST:-$(echo "$DB_HOST_PORT_DB" | cut -d'/' -f1 | cut -d':' -f1)}
    export DB_PORT=${DB_PORT:-$(echo "$DB_HOST_PORT_DB" | cut -d'/' -f1 | cut -d':' -f2 -s)}
    export DB_PORT=${DB_PORT:-5432}
fi

export DB_HOST=${DB_HOST:-localhost}
export DB_PORT=${DB_PORT:-5432}
export DB_USERNAME=${DB_USERNAME:-neondb_owner}
export DB_PASSWORD=${DB_PASSWORD:-npg_xvnhi6kWEa3U}
export EUREKA_SERVER_HOST="127.0.0.1"

echo "[Init] Target DB Host: $DB_HOST (Port: $DB_PORT)"

# Helper function to construct JDBC URL
get_jdbc_url() {
    local db_name=$1
    if [ "$DB_HOST" != "localhost" ] && [ "$DB_HOST" != "127.0.0.1" ]; then
        echo "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${db_name}?sslmode=require"
    else
        echo "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${db_name}"
    fi
}

# Ultra-fast JVM Tuning for 512MB RAM multi-service container
FAST_JVM_OPTS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss256k -Xms16m -Xmx40m"
GATEWAY_JVM_OPTS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss256k -Xms32m -Xmx64m"

# 1. Start Eureka Server in background
echo "[1/10] Starting Eureka Discovery Server on port 8761..."
java $FAST_JVM_OPTS -jar /app/EurekaServer.jar > /tmp/eureka.log 2>&1 &

# 2. Start API Gateway IMMEDIATELY so Render's port scanner detects port 8080 right away
echo "[2/10] Starting API Gateway on Port 8080..."
java $GATEWAY_JVM_OPTS -jar /app/api-gateway.jar > /tmp/gateway.log 2>&1 &
GATEWAY_PID=$!

# Tail gateway log to stdout so Render logs show Gateway activity
tail -f /tmp/gateway.log &

# 3. Start Core Business Services with slight stagger to avoid CPU spikes
sleep 3
echo "[3/10] Starting AuthService (registration & login)..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_auth") \
java $FAST_JVM_OPTS -jar /app/AuthService.jar > /tmp/auth.log 2>&1 &

sleep 2
echo "[4/10] Starting UserService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_db") \
java $FAST_JVM_OPTS -jar /app/user-service.jar > /tmp/user.log 2>&1 &

sleep 2
echo "[5/10] Starting StartupService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_startups") \
java $FAST_JVM_OPTS -jar /app/startup-service.jar > /tmp/startup.log 2>&1 &

sleep 2
echo "[6/10] Starting InvestmentService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_investments") \
java $FAST_JVM_OPTS -jar /app/InvestmentService.jar > /tmp/investment.log 2>&1 &

sleep 2
echo "[7/10] Starting TeamService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_teams") \
java $FAST_JVM_OPTS -jar /app/TeamService.jar > /tmp/team.log 2>&1 &

sleep 2
echo "[8/10] Starting MessagingService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_messages") \
java $FAST_JVM_OPTS -jar /app/MessagingService.jar > /tmp/messaging.log 2>&1 &

sleep 2
echo "[9/10] Starting NotificationService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_notifications") \
java $FAST_JVM_OPTS -jar /app/NotificationService.jar > /tmp/notification.log 2>&1 &

sleep 2
echo "[10/10] Starting PaymentService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "paymentdb") \
java $FAST_JVM_OPTS -jar /app/PaymentService.jar > /tmp/payment.log 2>&1 &

echo "[Ready] All 10 FounderLink microservices are initializing and registering with Eureka!"

# Keep container alive by waiting for Gateway process
wait $GATEWAY_PID
