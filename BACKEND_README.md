# FounderLink — Backend Architecture & Codebase Guide

> This document explains every microservice, every design decision, and every advanced concept used in the FounderLink backend — written to help you understand and confidently explain the codebase in an interview.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Architecture Diagram](#2-architecture-diagram)
3. [Infrastructure Services](#3-infrastructure-services)
   - [Config Server](#31-config-server-port-8888)
   - [Eureka Server](#32-eureka-server-port-8761)
4. [API Gateway](#4-api-gateway-port-8080)
   - [JWT Authentication Filter](#41-jwt-authentication-filter)
   - [CORS Configuration](#42-cors-configuration)
   - [Route Configuration](#43-route-configuration)
5. [Microservices](#5-microservices)
   - [Auth Service](#51-auth-service-port-8081)
   - [User Service](#52-user-service-port-8082)
   - [Startup Service](#53-startup-service-port-8083)
   - [Investment Service](#54-investment-service-port-8084)
   - [Team Service](#55-team-service-port-8085)
   - [Messaging Service](#56-messaging-service-port-8086)
   - [Notification Service](#57-notification-service-port-8087)
6. [Cross-Cutting Concerns](#6-cross-cutting-concerns)
   - [RabbitMQ Event-Driven Architecture](#61-rabbitmq-event-driven-architecture)
   - [Redis Caching](#62-redis-caching)
   - [OpenFeign + Circuit Breaker](#63-openfeign--circuit-breaker)
   - [AOP Logging](#64-aop-logging)
   - [Global Exception Handling](#65-global-exception-handling)
   - [JWT Security Model](#66-jwt-security-model)
7. [Database Design](#7-database-design)
8. [Port Reference](#8-port-reference)
9. [Interview Q&A Cheatsheet](#9-interview-qa-cheatsheet)

---

## 1. System Overview

FounderLink is a **microservices-based** platform that connects startup founders, investors, and co-founders. The backend is built with **Spring Boot** and follows a cloud-native architecture using:

- **Spring Cloud Gateway** — single entry point for all API calls
- **Spring Cloud Netflix Eureka** — service discovery so services find each other dynamically
- **Spring Cloud Config Server** — centralized configuration management
- **RabbitMQ** — asynchronous event-driven communication between services
- **Redis** — distributed caching to reduce database load
- **OpenFeign** — declarative HTTP client for inter-service calls
- **JWT (JSON Web Tokens)** — stateless authentication
- **PostgreSQL** — relational database (each service has its own schema)
- **Spring AOP** — cross-cutting concerns like logging
- **Swagger/OpenAPI** — auto-generated API documentation for each service

---

## 2. Architecture Diagram

```
                          ┌─────────────────────────────────────────┐
                          │           React Frontend (3000)          │
                          └──────────────────┬──────────────────────┘
                                             │ HTTP
                          ┌──────────────────▼──────────────────────┐
                          │         API Gateway (8080)               │
                          │   JWT Filter → Route → Load Balance      │
                          └──┬──────┬──────┬──────┬──────┬─────┬───┘
                             │      │      │      │      │     │
                    ┌────────▼┐ ┌───▼──┐ ┌▼────┐ ┌▼───┐ ┌▼──┐ ┌▼────┐
                    │  Auth   │ │ User │ │Start│ │Inv.│ │Team│ │Msg  │
                    │  8081   │ │ 8082 │ │8083 │ │8084│ │8085│ │8086 │
                    └─────────┘ └──────┘ └──┬──┘ └──┬─┘ └──┬─┘ └──┬──┘
                                             │       │      │      │
                    ┌────────────────────────▼───────▼──────▼──────▼──┐
                    │              RabbitMQ Message Broker              │
                    └────────────────────────┬──────────────────────────┘
                                             │ Events consumed
                                    ┌────────▼────────┐
                                    │  Notification   │
                                    │    Service 8087 │
                                    └─────────────────┘

       ┌─────────────────┐           ┌──────────────────────┐
       │  Config Server  │◄──────────│  All services pull   │
       │     8888        │           │  config on startup   │
       └─────────────────┘           └──────────────────────┘

       ┌─────────────────┐           ┌──────────────────────┐
       │  Eureka Server  │◄──────────│  All services        │
       │     8761        │           │  register & discover │
       └─────────────────┘           └──────────────────────┘
```

---

## 3. Infrastructure Services

### 3.1 Config Server (Port 8888)

**What it does:**
The Config Server is a centralized place to store configuration for all microservices. Instead of each service having its own hardcoded config, they all pull their configuration from this server at startup.

**Why we built it:**
In a microservices system, you may have 8–10 services. If you hardcode the database URL or JWT secret in each one, changing that secret means redeploying all 8 services. Config Server lets you change it in one place.

**Key concept — `optional:configserver:`**
Every service has this in its `application.yml`:
```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888
```
The `optional:` prefix means — if the Config Server is not reachable, don't crash; fall back to local `application.yml`. This is important for local development.

**How it works:**
1. Config Server starts and exposes configuration at `http://localhost:8888/{service-name}/default`
2. Each service sends a request to Config Server on startup
3. Config Server returns the properties for that service
4. Service uses those properties to configure itself

---

### 3.2 Eureka Server (Port 8761)

**What it does:**
Eureka is a **Service Registry and Discovery** server. Every microservice registers itself with Eureka when it starts up, and uses Eureka to find other services by name instead of hardcoded IP addresses.

**Why we built it:**
In a microservices system, services need to call each other. You can't hardcode `http://localhost:8083` because in production, the service might be running on a different machine or multiple instances. Eureka solves this by letting services say "I am `startup-service`" and letting other services say "give me the address of `startup-service`."

**Key configuration:**
```yaml
server:
  port: 8761
eureka:
  client:
    register-with-eureka: false   # Server doesn't register itself
    fetch-registry: false         # Server doesn't need to fetch others
  server:
    enable-self-preservation: false  # In dev, disable false-positive warnings
```

**How every other service registers:**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

**The `lb://` prefix in Gateway:**
When the gateway routes say `uri: lb://STARTUP-SERVICE`, the `lb://` prefix tells Spring Cloud to use Eureka to look up a service named `STARTUP-SERVICE` and load-balance across its instances.

---

## 4. API Gateway (Port 8080)

The API Gateway is the **single entry point** for all client requests. The frontend never talks directly to individual microservices — every request goes through the gateway.

**Why a gateway?**
- **Single point of security** — JWT is validated once here, not in every service
- **Routing** — directs requests to the correct microservice
- **CORS handling** — configured once for the entire platform
- **Load balancing** — distributes traffic across multiple instances of a service

### 4.1 JWT Authentication Filter

**File:** `filter/JwtAuthenticationFilter.java`

This is the most important class in the gateway. Here's exactly what it does step-by-step:

```
Client Request
     │
     ▼
Is path /auth/register, /auth/login, or /auth/refresh?
     │
     ├── YES → Forward directly (no auth needed)
     │
     └── NO → Is there an Authorization: Bearer <token> header?
                    │
                    ├── NO → Return 401 Unauthorized
                    │
                    └── YES → Is the token valid and not expired?
                                   │
                                   ├── NO → Return 401 Unauthorized
                                   │
                                   └── YES → Extract userId and roles
                                              → Add X-User-Id header
                                              → Add X-User-Roles header
                                              → Forward request to service
```

**Why we pass headers instead of the token:**
Each downstream microservice needs to know WHO is making the request. Instead of each service decoding the JWT again (which would require the JWT secret in every service), the gateway decodes it ONCE and puts the userId and roles in custom headers. The downstream services just read `X-User-Id` and `X-User-Roles`.

**Code explanation:**
```java
// 1. Check if route is public (no auth needed)
if (isPublicPath(path)) return chain.filter(exchange);

// 2. Extract Bearer token from Authorization header
String token = authHeader.substring(7); // Remove "Bearer "

// 3. Validate the token using the JWT secret
if (!jwtUtil.validateToken(token)) → return 401

// 4. Extract claims and add as headers
String userId = jwtUtil.extractUserId(token);
String roles  = jwtUtil.extractRoles(token);

// 5. Add headers so downstream services know the user
exchange.getRequest().mutate()
    .header("X-User-Id",    userId)
    .header("X-User-Roles", roles)
```

**Why services still have their own SecurityConfig:**
Even though the gateway validates the JWT, each service also has its own `JwtAuthenticationFilter` that reads the `X-User-Id` and `X-User-Roles` headers and builds a Spring Security `Authentication` object. This allows `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` to work in individual service controllers.

### 4.2 CORS Configuration

**File:** `config/CorsConfig.java`

CORS (Cross-Origin Resource Sharing) is a browser security mechanism. When your React app on `localhost:3000` calls `localhost:8080`, the browser blocks it by default because they are on different ports (different origins).

```java
config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);  // Required to allow cookies/auth headers
config.setMaxAge(3600L);           // Browser caches preflight for 1 hour
```

**Why `CorsWebFilter` instead of `@CrossOrigin`:**
The gateway uses **Spring WebFlux** (reactive/non-blocking), not the traditional Spring MVC. So we use `CorsWebFilter` (reactive version) instead of `@CrossOrigin` (MVC version). Using the wrong one would cause CORS to silently not work.

### 4.3 Route Configuration

**File:** `application.yml`

Each route follows this pattern:
```yaml
- id: startup-service
  uri: lb://STARTUP-SERVICE        # lb:// = load balanced via Eureka
  predicates:
    - Path=/startups/**            # Any URL starting with /startups goes here
  filters:
    - name: JwtAuthenticationFilter  # Apply JWT validation
```

**Route table:**

| Route ID | Path Pattern | Service Name | Port |
|---|---|---|---|
| auth-service | `/auth/**` | AUTH-SERVICE | 8081 |
| user-service | `/users/**` | USER-SERVICE | 8082 |
| startup-service | `/startups/**` | STARTUP-SERVICE | 8083 |
| investment-service | `/investments/**` | INVESTMENT-SERVICE | 8084 |
| team-service | `/teams/**` | TEAM-SERVICE | 8085 |
| messaging-service | `/messages/**`, `/conversations/**` | MESSAGING-SERVICE | 8086 |
| notification-service | `/notifications/**` | NOTIFICATION-SERVICE | 8087 |

---

## 5. Microservices

### 5.1 Auth Service (Port 8081)

**Responsibility:** User registration, login, JWT token generation, and token refresh.

**Key classes:**

| Class | Purpose |
|---|---|
| `AuthController` | REST endpoints: `/auth/register`, `/auth/login`, `/auth/refresh` |
| `AuthService` | Business logic — validate credentials, generate tokens |
| `JwtUtil` | Generate and validate JWT access tokens and refresh tokens |
| `SecurityConfig` | Permits all `/auth/**` endpoints without authentication |
| `DataSeeder` | Runs on startup to create default roles in DB if not present |
| `LoggingAspect` | AOP-based logging for all service method calls |
| `GlobalExceptionHandler` | Returns structured JSON error responses for all exceptions |

**Registration flow:**
```
POST /auth/register
  → Validate request (name, email, password, role)
  → Check if email already exists → throw 409 if yes
  → BCrypt encode the password
  → Look up the RoleEntity from the roles table
  → Save UserEntity with hashed password + role
  → Return 201 Created with user details
```

**Login flow:**
```
POST /auth/login
  → Load user by email → throw 404 if not found
  → BCrypt matches(inputPassword, storedHashedPassword) → throw 401 if mismatch
  → Generate JWT access token (contains userId, email, roles, expiry)
  → Generate refresh token (longer expiry, contains userId only)
  → Return both tokens + userId + role + email
```

**JWT Token structure:**
```
Header.Payload.Signature
       ↓
{
  "sub": "42",                    ← userId as subject
  "email": "user@example.com",
  "roles": ["ROLE_FOUNDER"],
  "iat": 1700000000,              ← issued at
  "exp": 1700086400               ← expires in 24 hours
}
```

**Why separate access and refresh tokens?**
Access tokens are short-lived (24h) so that if stolen, the damage window is small. Refresh tokens are long-lived and used ONLY to get a new access token — they are never sent to business endpoints.

**DataSeeder — why we built it:**
The roles table must have rows (`ROLE_FOUNDER`, `ROLE_INVESTOR`, etc.) before any user can register. The `DataSeeder` runs at startup using `@PostConstruct` and creates missing roles. This prevents a chicken-and-egg problem in fresh environments.

---

### 5.2 User Service (Port 8082)

**Responsibility:** Manages user profile data — name, bio, skills, profile picture, location.

**Key classes:**

| Class | Purpose |
|---|---|
| `UserProfileController` | REST endpoints for CRUD on user profiles |
| `UserProfileServiceImpl` | Business logic with Redis caching |
| `UserProfile` (Entity) | JPA entity mapped to `user_profiles` table |
| `RedisConfig` | Configures Redis with 10-min TTL and JSON serialization |

**Why a separate User Service from Auth Service?**
Auth Service only cares about credentials (email, password, role). User Service cares about profile data (bio, skills, location). Separating them follows the **Single Responsibility Principle** — each service owns one domain. Also, profile data changes frequently while credentials rarely change, so they scale independently.

**Caching in User Service:**
```java
@Cacheable(value = "userProfiles", key = "#userId")
public UserProfileResponse getProfile(Long userId) {
    // First call → hits database, stores result in Redis
    // Subsequent calls → returned directly from Redis (fast)
}

@CacheEvict(value = "userProfiles", key = "#userId")
public UserProfileResponse updateProfile(Long userId, ...) {
    // After update → evicts old cache so next read is fresh
}
```

**Why Redis for caching?**
Profile data is read far more often than it is written. Without caching, every profile view = one database query. With Redis, the same profile query is served from memory in under 1ms instead of 5–20ms from the database.

---

### 5.3 Startup Service (Port 8083)

**Responsibility:** Full lifecycle management of startups — create, update, delete, search, approve, and follow.

**Key classes:**

| Class | Purpose |
|---|---|
| `StartupController` | REST endpoints with role-based `@PreAuthorize` |
| `StartupServiceImpl` | Business logic, caching, event publishing |
| `Startup` (Entity) | JPA entity with all startup fields |
| `StartupRepository` | JPA repository with custom search queries |
| `RabbitMQConfig` | Declares queue, exchange, and binding for startup events |
| `RedisConfig` | Redis caching with 10-min TTL |

**Role-based access control (important for interview):**
```java
@PostMapping
@PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER')")
public ResponseEntity<StartupResponse> createStartup(Authentication auth, ...) {
    Long founderId = Long.parseLong(auth.getName());  // auth.getName() = X-User-Id header value
    ...
}

@GetMapping("/admin/all")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public ResponseEntity<Page<StartupResponse>> getAllStartupsForAdmin(...) { ... }
```

**How `Authentication` object is populated:**
The gateway adds `X-User-Id` and `X-User-Roles` headers. The service's own `JwtAuthenticationFilter` reads these headers and builds a Spring Security `UsernamePasswordAuthenticationToken`, which gets stored in the `SecurityContextHolder`. When `@PreAuthorize` evaluates, it reads authorities from this context.

**Caching in Startup Service:**
```java
@Cacheable(value = "approvedStartups", key = "#pageable")
public Page<StartupResponse> getAllApprovedStartups(Pageable pageable) { ... }

@CacheEvict(value = "approvedStartups", allEntries = true)
public StartupResponse createStartup(...) { ... }
```

**Event publishing (after startup creation):**
```java
// After saving startup to DB, publish event to RabbitMQ
rabbitTemplate.convertAndSend(exchange, routingKey, startupCreatedEvent);
```
This notifies the Notification Service asynchronously — the startup service doesn't wait for notifications to be sent.

**Approval flow:**
Startups are created with `isApproved = false`. Only `ROLE_ADMIN` can call `PUT /startups/{id}/approve` to set `isApproved = true`. The public `GET /startups` endpoint only returns approved startups. Admin uses `GET /startups/admin/all` to see everything including pending ones.

---

### 5.4 Investment Service (Port 8084)

**Responsibility:** Handles investment interest from investors in startups. Tracks investment amounts, status, and approvals.

**Key classes:**

| Class | Purpose |
|---|---|
| `InvestmentController` | REST endpoints for create/read/update investments |
| `InvestmentService` | Business logic for investment lifecycle |
| `StartupClient` | Feign client to call Startup Service |
| `StartupClientFallbackFactory` | Circuit breaker fallback when Startup Service is down |
| `EventPublisher` | Publishes `INVESTMENT_CREATED` and `INVESTMENT_APPROVED` events |
| `LoggingAspect` | AOP logging for all service methods |

**OpenFeign — inter-service communication:**
```java
@FeignClient(
    name = "startup-service",
    fallbackFactory = StartupClientFallbackFactory.class
)
public interface StartupClient {
    @GetMapping("/startups/{id}")
    StartupDTO getStartupById(@PathVariable Long id);
}
```

**Why Feign instead of RestTemplate?**
Feign is declarative — you write an interface, Spring generates the HTTP client automatically. It integrates with Eureka (resolves `startup-service` to actual URL) and supports fallbacks. RestTemplate requires you to manually build URLs and handle exceptions.

**Circuit Breaker pattern (fallback):**
```java
public class StartupClientFallbackFactory implements FallbackFactory<StartupClient> {
    @Override
    public StartupClient create(Throwable cause) {
        return id -> {
            throw new ServiceUnavailableException("Startup Service is currently unavailable");
        };
    }
}
```

**Why Circuit Breaker?**
Without it, if the Startup Service goes down, Investment Service would hang waiting for a response, consuming threads, and eventually crashing too (cascade failure). The circuit breaker "trips" after repeated failures and immediately returns an error response, protecting the caller.

**Events published by Investment Service:**
```
INVESTMENT_CREATED  → sent to RabbitMQ when investor expresses interest
INVESTMENT_APPROVED → sent to RabbitMQ when founder approves the investment
```

---

### 5.5 Team Service (Port 8085)

**Responsibility:** Manages team membership for startups — founders can invite co-founders, manage roles within the team.

**Key classes:**

| Class | Purpose |
|---|---|
| `TeamController` | REST endpoints for team management |
| `TeamService` | Business logic for invitations and membership |
| `TeamMember` (Entity) | JPA entity linking users to startups with roles |
| `EventPublisher` | Publishes `TEAM_INVITE_SENT` event to RabbitMQ |
| `RabbitMQConfig` | Declares team event queue and exchange binding |

**Event on team invite:**
When a founder invites someone to their team, an event is published:
```java
rabbitTemplate.convertAndSend(exchange, "team.invite", teamInviteEvent);
```
The Notification Service picks this up and creates a notification for the invited user.

---

### 5.6 Messaging Service (Port 8086)

**Responsibility:** Real-time conversations between users — founders talking to investors, co-founders communicating with the team.

**Key classes:**

| Class | Purpose |
|---|---|
| `MessageController` | REST endpoints for conversations and messages |
| `MessagingServiceImpl` | Business logic — find or create conversations, send messages |
| `Conversation` (Entity) | Represents a two-user conversation thread |
| `Message` (Entity) | Individual message within a conversation |
| `ConversationRepository` | Custom query to find conversation between two users |
| `MessageRepository` | Query messages by conversation, ordered by timestamp |

**Conversation model:**
A `Conversation` entity has `participantOneId` and `participantTwoId`. When a user tries to message someone, the service first checks if a conversation already exists between these two users. If yes, it reuses it. If no, it creates a new one. This prevents duplicate conversations.

---

### 5.7 Notification Service (Port 8087)

**Responsibility:** Creates and serves in-app notifications. Listens to events from other services and stores notifications in the database.

**Key classes:**

| Class | Purpose |
|---|---|
| `NotificationController` | REST endpoints to fetch and mark notifications as read |
| `NotificationEventListener` | `@RabbitListener` methods that consume events |
| `RabbitMQConfig` | Declares all queues, exchange, and bindings |
| `Notification` (Entity) | Stores notification data per user |
| `NotificationType` (Enum) | `STARTUP_CREATED`, `INVESTMENT_CREATED`, `INVESTMENT_APPROVED`, `TEAM_INVITE` |

**RabbitMQ setup:**
```java
// One topic exchange receives all events
@Bean TopicExchange exchange() {
    return new TopicExchange("founderlink.exchange");
}

// Four dedicated queues for different event types
@Bean Queue startupQueue()    { return new Queue("startup.created.queue"); }
@Bean Queue investmentQueue() { return new Queue("investment.created.queue"); }
@Bean Queue approvedQueue()   { return new Queue("investment.approved.queue"); }
@Bean Queue teamQueue()       { return new Queue("team.invite.queue"); }

// Routing key bindings
@Bean Binding startupBinding()    { /* routing key: startup.created */ }
@Bean Binding investmentBinding() { /* routing key: investment.created */ }
@Bean Binding approvedBinding()   { /* routing key: investment.approved */ }
@Bean Binding teamBinding()       { /* routing key: team.invite */ }
```

**Event listener:**
```java
@RabbitListener(queues = "investment.created.queue")
public void handleInvestmentCreated(InvestmentCreatedEvent event) {
    Notification n = new Notification();
    n.setUserId(event.getFounderId());   // Notify the founder
    n.setMessage("New investment interest from investor " + event.getInvestorId());
    n.setType(NotificationType.INVESTMENT_CREATED);
    n.setRead(false);
    notificationRepository.save(n);
}
```

**Why async notifications via RabbitMQ?**
If notifications were synchronous (Investment Service directly calls Notification Service via HTTP), then:
- If Notification Service is down → Investment creation fails
- Investment Service must wait for notification to be sent before returning a response

With RabbitMQ, Investment Service just publishes the event and immediately returns. The Notification Service processes it whenever it's ready. These are completely decoupled.

---

## 6. Cross-Cutting Concerns

### 6.1 RabbitMQ Event-Driven Architecture

**What is it?**
Instead of services calling each other directly (tight coupling), they communicate by publishing events to a message broker (RabbitMQ). Other services subscribe to events they care about.

**Why we used it:**
- **Decoupling** — Startup Service doesn't know about Notification Service. It just publishes a "startup.created" event.
- **Scalability** — Notification Service can be scaled independently without affecting publishers
- **Resilience** — If Notification Service is down, messages queue up in RabbitMQ and are processed when it comes back
- **Async** — Creating a startup doesn't wait for all downstream notifications to be sent

**Event flow:**
```
Startup Created
     ↓
StartupServiceImpl.createStartup()
     ↓ publishes to RabbitMQ
"founderlink.exchange" → routing key "startup.created"
     ↓
"startup.created.queue"
     ↓ consumed by
NotificationEventListener.handleStartupCreated()
     ↓
Notification saved to DB for relevant users
```

**Topic Exchange vs Direct Exchange:**
We use a `TopicExchange`. The routing key can use wildcards — `investment.*` would match both `investment.created` and `investment.approved`. This is more flexible than a `DirectExchange` which requires exact key matching.

---

### 6.2 Redis Caching

**What it is:**
Redis is an in-memory key-value store used as a cache. Instead of hitting the database on every request, frequently read data is stored in Redis and served from there.

**Configuration (same in both startup-service and user-service):**
```java
@Bean
public RedisCacheConfiguration cacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))              // Auto-expire after 10 minutes
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new Jackson2JsonRedisSerializer<>(Object.class)  // Store as JSON
            )
        );
}
```

**Annotations used:**

| Annotation | Meaning |
|---|---|
| `@Cacheable("approvedStartups")` | Check cache first; only call method if cache miss |
| `@CacheEvict(allEntries = true)` | Delete all entries in this cache (on write/update/delete) |
| `@CachePut` | Always call method but also update cache with result |

**Why TTL of 10 minutes?**
If we never evict, old data stays forever. 10-minute TTL ensures data is never more than 10 minutes stale even if explicit eviction misses a case.

**Why Jackson2JsonRedisSerializer?**
By default, Redis stores Java objects as binary (Java serialization). This is not human-readable and causes class version issues. JSON serialization stores objects as readable JSON strings and is more compatible.

---

### 6.3 OpenFeign + Circuit Breaker

**OpenFeign** is used in Investment Service to call Startup Service. It generates an HTTP client from a simple interface — no boilerplate needed.

**Why we added a FallbackFactory instead of just Fallback:**
`FallbackFactory` receives the actual exception (`Throwable cause`) that triggered the fallback. This allows us to log the real error and return a meaningful message. A simple `Fallback` class doesn't tell you WHY it failed.

```java
// FallbackFactory approach — we know WHY it failed
public StartupClient create(Throwable cause) {
    log.error("Startup Service call failed: {}", cause.getMessage());
    return id -> { throw new ServiceUnavailableException("..."); };
}
```

**Circuit breaker states:**
```
CLOSED → Everything works normally, requests go through
   ↓ (too many failures)
OPEN → Immediately return fallback, don't even try the service
   ↓ (after timeout)
HALF-OPEN → Try one request; if it succeeds → CLOSED, if not → OPEN again
```

---

### 6.4 AOP Logging

**All services** (Auth, Investment, Messaging, Notification, Team) have a `LoggingAspect` class.

**What is AOP (Aspect-Oriented Programming)?**
AOP lets you add behavior to existing code without modifying it. A "logging aspect" automatically runs before/after every method in the service layer — you don't have to add log statements to every single method.

```java
@Aspect
@Component
public class LoggingAspect {

    // Pointcut — which methods to intercept
    @Pointcut("execution(* com.capgemini.authservice.service.*.*(..))")
    public void serviceMethods() {}

    // Advice — what to do around those methods
    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        log.info("Entering: {}.{}()", className, methodName);
        try {
            Object result = joinPoint.proceed();  // Actually run the method
            log.info("Exiting: {} - took {}ms", methodName, elapsed);
            return result;
        } catch (Exception e) {
            log.error("Exception in {}: {}", methodName, e.getMessage());
            throw e;
        }
    }
}
```

**Why AOP instead of manual logging?**
Without AOP, you'd need to add `log.info("Entering createStartup...")` and `log.info("Exiting createStartup...")` to every service method — hundreds of lines of boilerplate. AOP handles it once for all methods. Also, business logic stays clean.

---

### 6.5 Global Exception Handling

Every service has a `GlobalExceptionHandler` annotated with `@RestControllerAdvice`.

**Why?**
Without it, unhandled exceptions return raw Spring error responses with a 500 status and a wall of stack trace. With `@RestControllerAdvice`, every exception type is caught and converted to a clean, structured JSON response:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Startup with id 99 not found",
  "timestamp": "2025-03-23T10:15:30"
}
```

**Exception hierarchy used across services:**
- `ResourceNotFoundException` → 404
- `DuplicateResourceException` → 409
- `UnauthorizedException` → 403
- `BadRequestException` → 400
- `ServiceUnavailableException` → 503 (when Feign fallback triggers)

---

### 6.6 JWT Security Model

**How it flows through the entire system:**

```
1. User logs in → Auth Service returns JWT token
2. Frontend stores token in localStorage
3. Frontend sends: Authorization: Bearer <token> on every request
4. API Gateway intercepts the request
5. Gateway's JwtAuthenticationFilter validates the token
6. Gateway extracts userId and roles from token claims
7. Gateway adds headers: X-User-Id: 42, X-User-Roles: ROLE_FOUNDER
8. Request forwarded to microservice (WITHOUT the original Bearer token)
9. Microservice's own JwtAuthenticationFilter reads X-User-Id and X-User-Roles
10. Builds Spring Security Authentication object from headers
11. @PreAuthorize("hasAuthority('ROLE_FOUNDER')") checks Authentication
12. auth.getName() returns the userId (used to verify ownership)
```

**Why HMAC-SHA256 for signing?**
JWT tokens are signed so that clients cannot tamper with the payload (e.g., change `"role": "ROLE_INVESTOR"` to `"role": "ROLE_ADMIN"`). The signature is a cryptographic hash of the header + payload using a secret key. If anyone changes the payload, the signature won't match and the token is rejected.

**Shared JWT secret:**
All services share the same JWT secret key (in `application.yml`). This is required because any service that validates the token needs to verify the signature, and that requires the same secret that was used to sign it.

---

## 7. Database Design

Each microservice has its **own dedicated PostgreSQL database**. This is the **Database-per-Service pattern** — a core microservices principle.

| Service | Database | Key Tables |
|---|---|---|
| Auth Service | `founderlink_auth` | `users`, `roles` |
| User Service | `founderlink_users` | `user_profiles` |
| Startup Service | `founderlink_startups` | `startups`, `startup_followers` |
| Investment Service | `founderlink_investments` | `investments` |
| Team Service | `founderlink_teams` | `team_members` |
| Messaging Service | `founderlink_messaging` | `conversations`, `messages` |
| Notification Service | `founderlink_notifications` | `notifications` |

**Why separate databases?**
- **Independence** — Startup Service can change its schema without affecting other services
- **Isolation** — A bad migration in Investment Service doesn't corrupt startup data
- **Scalability** — You can scale only the database that's under load

**`spring.jpa.hibernate.ddl-auto: update`**
All services use `ddl-auto: update`, which means Hibernate automatically creates or alters tables based on your entity classes at startup. Good for development; in production you'd use `validate` with Flyway/Liquibase migrations.

---

## 8. Port Reference

| Service | Port | Purpose |
|---|---|---|
| Config Server | 8888 | Centralized configuration |
| Eureka Server | 8761 | Service registry & discovery |
| API Gateway | 8080 | Single entry point, JWT validation, routing |
| Auth Service | 8081 | Registration, login, JWT generation |
| User Service | 8082 | User profile management |
| Startup Service | 8083 | Startup CRUD, search, approval |
| Investment Service | 8084 | Investment tracking, Feign → Startup |
| Team Service | 8085 | Team membership & invitations |
| Messaging Service | 8086 | Conversations & messages |
| Notification Service | 8087 | Async notifications via RabbitMQ |
| PostgreSQL | 5432 | Relational database |
| RabbitMQ | 5672 | Message broker |
| Redis | 6379 | Distributed cache |
| React Frontend | 3000 | UI |

---

## 9. Interview Q&A Cheatsheet

**Q: Why microservices instead of a monolith?**
Each service can be developed, deployed, and scaled independently. If Startup Service gets heavy traffic, we scale only that. In a monolith, you'd have to scale the entire application.

**Q: How does the API Gateway know where to route requests?**
Route predicates in `application.yml` match URL paths. The gateway uses `lb://SERVICE-NAME` which queries Eureka for the actual address of that service.

**Q: What happens if Config Server is down when a service starts?**
The `optional:` prefix in `spring.config.import` means the service falls back to its own local `application.yml` instead of crashing.

**Q: Why does the gateway add `X-User-Id` headers instead of forwarding the token?**
So downstream services don't need the JWT secret. The gateway decodes the token once, extracts the important claims, and passes them as plain headers. This is the **Token Relay pattern**.

**Q: How does `@PreAuthorize` know the user's role?**
Each service has a `JwtAuthenticationFilter` that reads the `X-User-Roles` header and creates a Spring Security `Authentication` object with `GrantedAuthority` values. `@PreAuthorize` reads from this Security Context.

**Q: Why RabbitMQ instead of direct HTTP calls for notifications?**
Direct HTTP calls are synchronous and tightly coupled. If Notification Service is down, every startup creation would fail. RabbitMQ decouples them — messages queue up and are processed when the service is available.

**Q: What is the Topic Exchange in RabbitMQ?**
A Topic Exchange routes messages based on routing key patterns. We use routing keys like `startup.created`, `investment.approved`. Multiple queues can bind to the same exchange with different patterns, so one event can trigger multiple consumers.

**Q: Why does each microservice have its own `JwtAuthenticationFilter` if the gateway already validates the token?**
Defense in depth. If someone bypasses the gateway and calls a service directly, the service-level filter still protects it. Also, the service filter is what populates the Spring Security context, enabling `@PreAuthorize` and `auth.getName()` to work.

**Q: What is AOP and where did you use it?**
Aspect-Oriented Programming separates cross-cutting concerns from business logic. We used it for logging — `LoggingAspect` in every service automatically logs entry, exit, execution time, and exceptions for all service methods without touching the business logic code.

**Q: What is a Circuit Breaker?**
A pattern to prevent cascade failures. If Investment Service calls Startup Service and it fails repeatedly, the circuit "opens" and subsequent calls immediately return a fallback response without actually calling the failing service. After a timeout it tries again (half-open state).

**Q: What is `@Cacheable` and how does it work?**
Spring checks the cache first using the method arguments as the key. If a cached value exists, it returns it without executing the method. If not, it executes the method and stores the result in cache. `@CacheEvict` removes entries when data changes.

**Q: Why Database-per-Service?**
Each service owns its own data. This prevents tight coupling at the data layer. Services communicate through APIs or events, never by sharing a database. This is a core microservices principle.

**Q: How are passwords stored?**
BCrypt hashing via Spring Security's `BCryptPasswordEncoder`. BCrypt is a one-way hash with a built-in salt — even identical passwords produce different hashes. The original password can never be recovered from the hash.
