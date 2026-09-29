# 🚀 FounderLink — Full-Stack Startup & Investor Platform

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-19.0-61DAFB?logo=react" alt="React 19" />
  <img src="https://img.shields.io/badge/TypeScript-5.0-blue?logo=typescript" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker" alt="Docker" />
  <img src="https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/RabbitMQ-3.13-FF6600?logo=rabbitmq" alt="RabbitMQ" />
  <img src="https://img.shields.io/badge/Redis-Cache-DC382D?logo=redis" alt="Redis" />
  <img src="https://img.shields.io/badge/Swagger-OpenAPI%203-85EA2D?logo=swagger" alt="Swagger" />
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License" />
</p>

**FounderLink** is an enterprise-grade, event-driven microservices platform connecting innovative startup founders with venture capital and angel investors. The platform supports startup management, team collaboration, investment proposals, Razorpay payment processing, real-time messaging, and multi-channel notifications.

---

## 🏛️ System Architecture

```
                                  ┌─────────────────────────────┐
                                  │   React 19 Frontend (3001)  │
                                  └──────────────┬──────────────┘
                                                 │
                                                 ▼
                                  ┌─────────────────────────────┐
                                  │   Spring Cloud API Gateway  │
                                  │         (Port 8080)         │
                                  └──────────────┬──────────────┘
                                                 │
       ┌──────────────────┬──────────────────────┼──────────────────────┬──────────────────┐
       ▼                  ▼                      ▼                      ▼                  ▼
┌──────────────┐   ┌──────────────┐       ┌──────────────┐       ┌──────────────┐   ┌──────────────┐
│ Auth Service │   │ User Service │  ...  │StartupService│  ...  │PaymentService│   │Eureka Server │
│ (Port 8081)  │   │ (Port 8082)  │       │ (Port 8083)  │       │ (Port 8089)  │   │ (Port 8761)  │
└──────┬───────┘   └──────┬───────┘       └──────┬───────┘       └──────┬───────┘   └──────────────┘
       │                  │                      │                      │
       └──────────────────┴──────────────────────┼──────────────────────┘
                                                 │
                   ┌─────────────────────────────┴─────────────────────────────┐
                   ▼                                                           ▼
    ┌─────────────────────────────┐                             ┌─────────────────────────────┐
    │     RabbitMQ Message Bus    │                             │     PostgreSQL Databases    │
    │         (Port 5672)         │                             │         (Port 5433)         │
    └─────────────────────────────┘                             └─────────────────────────────┘
```

---

## ✨ Key Features

- **🔐 Centralized Authentication & Security**: Stateless JWT authentication with refresh token rotation and role-based access (`ROLE_FOUNDER`, `ROLE_INVESTOR`, `ROLE_COFOUNDER`, `ROLE_ADMIN`).
- **🚀 Startup Lifecycle**: Startup creation, stage tracking (Idea, MVP, Seed, Series A), financial metrics, and follower community.
- **💼 Investor Deals**: Investment proposals, multi-tier status workflows, deal reviews, and investment analytics.
- **💳 Razorpay Payment Gateway**: Seamless checkout, instant webhook signature validation, idempotency, and automated receipt emails.
- **👥 Team Collaboration**: Role-based team member invitations and management.
- **💬 Real-Time Messaging**: Direct messaging and conversation tracking between founders and investors.
- **🔔 Asynchronous Event Notifications**: RabbitMQ event-driven notifications with Gmail SMTP email delivery.
- **📊 Observability & Monitoring**: Distributed tracing with Zipkin, metrics aggregation via Prometheus, and customizable dashboards in Grafana.
- **📑 Unified Swagger UI**: Consolidated OpenAPI documentation for all 9 microservices in a single gateway portal.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | React 19, TypeScript, Vite, Tailwind CSS, Lucide Icons, Redux Toolkit |
| **Backend Framework** | Java 17, Spring Boot 3.3.4, Spring Cloud 2023.0.3 |
| **Service Mesh** | Spring Cloud Gateway, Netflix Eureka Registry, Spring Cloud Config |
| **Database & Cache** | PostgreSQL 15, Spring Data JPA, Hibernate ORM, Redis 7 |
| **Asynchronous Messaging**| RabbitMQ (AMQP), Spring AMQP |
| **Payment & Mail** | Razorpay Java SDK, JavaMailSender, Thymeleaf Templates |
| **Observability** | Micrometer, Brave, Prometheus, Grafana, Zipkin |
| **Containerization** | Docker, Docker Compose, Multi-Stage BuildKit Caching |

---

## ⚡ Quick Start (Docker Compose)

### 1. Prerequisites
- [Docker](https://docs.docker.com/get-docker/) & [Docker Compose](https://docs.docker.com/compose/) installed.

### 2. Clone and Run
```bash
# Clone the repository
git clone https://github.com/abdulyahiya/FounderLink.git
cd FounderLink

# Start the complete stack
DOCKER_BUILDKIT=1 docker compose up -d --build
```

---

## 🌐 Live Port Map & Endpoints

| Service / Tool | Port | URL | Description |
| :--- | :--- | :--- | :--- |
| **Frontend UI** | `3001` | [http://localhost:3001](http://localhost:3001) | React 19 Client Web App |
| **API Gateway** | `8080` | [http://localhost:8080](http://localhost:8080) | Reverse Proxy & Routing |
| **Unified Swagger UI**| `8080` | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | Interactive API Documentation |
| **Eureka Registry** | `8761` | [http://localhost:8761](http://localhost:8761) | Service Discovery Dashboard |
| **Config Server** | `8888` | [http://localhost:8888](http://localhost:8888) | Spring Cloud Config Server |
| **RabbitMQ Management**| `15672` | [http://localhost:15672](http://localhost:15672) | Broker UI (`guest` / `guest`) |
| **Grafana Dashboards** | `3002` | [http://localhost:3002](http://localhost:3002) | Metrics & System Monitoring |
| **Prometheus** | `9090` | [http://localhost:9090](http://localhost:9090) | Time-Series Metrics Server |
| **Zipkin Tracing** | `9412` | [http://localhost:9412](http://localhost:9412) | Distributed Tracing Dashboard |
| **PostgreSQL** | `5433` | `localhost:5433` | Database Server |
| **Redis Cache** | `6380` | `localhost:6380` | In-Memory Cache |

---

## 🔑 Default Admin Account

- **Email**: `admin@founderlink.com`
- **Password**: `Admin@123`
- **Role**: `ROLE_ADMIN`

---

## 📂 Microservices Architecture

- [`AuthService`](file:///Users/Learning/FounderLink/AuthService) (`8081`): User registration, JWT login, authentication, and password reset.
- [`user-service`](file:///Users/Learning/FounderLink/user-service) (`8082`): User profiles, bios, skills, and portfolio data.
- [`startup-service`](file:///Users/Learning/FounderLink/startup-service) (`8083`): Startup pitch details, stage management, and follower relationships.
- [`InvestmentService`](file:///Users/Learning/FounderLink/InvestmentService) (`8084`): Investment proposals, status tracking, and deal flows.
- [`TeamService`](file:///Users/Learning/FounderLink/TeamService) (`8085`): Team collaboration, invitations, and role assignments.
- [`MessagingService`](file:///Users/Learning/FounderLink/MessagingService) (`8086`): Direct messaging between founders and investors.
- [`NotificationService`](file:///Users/Learning/FounderLink/NotificationService) (`8087`): Multi-channel event notifications and automated emails.
- [`PaymentService`](file:///Users/Learning/FounderLink/PaymentService) (`8089`): Razorpay integration, payment verification, and transaction history.
- [`api-gateway`](file:///Users/Learning/FounderLink/api-gateway) (`8080`): Reactive gateway with unified Swagger UI aggregation.

---

## 📄 Documentation

- [Complete High-Level & Low-Level Design (HLD/LLD)](./PROJECT_DOCUMENTATION_HLD_LLD.md)
- [Audit & Changes Report](./CHANGES_REPORT.md)
- [Backend Services Guide](./BACKEND_README.md)

---

## 📜 License

This project is licensed under the MIT License.
