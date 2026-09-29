# FounderLink - Interview Preparation Guide

This document contains comprehensive answers to the 24 interview questions regarding the FounderLink project and its Microservices Architecture. It is tailored to the technologies used in your project (Spring Boot, React, API Gateway, RabbitMQ, JWT, etc.).

## General Project Questions

### 1. Project Introduction
**Answer:** FounderLink is a comprehensive platform designed to bridge the gap between startup founders, potential co-founders, investors, and essential resources. The primary goal is to facilitate networking and collaboration within the startup ecosystem. To ensure the platform is highly scalable, resilient, and easily maintainable as the user base grows, it was architected from the ground up using a modern Microservices Architecture with Spring Boot for the backend and React for the frontend.

### 2. Project Flow of Application
**Answer:** The typical flow starts when a user interacts with the React frontend. 
1. The frontend sends an HTTP request (e.g., login or fetching user data).
2. This request first hits the **API Gateway**, which acts as the single entry point.
3. The API Gateway handles cross-cutting concerns like JWT validation and CORS.
4. If valid, the Gateway routes the request to the appropriate downstream microservice (e.g., `AuthService` or `UserService`).
5. The microservice processes the business logic, often interacting with its own dedicated database (PostgreSQL).
6. If a service needs data from another, it uses **Feign Clients** for synchronous calls. For event-driven tasks (like sending a welcome email after registration), it publishes a message to **RabbitMQ**, which is then consumed asynchronously by a Notification service.
7. The response flows back through the Gateway to the frontend.

### 3. Project PPT Explanation (Outline)
**Answer:** If presenting a PPT, I would structure it as follows:
- **Slide 1: Problem Statement** (The difficulty founders face in finding right partners/investors).
- **Slide 2: Solution (FounderLink)** (What the platform does).
- **Slide 3: High-Level Architecture** (Visual diagram of frontend, API gateway, microservices, and databases).
- **Slide 4: Tech Stack** (Highlighting React, Spring Boot, PostgreSQL, RabbitMQ, Docker).
- **Slide 5: Key Features** (Authentication, Matching, Messaging, etc.).
- **Slide 6: Challenges & Solutions** (e.g., handling distributed data, solved using specific patterns).
- **Slide 7: Future Scope**.

### 4. Project Architecture Explanation
**Answer:** The architecture is a decentralized **Microservices pattern**. The client (React app) talks to a **Spring Cloud Gateway**. Behind the gateway, we have several independent Spring Boot applications acting as microservices (e.g., Auth Service, User Service). 
- **Service Discovery:** We use Eureka Server so services can find each other dynamically.
- **Configuration:** Spring Cloud Config Server manages properties centrally.
- **Data Management:** Each service has its own dedicated database to ensure loose coupling.
- **Communication:** REST/Feign for direct service-to-service calls, and RabbitMQ for asynchronous event-driven communication to decouple processes.

### 5. Architecture Diagram Walkthrough
**Answer:** *(Imagine pointing at a diagram)* "Here on the left, we have the React Frontend. All traffic from the frontend hits this block here: the API Gateway. It validates the JWT token. Below that, we have our service registry (Eureka). On the right, you see our core services: Auth, User, etc. Notice that each service points to its own database icon. Finally, connecting some of these services is the RabbitMQ message broker for handling background events without blocking the main request thread."

### 6. Case Study Explanation
**Answer:** *(Use a specific feature as a case study, e.g., User Registration)* "Let's look at User Registration. When a user registers, the frontend sends data to the `AuthService`. The `AuthService` encrypts the password and saves the credentials to its DB. It then emits a 'UserRegisteredEvent' to a RabbitMQ exchange. The `UserService` picks this up to create a profile record in its DB, and a `NotificationService` picks it up to send a welcome email. This ensures the Auth Service isn't slowed down by email sending or profile creation."

### 7. Layers in Microservice Architecture & their functionalities
**Answer:** Within a typical Spring Boot microservice in our project, we follow a layered architecture:
- **Controller Layer:** Handles incoming HTTP requests, maps them to endpoints, and validates initial input.
- **Service Layer:** Contains the core business logic. It orchestrates data flow and rules.
- **Repository/DAO Layer:** Handles data access operations using Spring Data JPA to interact with the database.
- **Entity/DTO Layer:** Entities represent database tables, while DTOs (Data Transfer Objects) are used to transfer data between the client and the controller without exposing the database schema.

### 8 & 9. Technologies used with versions (Tech Stack)
**Answer:**
- **Frontend:** React (with TypeScript), Tailwind CSS.
- **Backend:** Java 17, Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security).
- **Infrastructure/Routing:** Spring Cloud Gateway, Netflix Eureka, Spring Cloud Config.
- **Database:** PostgreSQL.
- **Messaging Broker:** RabbitMQ.
- **Containerization:** Docker & Docker Compose.

### 10. Future improvements / implementations
**Answer:** 
- Implementing centralized logging (e.g., ELK stack - Elasticsearch, Logstash, Kibana) to trace requests across multiple services easily.
- Adding caching (e.g., Redis) for frequently accessed data to improve response times.
- Implementing a full CI/CD pipeline using GitHub Actions or Jenkins.

### 11. Production-ready improvements
**Answer:**
- **Resilience:** Adding Circuit Breakers (like Resilience4j) to prevent cascading failures if a downstream service goes offline.
- **Security:** Moving secrets out of configuration files and into a secure vault (like HashiCorp Vault or AWS Secrets Manager).
- **Monitoring:** Adding Prometheus and Grafana for real-time monitoring of service health and metrics.
- **Container Orchestration:** Moving from Docker Compose to Kubernetes for better scaling and self-healing in production.

### 12. Explain any one Controller implemented in your project
**Answer:** In the `AuthService`, we have an `AuthController`. It has an endpoint `POST /api/auth/login`. 
It accepts a `LoginRequestDTO` containing the username and password. The controller passes this to the `AuthService` class, which checks the credentials against the database. If valid, the service generates a JWT. The controller then wraps this token in a `LoginResponseDTO` and returns it to the client with a 200 OK status.

### 13. DTO purpose and implementation
**Answer:** DTOs (Data Transfer Objects) are crucial for separating the internal database representation (Entities) from the API payload. 
- **Purpose:** They prevent over-posting vulnerabilities, hide sensitive data (like passwords) from API responses, and allow the API structure to remain stable even if the underlying database schema changes.
- **Implementation:** I created classes like `UserDto` or `RegistrationRequest`. In the Controller, I accept the DTO, and then use a mapper (manual or MapStruct) to convert the DTO to an Entity before passing it to the repository.

### 14. Entity relationships in your services
**Answer:** Because it's a microservices architecture, we keep entity relationships strictly *within* the boundaries of a single service. For example, in the User Service, a `User` entity might have a One-to-Many relationship with `Experience` or `Education` entities. However, we *do not* have foreign keys crossing databases between the Auth Service and User Service. Instead, we reference records by storing the unique `userId` (UUID) in the related services.

### 15. Jar file storage after build
**Answer:** When building locally using Maven (`mvn clean install`), the generated `.jar` files are stored in the `/target` directory of each respective module. In a Dockerized environment, the Dockerfile copies this `.jar` file from the target directory into the container image (usually into a `/app` directory) so it can be executed using the `java -jar` command.

---

## Microservices Architecture Related

### 16. Why Microservices Architecture?
**Answer:** We chose Microservices to achieve independent deployability and scalability. As FounderLink grows, different parts of the system will experience different loads. For example, the matching engine might need more compute power than the notification service. Microservices allow us to scale them independently, use different technologies if needed, and allow different team members to work on separate services without merge conflicts in a massive monolithic codebase.

### 17. Benefits of Microservices over Monolithic Architecture
**Answer:**
- **Independent Deployment:** A bug fix in the Auth service doesn't require redeploying the entire application.
- **Fault Isolation:** If the email service crashes, the core application (like logging in and viewing profiles) remains unaffected.
- **Technology Agnostic:** You can write one service in Java and another in Node.js or Python if it better suits the task.
- **Easier to Understand:** Each service has a specific, bounded context, making the codebase smaller and easier for new developers to grasp.

### 18. Advantages of Monolithic Architecture
**Answer:** It's important to acknowledge that monoliths aren't bad. Their advantages include:
- **Simplicity:** Easier to develop initially, easier to test (no complex network calls to mock), and easier to deploy (just one artifact).
- **Data Consistency:** Since everything uses one database, ACID transactions are straightforward to implement.
- **Performance:** No network latency between different modules of the application.

### 19. Why modern applications prefer Microservices?
**Answer:** Modern applications prioritize agility, rapid release cycles, and massive scale. Cloud-native environments (like AWS, Kubernetes) are practically built to host microservices. The business need to push updates multiple times a day without downtime makes the independent deployability of microservices highly attractive, outweighing the added operational complexity.

### 20. Why different databases per microservice?
**Answer:** This is known as the "Database-per-service" pattern. It ensures **loose coupling**. If multiple services shared a single database, a change in the database schema required by Service A might break Service B. It also prevents one service from hogging all database connections and bringing down the entire system. It forces services to communicate strictly through APIs.

### 21. Why not use different tables in a single DB?
**Answer:** While different tables in a single database provide logical separation, they don't provide physical isolation. A runaway query in one service can lock tables or consume all CPU/memory on the database server, causing all other services sharing that DB to fail. Also, a single database forces all services to use the same database technology (e.g., you couldn't use Postgres for relational data and MongoDB for unstructured document data easily).

### 22. How did you implement data consistency across services?
**Answer:** Since we can't use traditional ACID transactions across different databases, we use **Eventual Consistency**. When a transaction spans multiple services, we use an event-driven approach (like the Saga pattern). For example, if a user updates their profile, the UserService updates its DB and publishes a `ProfileUpdatedEvent` to RabbitMQ. Other services interested in this (like a Search Indexing service) consume this event and update their own local data. The data becomes consistent across the system *eventually*.

### 23. What happens if one microservice fails?
**Answer:** Because of our architecture, the failure should be isolated. If the Notification Service goes down, the RabbitMQ broker will simply hold onto the messages. Once the service is restarted, it will process the backlog. For synchronous calls (via Feign), we can implement fallback methods or Circuit Breakers so that if Service A cannot reach Service B, it returns a cached response or a graceful error message, rather than crashing Service A.

### 24. How do you detect a disrupted service?
**Answer:** We use the **Eureka Service Registry**. Every microservice sends a "heartbeat" to Eureka periodically. If Eureka stops receiving heartbeats from a specific instance, it removes that instance from its registry, so the API Gateway stops routing traffic to it. In a production environment, we would also use monitoring tools like Prometheus to track error rates and alert us, and tools like Zipkin or Sleuth for distributed tracing to see exactly where a request failed in the chain of services.

---

## API Gateway

### 25. What is API Gateway?
**Answer:** The API Gateway acts as the single entry point for all client applications. Instead of clients calling each microservice directly (and needing to know their hostnames/ports), they send all requests to the Gateway, which then routes them to the appropriate service. In our project, we used Spring Cloud Gateway.

### 26. Roles of API Gateway
**Answer:** Its primary roles include:
- **Routing:** Forwarding requests to the correct downstream microservice.
- **Security:** Handling authentication and authorization (e.g., validating our JWT tokens) before requests reach the core services.
- **Cross-Origin Resource Sharing (CORS):** Managing CORS policies centrally so the React frontend can communicate with the backend.
- **Load Balancing:** Distributing requests across multiple instances of a service.
- **Rate Limiting:** Preventing abuse by limiting the number of requests a client can make in a given timeframe.

### 27. How do you integrate/register microservices with API Gateway?
**Answer:** We integrate the API Gateway with **Eureka**. Instead of hardcoding the URLs of microservices in the Gateway configuration (`application.yml`), we configure the Gateway to use `lb://SERVICE-NAME` (load-balanced URL). The Gateway queries Eureka to resolve `SERVICE-NAME` to its actual IP address and port dynamically.

### 28. How does Admin access the application through Gateway?
**Answer:** The Admin accesses the application through the same API Gateway. However, during the authentication phase, the Gateway's security filter reads the JWT token, extracts the user's role (e.g., `ROLE_ADMIN`), and passes it downstream. Certain routes in the Gateway or specific endpoints in the microservices will be secured using `@PreAuthorize("hasRole('ADMIN')")`, ensuring only users with admin privileges can access those specific functions.

---

## Service Discovery – Eureka

### 29. Why did you use Eureka Server?
**Answer:** In a microservices architecture, services often need to talk to each other (e.g., Auth service talking to User service). Because instances can scale up or down, and their IP addresses or ports might change dynamically (especially in Docker/Kubernetes environments), we can't hardcode URLs. Eureka Server acts as a dynamic phonebook so services can find each other based on their logical names.

### 30. Need for Service Discovery
**Answer:** Without Service Discovery, managing network locations is a nightmare. It provides:
- **Resilience:** If an instance crashes, Eureka removes it, so traffic isn't routed to a dead node.
- **Scalability:** New instances can spin up, register themselves, and immediately start receiving traffic.
- **Decoupling:** Services don't need to know the physical network topologies of other services.

### 31. How does Eureka create an instance for a microservice?
**Answer:** Eureka doesn't "create" the instance. The microservice itself spins up (e.g., via Docker). Inside the microservice, we include the `@EnableDiscoveryClient` (or `@EnableEurekaClient`) annotation and Eureka Client dependencies. On startup, the client contacts the Eureka Server URL (configured in `application.yml`) and registers its logical application name, IP address, and port.

### 32. How are services registered with Eureka?
**Answer:** 
1. We add the `spring-cloud-starter-netflix-eureka-client` dependency to the microservice.
2. In the `application.yml`, we define `eureka.client.serviceUrl.defaultZone` pointing to the Eureka server's URL.
3. We set `spring.application.name` (e.g., `user-service`).
4. When the application starts, the Eureka client automatically sends a REST call to the Eureka server, registering its details. It continues to send periodic "heartbeats" to prove it's still alive.

---

## Inter-Service Communication

### 33. How do microservices communicate with each other?
**Answer:** They communicate in two primary ways:
- **Synchronous Communication:** The calling service waits for a response. We use HTTP/REST protocols facilitated by **OpenFeign**.
- **Asynchronous Communication:** The calling service fires a message and doesn't wait for a response. We use **RabbitMQ** for this.

### 34. One-way vs Two-way service communication (arrow connections)
**Answer:** 
- **Two-way (Synchronous):** Service A sends a request to Service B and waits for the HTTP response (e.g., Fetching user details during a login check).
- **One-way (Asynchronous):** Service A publishes an event to a message broker (RabbitMQ) and immediately continues its work. Service B consumes the event at its own pace (e.g., sending a welcome email).

### 35. How does OpenFeign simplify communication?
**Answer:** OpenFeign is a declarative web service client. Instead of writing boilerplate code to construct HTTP requests, parse JSON, and handle errors (like we would with `RestTemplate`), we just define a Java interface annotated with `@FeignClient`. We declare the method signatures and HTTP mappings (e.g., `@GetMapping`), and Feign automatically generates the implementation at runtime.

### 36. How did you implement Feign Client?
**Answer:** 
1. Added the `@EnableFeignClients` annotation to the main application class.
2. Created an interface (e.g., `UserClient`) and annotated it with `@FeignClient(name = "user-service")`.
3. Added abstract methods annotated with Spring MVC annotations (like `@GetMapping("/users/{id}")`) corresponding to the target service's endpoints.
4. In my business logic, I autowired this interface and called the methods directly, as if it were a local bean.

### 37. Communication without OpenFeign (e.g., RestTemplate)
**Answer:** Before Feign, we would use `RestTemplate` or the newer `WebClient`. With `RestTemplate`, you have to explicitly construct the URL string, handle the HTTP headers, execute the `exchange()` or `getForObject()` method, and manually deal with the ResponseEntity. Feign abstracts all this away into a clean interface.

### 38. Synchronous vs Asynchronous communication
**Answer:**
- **Synchronous:** Real-time, point-to-point. The client blocks until the server responds. Good for queries (getting data), but can lead to cascading failures if the target service is slow.
- **Asynchronous:** Event-driven, non-blocking. The client sends a message and moves on. Good for commands or events (updating states, sending emails). It provides better decoupling and system resilience.

### 39. What have you used in your project and why?
**Answer:** I used **both**. I used **OpenFeign** (synchronous) when a service absolutely needed immediate data from another service to complete a user request (e.g., checking if a specific user exists before performing an action). I used **RabbitMQ** (asynchronous) for background tasks like sending notifications or syncing profile updates, where the user doesn't need to wait for the email to actually send before seeing a "Success" screen.

### 40. RabbitMQ implementation in your project
**Answer:** 
1. Configured RabbitMQ connection properties in `application.yml`.
2. Created Configuration classes defining **Exchanges**, **Queues**, and **Bindings**.
3. In the producer service (e.g., AuthService), I used `RabbitTemplate` to convert an event object to JSON and send it to an exchange.
4. In the consumer service (e.g., NotificationService), I used the `@RabbitListener(queues = "notification_queue")` annotation on a method to automatically process incoming messages.

### 41. Use of RabbitMQ in future implementation
**Answer:** In the future, I plan to use RabbitMQ for:
- Implementing the **CQRS** (Command Query Responsibility Segregation) pattern, where commands (writes) are sent via RabbitMQ to update a separate read-optimized database.
- Batch processing of analytics data.
- Handling long-running tasks like generating large PDF reports for founders.

---

## Security

### 42. Authentication vs Authorization
**Answer:**
- **Authentication:** Verifying *who* you are (e.g., checking username and password during login).
- **Authorization:** Verifying *what* you are allowed to do (e.g., checking if the logged-in user has the "ADMIN" role to delete a user).

### 43. How did you control access of authorized users?
**Answer:** After authentication, the system generates a JWT. For subsequent requests, the API Gateway inspects this token. In the microservices, we use Spring Security annotations like `@PreAuthorize("hasAuthority('ROLE_FOUNDER')")` on controller methods to ensure only users with the correct roles embedded in their JWT can access specific endpoints.

### 44. What is JWT and how did you use it?
**Answer:** JWT (JSON Web Token) is a stateless, secure way to transmit information between parties as a JSON object. 
In our project, when a user logs in, the `AuthService` validates their credentials and generates a JWT containing their user ID and roles, signed with a secret key. The frontend stores this token and sends it in the `Authorization: Bearer <token>` header on every subsequent request. The API Gateway validates the token's signature without needing to hit the database.

### 45. Security implemented at Gateway level or Service level?
**Answer:** Security is implemented at **both** levels. 
- **Gateway Level:** Handles the heavy lifting of validating the JWT signature, checking expiration, and rejecting invalid requests immediately before they reach internal networks.
- **Service Level:** The internal services trust the Gateway but still perform fine-grained authorization (Role-Based Access Control) using `@PreAuthorize` to ensure the user has the right permissions for the specific action.

---

## Logging

### 46. How did you implement logging in your project?
**Answer:** We used SLF4J with Logback (the default in Spring Boot). I added logging statements (`log.info()`, `log.error()`) strategically in controllers and services to track request flow and capture errors. In a microservices environment, it's also crucial to use a tool like **Spring Cloud Sleuth** (or Micrometer Tracing) which injects a unique `traceId` into the logs, allowing us to track a single request as it jumps across multiple different microservices.

---

## Database & JPA Implementation

### 47. Which database did you use?
**Answer:** We used **PostgreSQL**, an open-source, powerful object-relational database system. We chose it for its reliability, feature robustness, and strong support for JSON data, which is useful in modern applications.

### 48. How did you configure the database?
**Answer:** In the `application.yml` of each microservice (or via the Config Server), we defined the data source properties:
`spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, and `spring.datasource.driver-class-name`. We also configured JPA/Hibernate properties like `spring.jpa.hibernate.ddl-auto=update` for development.

### 49. How is MySQL/PostgreSQL connected to Spring Boot?
**Answer:** It connects using JDBC (Java Database Connectivity) underneath, but we abstract this using **Spring Data JPA**. We include the database driver dependency (e.g., `postgresql` driver) and the `spring-boot-starter-data-jpa` dependency. Spring Boot's auto-configuration detects the driver and properties, and automatically creates a `DataSource` and an `EntityManager` for us.

### 50. How are services connected to database (code explanation)?
**Answer:** 
1. We create an Entity class (e.g., `User.java`) annotated with `@Entity` and `@Table`.
2. We map Java fields to database columns using `@Id`, `@Column`, etc.
3. We create an interface (e.g., `UserRepository`) that extends `JpaRepository<User, Long>`.
4. We inject this repository into our Service layer. Spring Data JPA dynamically generates the implementation of the database operations at runtime.

### 51. JPARepository extends which interface?
**Answer:** `JpaRepository` extends `PagingAndSortingRepository`, which in turn extends `CrudRepository`. Therefore, `JpaRepository` provides all basic CRUD operations, pagination, sorting, and additional JPA-specific methods like `flush()` or `saveAndFlush()`.

### 52. Derived queries in JPA Repository
**Answer:** Derived queries (or Query Methods) allow us to execute database queries by simply defining method names following specific naming conventions. For example, if I declare `Optional<User> findByEmail(String email);` in the interface, Spring Data JPA automatically writes and executes the SQL query `SELECT * FROM users WHERE email = ?` without me needing to write any SQL or JPQL.

### 53. SQL Queries used in your project
**Answer:** Most basic SQL queries were handled by JPA derived methods (e.g., finding a user by ID, checking if an email exists). For more complex scenarios, I used the `@Query` annotation. For example, joining tables or performing complex aggregations where derived query names would be too long or impossible to construct.

### 54. Order By Desc query
**Answer:** Using Spring Data JPA derived queries, I can append `OrderBy[Field]Desc`. For example: `List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);`. Alternatively, using JPQL: `@Query("SELECT p FROM Post p WHERE p.userId = :userId ORDER BY p.createdAt DESC")`.

### 55. DDL operations used
**Answer:** DDL (Data Definition Language) operations define the schema. In our Spring Boot project, we relied on Hibernate to handle this during development using the property `spring.jpa.hibernate.ddl-auto=update`. When the application started, Hibernate automatically executed DDL commands (like `CREATE TABLE`, `ALTER TABLE`, `ADD CONSTRAINT`) based on our `@Entity` class definitions. In production, we would use a migration tool like Flyway or Liquibase.

### 56. Primary Key vs Foreign Key in your schema
**Answer:**
- **Primary Key:** A unique identifier for a record in a table (e.g., `id` in the `users` table). I mapped this using the `@Id` annotation.
- **Foreign Key:** A field in one table that links to the primary key of another table, establishing a relationship. In JPA, we map this using annotations like `@ManyToOne` or `@OneToMany` along with `@JoinColumn(name = "foreign_key_id")`. Note that in our microservices, foreign keys only existed *within* a single service's database, not across different services' databases.

### 57. Operation used to combine two tables (JOIN)
**Answer:** To combine data from two related tables, we use a **JOIN** operation. In Spring Data JPA, when we define a relationship like `@OneToMany` with `FetchType.EAGER` (or explicitly write a `JOIN FETCH` query in JPQL), Hibernate automatically generates the SQL `INNER JOIN` or `LEFT OUTER JOIN` behind the scenes to fetch the parent entity along with its related child entities in a single database round trip.

---

## Exception Handling (Project Level)

### 58. What is Global Exception Handling?
**Answer:** Instead of writing `try-catch` blocks in every controller method, Global Exception Handling allows us to catch exceptions centrally across the entire application. It intercepts exceptions thrown by any controller and returns a standardized error response to the client.

### 59. How did you implement Global Exception Handling?
**Answer:** I used the `@ControllerAdvice` (or `@RestControllerAdvice`) annotation on a class. Inside this class, I wrote methods annotated with `@ExceptionHandler(SpecificException.class)`. When that specific exception is thrown anywhere in the application, this method catches it and formats a `ResponseEntity` with a proper HTTP status code and a custom error message DTO.

### 60. How did you raise custom exceptions in your project?
**Answer:** I created custom exception classes that extend `RuntimeException` (e.g., `UserNotFoundException`, `EmailAlreadyExistsException`). In my Service layer, if a business rule failed, I used the `throw new` keyword. For example: `if (user == null) throw new UserNotFoundException("User not found with ID: " + id);`. This exception is then caught by the `@ControllerAdvice` class.

---

## Transactions

### 61. What is @Transactional?
**Answer:** `@Transactional` is a Spring annotation used to manage database transactions declaratively. It ensures that a series of database operations either all succeed (commit) or all fail (rollback). This guarantees the ACID (Atomicity, Consistency, Isolation, Durability) properties of the database.

### 62. Where did you use @Transactional in your project?
**Answer:** I used it primarily in the **Service layer** on methods that perform multiple write operations (e.g., saving a user profile and then saving their initial preferences in two separate tables within the same database). If the second save fails, the entire transaction rolls back so we don't end up with partial data.

### 63. @Transactional belongs to which concept?
**Answer:** It is built on the concept of **Spring AOP (Aspect-Oriented Programming)**. Spring creates a proxy around the class or method, opening a transaction before the method execution and either committing or rolling it back after the method completes (or throws a runtime exception).

---

## Testing

### 64. How does testing work in your project?
**Answer:** We wrote unit tests for our business logic using JUnit 5 and mocked external dependencies (like Repositories or Feign Clients) using Mockito. For the controller layer, we used `MockMvc` to simulate HTTP requests and verify responses.

### 65. JUnit implementation
**Answer:** We added the `spring-boot-starter-test` dependency. Test classes are annotated with `@SpringBootTest` (for integration) or `@ExtendWith(MockitoExtension.class)` (for unit tests). Test methods are annotated with `@Test`. We use `Assertions.assertEquals()` or `Assertions.assertNotNull()` to verify outcomes.

### 66. Explain one challenging/edge test case
**Answer:** A challenging test case was testing a race condition where two users try to connect with the same founder simultaneously. I had to mock the database to simulate a delay, throw a `DataIntegrityViolationException` for the second request, and assert that the service layer correctly handled the exception and returned a friendly "Already connected" error instead of a 500 server crash.

### 67. Mockito usage
**Answer:** We use Mockito to isolate the class being tested. For instance, when testing the `UserService`, we don't want to actually hit the real database. We use Mockito to create a "dummy" `UserRepository` that returns predefined data when its methods are called (using `when(...).thenReturn(...)`).

### 68. What is @Mock?
**Answer:** `@Mock` is a Mockito annotation used to create a fake instance of an object (a mock). For example, `@Mock UserRepository userRepository;` creates a mock repository. We also use `@InjectMocks` on the service class to automatically inject these mocked dependencies into it.

### 69. Can Swagger be used for testing?
**Answer:** Yes, Swagger (OpenAPI) provides an interactive UI (`/swagger-ui.html`) that allows developers and QA to manually test REST APIs directly from the browser without needing tools like Postman. You can input parameters, execute the request, and see the response format and status code.

---

## Performance / Load Handling

### 70. How does your system handle concurrent user requests?
**Answer:** Spring Boot uses an embedded Tomcat server by default, which assigns a separate thread from its thread pool to handle each incoming HTTP request. Because our microservices are stateless (we use JWTs instead of server-side sessions), we can handle concurrent requests efficiently without locking resources.

### 71. How do you handle server load?
**Answer:** To handle high server load, we scale our microservices horizontally. If the `MatchingService` is under heavy load, we spin up multiple instances of it. The API Gateway and Eureka automatically load-balance the traffic across these available instances.

### 72. Load balancing in your project
**Answer:** We achieve load balancing at two levels:
1. **Client-side Load Balancing:** The API Gateway uses Spring Cloud LoadBalancer (integrated with Eureka) to distribute requests among multiple instances of a downstream microservice in a round-robin fashion.
2. **Infrastructure Level:** In a production environment, we would also have an external load balancer (like AWS ALB or Nginx) distributing traffic to multiple instances of the API Gateway itself.

### 73. Real-world scenario: insufficient stock handling (Or in FounderLink: Overbooking an event/investor slot)
**Answer:** If two users try to book the last available slot simultaneously, we would use **Optimistic Locking** in JPA. We add a `@Version` field to the Entity. When both users read the slot, they get version 1. User A books it, updating the DB and version becomes 2. When User B's transaction tries to save, the DB checks the version. Since User B expects version 1 but the DB has version 2, an `ObjectOptimisticLockingFailureException` is thrown, which we catch and tell User B "Slot already taken."

### 74. Network issue scenario: response ID not received — how to fetch created data without ID?
**Answer:** If a client creates a resource but a network timeout prevents them from receiving the ID in the response, we can implement **Idempotency Keys**. The client generates a unique UUID (Idempotency Key) and sends it in the request header. We save this key along with the record. If the client doesn't get a response, they retry the exact same request with the same key. The server checks the DB, sees the key already exists, and simply returns the previously created record without creating a duplicate.

---

## Git / Build Tools

### 75. What is Git?
**Answer:** Git is a distributed version control system. It tracks changes in source code during software development, allowing multiple developers to work together non-linearly.

### 76. Why is Git used?
**Answer:** It's used for tracking changes, reverting to previous states if something breaks, branching for new feature development without affecting the main codebase, and merging code from different developers collaboratively.

### 77. What is GitHub?
**Answer:** GitHub is a cloud-based hosting service that lets you manage Git repositories. While Git is the tool running locally on your computer, GitHub provides a centralized hub to store code online, review pull requests, and manage CI/CD pipelines.

### 78. GitHub Release
**Answer:** A GitHub Release is a packaged version of the software ready for deployment. It usually corresponds to a Git tag (like `v1.0.0`) and contains compiled binaries, release notes, and source code at that exact point in time.

### 79. How to check which branch you are working on?
**Answer:** By running the command `git branch` in the terminal. The current branch will be highlighted with an asterisk (*). Alternatively, `git status` also tells you the current branch.

### 80. `mvnw` file usage
**Answer:** `mvnw` (Maven Wrapper) is a script provided in the project root. It allows anyone to run Maven commands (like `./mvnw clean install`) without actually having Maven installed on their computer. It automatically downloads the correct Maven version specified for the project, ensuring consistency across all developer machines.

### 81. Maven commands used
**Answer:** 
- `mvn clean`: Deletes the `target` directory (compiled files).
- `mvn compile`: Compiles the source code.
- `mvn test`: Runs the JUnit tests.
- `mvn install`: Compiles, tests, and packages the code into a `.jar` file, and installs it in the local Maven repository.
- `mvn spring-boot:run`: Starts the Spring Boot application locally.

---

## Spring Bean

### 82. Scope of Bean used in your project
**Answer:** The default scope in Spring is **Singleton** (one instance per Spring IoC container). Almost all our controllers, services, and repositories are Singletons, which is memory-efficient because they are stateless. We might use **Prototype** (a new instance every time) or **Request** scope (one instance per HTTP request) for specific components that hold state, though Singleton is the standard.

---

## B. CORE JAVA

### OOPS
1. **What is OOPS?** Object-Oriented Programming System is a paradigm based on "objects" that contain data (fields) and code (methods). The four main pillars are Encapsulation, Inheritance, Polymorphism, and Abstraction.
2. **Where have you used OOPS?** 
   - *Inheritance:* Extending `RuntimeException` for custom exceptions.
   - *Polymorphism:* Using interfaces like `JpaRepository` and overriding methods.
   - *Encapsulation:* Using `private` fields in Entities/DTOs and accessing them via getters/setters.
3. **Is Java 100% OOP?** No. Java uses primitive data types (like `int`, `boolean`, `char`) which are not objects, mainly for performance reasons.
4. **Abstract Class vs Interface:**
   - *Interface:* Only abstract methods (before Java 8), multiple inheritance allowed (a class can implement multiple interfaces), variables are `public static final`.
   - *Abstract Class:* Can have both abstract and concrete methods, single inheritance only, can have instance variables.

### Java Basics
5. **Platform Independent?** Java code is compiled into bytecode (`.class` files), which is platform-neutral. The JVM (Java Virtual Machine), which is platform-specific, translates this bytecode into machine code. "Write Once, Run Anywhere".
6. **Access Modifiers:** `private` (accessible only within the class), `default` (within the same package), `protected` (same package + subclasses), `public` (accessible everywhere).
7. **Call Static members:** Using the Class name (e.g., `Math.max()`). You don't need to instantiate the class.
8. **Constructor:** A special method used to initialize objects. It has the same name as the class and no return type. Created using `public ClassName() {}`.
9. **== vs .equals():** `==` checks for reference equality (if both point to the same memory location). `.equals()` checks for value equality (if the actual content is the same, assuming it's overridden).
10. **Creating String:** Using string literal (`String s = "Hello";`) or using the `new` keyword (`String s = new String("Hello");`).
11. **String Pool:** A special memory area in the Heap. When creating a string literal, JVM checks the pool. If it exists, it returns the reference. If not, it creates it. This saves memory. Using `new` bypasses the pool and creates a new object in the general heap.

### Collections Framework
12. **What is it?** A unified architecture for storing and manipulating a group of objects (lists, sets, maps).
13. **Features:** Reduces programming effort (provides ready-to-use data structures), increases performance, and allows interoperability among unrelated APIs.
14. **Interfaces:** `Collection`, `List`, `Set`, `Queue`, `Map` (Map is not a true Collection but part of the framework).
15. **Internal working:** (e.g., `HashMap`) Uses an array of nodes (buckets). It calculates the hashcode of the key to find the bucket index. If there's a collision, it stores multiple nodes in a linked list (or a balanced tree in Java 8+) at that bucket.

### Exceptions
16. **Exception:** An unwanted or unexpected event occurring during execution that disrupts the normal flow of instructions.
17. **Hierarchy:** `Throwable` is the root. It splits into `Error` (e.g., OutOfMemory, not meant to be caught) and `Exception`.
18. **Runtime vs Compile-time:**
    - *Compile-time (Checked):* Checked by the compiler (e.g., `IOException`). Must be handled with try-catch or `throws`.
    - *Runtime (Unchecked):* Not checked at compile time (e.g., `NullPointerException`). Extend `RuntimeException`.
19. **Types:** Checked and Unchecked.
20. **final vs finally vs finalize:**
    - `final`: Keyword to make a variable constant, method non-overridable, or class non-inheritable.
    - `finally`: Block in exception handling that always executes regardless of whether an exception occurred (used for cleanup).
    - `finalize()`: A method called by the Garbage Collector before an object is destroyed (deprecated in modern Java).

### Functional Programming (Java 8)
21. **Functional Interfaces:** An interface with exactly one abstract method (can have multiple default/static methods). Annotated with `@FunctionalInterface`.
22. **Types:** `Predicate`, `Function`, `Consumer`, `Supplier`.
23. **Predicate Interface:** Takes one argument and returns a `boolean`. Used for filtering.
24. **Lambda Expression:** A short block of code which takes in parameters and returns a value. Example: `(a, b) -> a + b;`
25. **Method Reference:** Shorthand for a lambda expression calling a specific method. Example: `System.out::println`.
26. **Optional Class:** A container object which may or may not contain a non-null value. Helps avoid `NullPointerException`.
27. **Alternatives to Optional:** Checking for `null` explicitly, or throwing exceptions directly using methods like `orElseThrow()`.
28. **Streams in Java:** A sequence of elements supporting sequential and parallel aggregate operations. Used to process collections in a declarative way.
29. **Intermediate vs Terminal:**
    - *Intermediate:* Return a new Stream (e.g., `filter()`, `map()`). They are lazy.
    - *Terminal:* Produce a non-stream result (e.g., `collect()`, `count()`). They trigger the execution.
30. **filter():** Intermediate operation that selects elements based on a Predicate.
31. **reduce():** Terminal operation that performs a reduction on the elements, returning a single value (e.g., summing all numbers).
32. **map() vs flatMap():** `map()` transforms each element into another object (1-to-1). `flatMap()` transforms each element into a Stream of objects and then flattens them into a single stream (1-to-Many).
33. **List.of():** Introduced in Java 9. Creates an immutable list easily. Example: `List<String> list = List.of("A", "B");`

### Java 8 / 17 Features
34. **Java 8 Features:** Lambdas, Streams, Functional Interfaces, Optional, Default methods in interfaces, new Date/Time API.
35. **Why Java 8?** To bring functional programming capabilities to Java, make code more concise, and easily utilize multi-core processors (Parallel Streams).
36. **Java 17 Features:** Sealed Classes, Pattern Matching for switch, Records, Text Blocks (multi-line strings).
37. **Why Java 17?** It is an LTS (Long Term Support) release. It provides better performance, modern syntax (like Records which replace Lombok's `@Data`), and improved security.

### Spring Boot / Spring MVC
38. **Spring MVC:** A module in Spring framework implementing the Model-View-Controller design pattern for building web applications and REST APIs.
39. **@SpringBootApplication:** A convenience annotation that combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`.
40. **@ComponentScan:** Tells Spring to scan the current package and its sub-packages for Spring components (annotated with `@Component`, `@Service`, etc.) to register them as Beans.
41. **@Component vs @Service:** Both register a bean. `@Component` is generic. `@Service` is a specialization indicating that the class holds business logic.
42. **@Autowired:** Used for dependency injection. Spring automatically resolves and injects the required bean into the field, constructor, or setter.
43. **@Primary:** When multiple beans of the same type exist, `@Primary` tells Spring to inject this specific bean by default.
44. **@RequestBody:** Maps the incoming HTTP request body (JSON) to a Java object.
45. **@ResponseBody:** Tells a controller that the object returned is automatically serialized into JSON and passed back into the HTTP response body. (Implicitly included in `@RestController`).
46. **@Valid:** Used to trigger validation on an object (e.g., checking `@NotNull` constraints on a DTO).
47. **@RestController:** A combination of `@Controller` and `@ResponseBody`. Used to create RESTful web services.
48. **Lombok:** A library used to reduce boilerplate code (like getters, setters, constructors) using annotations like `@Data`, `@NoArgsConstructor`.

### JPA / Hibernate
49. **Hibernate:** An Object-Relational Mapping (ORM) framework for Java. It maps Java classes to database tables.
50. **Why Hibernate?** It saves developers from writing complex JDBC boilerplate and raw SQL queries, handles database dialect translations, and manages caching.
51. **Main uses of JPA:** Java Persistence API is the specification; Hibernate is the implementation. Used to manage relational data in Java applications seamlessly.

### REST APIs
52. **Types of REST APIs:** Based on HTTP methods: GET (read), POST (create), PUT (update entire resource), PATCH (partial update), DELETE (remove).
53. **ResponseEntity:** Represents the entire HTTP response, including status code, headers, and body. Gives full control over the response sent to the client.
54. **HTTP Status Codes:**
    - 200 OK (Success)
    - 201 Created
    - 400 Bad Request (Client error)
    - 401 Unauthorized (Missing/invalid token)
    - 403 Forbidden (Valid token, but lacking permissions)
    - 404 Not Found
    - 500 Internal Server Error (Backend crash)

### Microservices Concepts (Brief Reiteration)
55. **Load Balancer:** Distributes network traffic across multiple servers to ensure no single server bears too much demand.
56. **Service Discovery:** A mechanism (like Eureka) for services to find each other dynamically without hardcoded IPs.
57. **Feign Client:** A declarative REST client to make synchronous inter-service calls easier.
58. **RabbitMQ:** A message broker for asynchronous communication between services.
59. **Sync vs Async:** Sync blocks the thread waiting for a response (HTTP). Async sends a message and moves on without waiting (RabbitMQ).
60. **Authentication vs Authorization:** Authentication verifies identity (login). Authorization verifies access rights (roles).
