#!/bin/bash
set -e

echo "=========================================="
echo " Starting FounderLink Cloud Production Stack"
echo "=========================================="

# Parse DATABASE_URL if available
if [ -n "$DATABASE_URL" ]; then
    echo "[Init] Configuring PostgreSQL from DATABASE_URL..."
    # Format: postgresql://USER:PASS@HOST:PORT/DB?PARAMS or postgres://...
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

# JVM Tuning for 512MB RAM multi-service container
JVM_OPTS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xms32m -Xmx48m"
GATEWAY_JVM_OPTS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xms48m -Xmx64m"

# 1. Start Eureka Server
echo "[1/10] Starting Eureka Discovery Server..."
java $JVM_OPTS -jar /app/EurekaServer.jar > /tmp/eureka.log 2>&1 &
EUREKA_PID=$!

# Wait for Eureka to be healthy
echo "[Init] Waiting for Eureka Server to bind on port 8761..."
for i in $(seq 1 30); do
    if curl -s http://127.0.0.1:8761/actuator/health | grep -q "UP"; then
        echo "[Init] Eureka Server is UP!"
        break
    fi
    sleep 2
done

# 2. Start Auth Service
echo "[2/10] Starting AuthService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_auth") \
java $JVM_OPTS -jar /app/AuthService.jar > /tmp/auth.log 2>&1 &

# 3. Start User Service
echo "[3/10] Starting UserService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_db") \
java $JVM_OPTS -jar /app/user-service.jar > /tmp/user.log 2>&1 &

# 4. Start Startup Service
echo "[4/10] Starting StartupService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_startups") \
java $JVM_OPTS -jar /app/startup-service.jar > /tmp/startup.log 2>&1 &

# 5. Start Investment Service
echo "[5/10] Starting InvestmentService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_investments") \
java $JVM_OPTS -jar /app/InvestmentService.jar > /tmp/investment.log 2>&1 &

# 6. Start Team Service
echo "[6/10] Starting TeamService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_teams") \
java $JVM_OPTS -jar /app/TeamService.jar > /tmp/team.log 2>&1 &

# 7. Start Messaging Service
echo "[7/10] Starting MessagingService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_messages") \
java $JVM_OPTS -jar /app/MessagingService.jar > /tmp/messaging.log 2>&1 &

# 8. Start Notification Service
echo "[8/10] Starting NotificationService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_notifications") \
java $JVM_OPTS -jar /app/NotificationService.jar > /tmp/notification.log 2>&1 &

# 9. Start Payment Service
echo "[9/10] Starting PaymentService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "paymentdb") \
java $JVM_OPTS -jar /app/PaymentService.jar > /tmp/payment.log 2>&1 &

# Give services a brief moment to register with Eureka
sleep 5

# 10. Start API Gateway in Foreground (Binds public port 8080)
echo "[10/10] Starting API Gateway on Port 8080..."
exec java $GATEWAY_JVM_OPTS -jar /app/api-gateway.jar
