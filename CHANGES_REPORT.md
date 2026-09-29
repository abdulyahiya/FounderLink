# FounderLink — Audit, Bug Fixes & Changes Report

**Date of Audit**: September 29, 2026  
**Auditor**: Antigravity AI  
**Scope**: Full Stack Microservices Audit (Backend + Frontend + Infrastructure + Test Suites)

---

## 1. Executive Summary of Changes

During the system inspection and build verification of the **FounderLink** repository, 5 critical issues were detected that prevented the project from cleanly compiling, executing its test suites, and running in Docker. All issues have been resolved and verified with 100% build and test pass rates.

---

## 2. Detailed Breakdown of Issues & Fixes

### 🛠️ Change 1: Lombok Annotation Processor in `PaymentService`

- **Affected File**: [`PaymentService/pom.xml`](file:///Users/Learning/FounderLink/PaymentService/pom.xml#L100-L108)
- **Problem**:
  The `maven-compiler-plugin` configuration was missing `<annotationProcessorPaths>` with `org.projectlombok:lombok`. When `mvn compile` was executed, the Java compiler did not generate getters, setters, constructors, or `@Slf4j` logger fields for Lombok-annotated classes (`Payment`, `EmailService`, `CreateOrderRequest`). This caused compilation to fail with `cannot find symbol` errors across multiple files.
- **Modification**:
  Added the Lombok annotation processor path to the compiler plugin definition:
  ```xml
  <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
          <source>17</source>
          <target>17</target>
          <release>17</release>
          <annotationProcessorPaths>
              <path>
                  <groupId>org.projectlombok</groupId>
                  <artifactId>lombok</artifactId>
              </path>
          </annotationProcessorPaths>
      </configuration>
  </plugin>
  ```
- **Outcome**: `PaymentService` now compiles cleanly without errors.

---

### 🛠️ Change 2: Missing Mock Dependencies in `AuthServiceTest`

- **Affected File**: [`AuthService/src/test/java/com/capgemini/authservice/service/AuthServiceTest.java`](file:///Users/Learning/FounderLink/AuthService/src/test/java/com/capgemini/authservice/service/AuthServiceTest.java#L46-L51)
- **Problem**:
  The `AuthService` constructor was recently updated to inject `WelcomeEmailService` and `PasswordResetEmailService`. However, `AuthServiceTest.java` was not updated to declare `@Mock` instances for these two dependencies. When `@InjectMocks` instantiated `AuthService`, the fields remained `null`, causing `register_whenValidRequest_shouldReturnRegisterResponse` to throw a `NullPointerException`.
- **Modification**:
  Added missing `@Mock` fields to `AuthServiceTest`:
  ```java
  @Mock
  private WelcomeEmailService welcomeEmailService;

  @Mock
  private PasswordResetEmailService passwordResetEmailService;
  ```
- **Outcome**: All 8 unit tests in `AuthServiceTest` pass with 100% success rate.

---

### 🛠️ Change 3: Controller & Service Test Sync in `user-service`

- **Affected Files**:
  - [`user-service/src/test/java/com/capgemini/user/controller/UserProfileControllerTest.java`](file:///Users/Learning/FounderLink/user-service/src/test/java/com/capgemini/user/controller/UserProfileControllerTest.java#L106-L114)
  - [`user-service/src/test/java/com/capgemini/user/service/UserProfileServiceTest.java`](file:///Users/Learning/FounderLink/user-service/src/test/java/com/capgemini/user/service/UserProfileServiceTest.java#L161-L177)
- **Problem**:
  1. `UserProfileController.java` was modified to catch missing profile exceptions and return an empty profile shell (`HTTP 200` with `userId`) so that the frontend form can render blank fields gracefully. However, `UserProfileControllerTest` still expected `HTTP 404 Not Found`.
  2. `UserProfileServiceImpl.java` was modified to automatically create a new profile on first save if not already present. However, `UserProfileServiceTest` still asserted that `updateProfile` threw `ResourceNotFoundException`.
- **Modification**:
  1. Updated `UserProfileControllerTest.java` to assert `HTTP 200 OK` with `{ "userId": 99 }`.
  2. Updated `UserProfileServiceTest.java` to test the automatic profile creation / upsert behavior on first save.
- **Outcome**: All 14 unit and integration tests in `user-service` pass cleanly.

---

### 🛠️ Change 4: EmailService Test Fixtures & MimeMessage Mocking in `PaymentService`

- **Affected File**: [`PaymentService/src/test/java/com/capgemini/payment/service/EmailServiceTest.java`](file:///Users/Learning/FounderLink/PaymentService/src/test/java/com/capgemini/payment/service/EmailServiceTest.java)
- **Problem**:
  1. `EmailService` was upgraded to send rich, responsive HTML emails using `MimeMessage` via `mailSender.createMimeMessage()`. However, `EmailServiceTest` was still asserting against `SimpleMailMessage`.
  2. The `buildPayment()` helper fixture did not populate `investorEmail` and `founderEmail`, causing `EmailService` to bail out early with `"Investor email not available, skipping email"`.
- **Modification**:
  1. Updated `EmailServiceTest` to mock `MimeMessage` and verify `mailSender.send(any(MimeMessage.class))`.
  2. Populated `investorEmail` and `founderEmail` in test fixtures.
- **Outcome**: All 41 tests in `PaymentService` pass with zero failures.

---

### 🛠️ Change 5: SonarQube DB Healthcheck in `docker-compose.yml`

- **Affected File**: [`docker-compose.yml`](file:///Users/Learning/FounderLink/docker-compose.yml#L72)
- **Problem**:
  `sonarqube-db` was configured with `POSTGRES_DB: sonarqube`, but the healthcheck used `pg_isready -U sonar`. By default, `pg_isready` attempts to connect to a database with the same name as the user (`sonar`), which did not exist. This caused Docker logs to repeatedly output `FATAL: database "sonar" does not exist`.
- **Modification**:
  Updated the healthcheck command to explicitly specify the target database:
  ```yaml
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U sonar -d sonarqube"]
    interval: 10s
    timeout: 5s
    retries: 10
  ```
- **Outcome**: `sonarqube-db` becomes healthy immediately without connection errors.

---

## 3. Build & Test Verification Results

### Backend (`mvn test` across all modules)
```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for FounderLink 0.0.1-SNAPSHOT:
[INFO] 
[INFO] EurekaServer ....................................... SUCCESS [  3.776 s]
[INFO] Config-Server ...................................... SUCCESS [  4.878 s]
[INFO] api-gateway ........................................ SUCCESS [  6.897 s]
[INFO] AuthService ........................................ SUCCESS [  6.301 s]
[INFO] user-service ....................................... SUCCESS [  6.143 s]
[INFO] startup-service .................................... SUCCESS [  5.882 s]
[INFO] InvestmentService .................................. SUCCESS [  5.546 s]
[INFO] TeamService ........................................ SUCCESS [  5.674 s]
[INFO] MessagingService ................................... SUCCESS [  5.115 s]
[INFO] NotificationService ................................ SUCCESS [  5.089 s]
[INFO] PaymentService ..................................... SUCCESS [  5.136 s]
[INFO] FounderLink ........................................ SUCCESS [  0.000 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time: 01:00 min
[INFO] ------------------------------------------------------------------------
```

### Frontend (`npm run build` in `founderlink-final`)
```
> founderlink-frontend-ts@0.2.0 build
> tsc && vite build

✓ 1998 modules transformed.
✓ built in 2.44s
```
