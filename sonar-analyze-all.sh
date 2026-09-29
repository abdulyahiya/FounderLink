#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  FounderLink — Run SonarQube analysis on all services
#  Usage: ./sonar-analyze-all.sh <your-sonar-token>
# ─────────────────────────────────────────────────────────────

SONAR_TOKEN=$1
SONAR_HOST=http://localhost:9000

if [ -z "$SONAR_TOKEN" ]; then
  echo "Usage: ./sonar-analyze-all.sh <your-sonar-token>"
  exit 1
fi

# Services with unit tests (JaCoCo enabled, tests will run)
SERVICES_WITH_TESTS=(
  "AuthService"
  "user-service"
  "startup-service"
  "InvestmentService"
  "TeamService"
  "MessagingService"
  "NotificationService"
)

SERVICES=(
  "AuthService:founderlink-auth-service:FounderLink Auth Service"
  "user-service:founderlink-user-service:FounderLink User Service"
  "startup-service:founderlink-startup-service:FounderLink Startup Service"
  "InvestmentService:founderlink-investment-service:FounderLink Investment Service"
  "TeamService:founderlink-team-service:FounderLink Team Service"
  "MessagingService:founderlink-messaging-service:FounderLink Messaging Service"
  "NotificationService:founderlink-notification-service:FounderLink Notification Service"
  "PaymentService:founderlink-payment-service:FounderLink Payment Service"
  "api-gateway:founderlink-api-gateway:FounderLink API Gateway"
  "Config-Server:founderlink-config-server:FounderLink Config Server"
  "EurekaServer:founderlink-eureka-server:FounderLink Eureka Server"
)

FAILED=()

for entry in "${SERVICES[@]}"; do
  DIR=$(echo $entry | cut -d: -f1)
  KEY=$(echo $entry | cut -d: -f2)
  NAME=$(echo $entry | cut -d: -f3)

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  Analysing: $NAME"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  # Services with unit tests — run tests to generate JaCoCo coverage report
  SERVICES_WITH_TESTS="AuthService user-service startup-service InvestmentService TeamService MessagingService NotificationService PaymentService"
  if echo "$SERVICES_WITH_TESTS" | grep -qw "$DIR"; then
    SKIP_TESTS=""
  else
    SKIP_TESTS="-DskipTests"
  fi

  mvn -B clean verify sonar:sonar $SKIP_TESTS \
    -Dsonar.projectKey=$KEY \
    -Dsonar.projectName="$NAME" \
    -Dsonar.host.url=$SONAR_HOST \
    -Dsonar.token=$SONAR_TOKEN \
    -f $DIR/pom.xml

  if [ $? -ne 0 ]; then
    echo "  ✗ FAILED: $NAME"
    FAILED+=("$NAME")
  else
    echo "  ✓ DONE: $NAME"
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [ ${#FAILED[@]} -eq 0 ]; then
  echo "  All services analysed successfully!"
  echo "  View results at: http://localhost:9000"
else
  echo "  The following services failed:"
  for f in "${FAILED[@]}"; do
    echo "    - $f"
  done
fi
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"