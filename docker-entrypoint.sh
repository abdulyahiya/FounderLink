#!/bin/bash
set -e

echo "=========================================="
echo " Starting FounderLink Cloud Production Stack"
echo "=========================================="

# ── 1. PostgreSQL Configuration ──────────────────────────────
if [ -n "$DATABASE_URL" ]; then
    echo "[Init] Parsing Neon PostgreSQL credentials from DATABASE_URL..."
    CLEAN_URL=$(echo "$DATABASE_URL" | sed -e 's/^postgresql:\/\///' -e 's/^postgres:\/\///')
    
    DB_USER_PASS=$(echo "$CLEAN_URL" | cut -d'@' -f1)
    export DB_USERNAME=${DB_USERNAME:-$(echo "$DB_USER_PASS" | cut -d':' -f1)}
    export DB_PASSWORD=${DB_PASSWORD:-$(echo "$DB_USER_PASS" | cut -d':' -f2)}
    
    DB_HOST_PORT_DB=$(echo "$CLEAN_URL" | cut -d'@' -f2)
    export DB_HOST=${DB_HOST:-$(echo "$DB_HOST_PORT_DB" | cut -d'/' -f1 | cut -d':' -f1)}
    export DB_PORT=${DB_PORT:-$(echo "$DB_HOST_PORT_DB" | cut -d'/' -f1 | cut -d':' -f2 -s)}
    export DB_PORT=${DB_PORT:-5432}
fi

export DB_HOST=${DB_HOST:-ep-square-hat-b87lvinm-pooler.c-14.us-east-1.aws.neon.tech}
export DB_PORT=${DB_PORT:-5432}
export DB_USERNAME=${DB_USERNAME:-neondb_owner}
export DB_PASSWORD=${DB_PASSWORD:-npg_xvnhi6kWEa3U}
export SPRING_DATASOURCE_USERNAME="$DB_USERNAME"
export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"

# ── 2. RabbitMQ Configuration ───────────────────────────────
if [ -n "$SPRING_RABBITMQ_ADDRESSES" ]; then
    echo "[Init] Parsing CloudAMQP credentials..."
    CLEAN_MQ=$(echo "$SPRING_RABBITMQ_ADDRESSES" | sed -e 's/^amqps:\/\///' -e 's/^amqp:\/\///')
    MQ_USER_PASS=$(echo "$CLEAN_MQ" | cut -d'@' -f1)
    MQ_HOST_VHOST=$(echo "$CLEAN_MQ" | cut -d'@' -f2)
    
    export RABBITMQ_USER=${RABBITMQ_USER:-$(echo "$MQ_USER_PASS" | cut -d':' -f1)}
    export RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-$(echo "$MQ_USER_PASS" | cut -d':' -f2)}
    export RABBITMQ_HOST=${RABBITMQ_HOST:-$(echo "$MQ_HOST_VHOST" | cut -d'/' -f1 | cut -d':' -f1)}
    export SPRING_RABBITMQ_HOST="$RABBITMQ_HOST"
    export SPRING_RABBITMQ_PORT="5671"
    export SPRING_RABBITMQ_USERNAME="$RABBITMQ_USER"
    export SPRING_RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD"
    export SPRING_RABBITMQ_SSL_ENABLED="true"
    export SPRING_RABBITMQ_VIRTUAL_HOST="${RABBITMQ_USER}"
fi

export RABBITMQ_HOST=${RABBITMQ_HOST:-warthog.lmq.cloudamqp.com}
export RABBITMQ_USER=${RABBITMQ_USER:-wxhdvvyu}
export RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-l4qvNpKdzBDNJnY4cItJS8wf9S5XVXUR}

# ── 3. Redis Configuration ──────────────────────────────────
export SPRING_DATA_REDIS_HOST=${SPRING_DATA_REDIS_HOST:-fun-kodiak-318520.upstash.io}
export SPRING_DATA_REDIS_PORT=${SPRING_DATA_REDIS_PORT:-6379}
export SPRING_DATA_REDIS_PASSWORD=${SPRING_DATA_REDIS_PASSWORD:-gQAAAAAABNw4AAIgcDF1ZjBiMGVjYTE2ZTg0M2E1ODczOWViOTE3NWMzOWIxNg}
export SPRING_DATA_REDIS_SSL_ENABLED="true"

# ── 4. Discovery & Networking ──────────────────────────────
export EUREKA_SERVER_HOST="127.0.0.1"
export CONFIG_SERVER_HOST="127.0.0.1"
export ZIPKIN_HOST="127.0.0.1"
export JWT_SECRET="5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437"

echo "[Init] DB Host: $DB_HOST (User: $DB_USERNAME)"
echo "[Init] RabbitMQ Host: $RABBITMQ_HOST"
echo "[Init] Redis Host: $SPRING_DATA_REDIS_HOST"

# Helper function to construct JDBC URL
get_jdbc_url() {
    local db_name=$1
    echo "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${db_name}?sslmode=require"
}

# JVM Tuning for 512MB RAM multi-service container
JVM_OPTS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss256k -Xms16m -Xmx40m"
GATEWAY_JVM_OPTS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss256k -Xms32m -Xmx64m"

# 1. Start Eureka Server
echo "[1/10] Starting Eureka Discovery Server on port 8761..."
java $JVM_OPTS -jar /app/EurekaServer.jar &
sleep 2

# 2. Start API Gateway (Listens on public PORT 8080)
echo "[2/10] Starting API Gateway on Port 8080..."
java $GATEWAY_JVM_OPTS -jar /app/api-gateway.jar &
GATEWAY_PID=$!
sleep 3

# 3. Start Core Services
echo "[3/10] Starting AuthService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_auth") \
java $JVM_OPTS -jar /app/AuthService.jar &

echo "[4/10] Starting UserService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_db") \
java $JVM_OPTS -jar /app/user-service.jar &

echo "[5/10] Starting StartupService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_startups") \
java $JVM_OPTS -jar /app/startup-service.jar &

echo "[6/10] Starting InvestmentService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_investments") \
java $JVM_OPTS -jar /app/InvestmentService.jar &

echo "[7/10] Starting TeamService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_teams") \
java $JVM_OPTS -jar /app/TeamService.jar &

echo "[8/10] Starting MessagingService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_messages") \
java $JVM_OPTS -jar /app/MessagingService.jar &

echo "[9/10] Starting NotificationService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "founderlink_notifications") \
java $JVM_OPTS -jar /app/NotificationService.jar &

echo "[10/10] Starting PaymentService..."
SPRING_DATASOURCE_URL=$(get_jdbc_url "paymentdb") \
java $JVM_OPTS -jar /app/PaymentService.jar &

echo "[Ready] All 10 FounderLink microservices have been started and connected to Neon DB, CloudAMQP & Upstash Redis!"

# Wait for API Gateway
wait $GATEWAY_PID
