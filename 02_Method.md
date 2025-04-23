=== Architecture

Backend: Java 21 with Spring Boot, leveraging WebFlux for reactive processing and Hexagonal Architecture for modularity.

Database:

PostgreSQL for ACID-compliant transactional data.

MongoDB for non-relational data such as user preferences.

Redis for caching frequently accessed data.

Messaging:

RabbitMQ for asynchronous transaction handling.

WebSockets for real-time notifications.

Payment Integration:

Mocked integrations with payment gateways initially.

CurrencyLayer for real-time exchange rates.

Security:

AES-256 encryption for sensitive data.

OAuth2 and JWT for authentication.

Rate limiting using Spring Cloud Gateway.

=== Backend Modules

The backend system will be modularized into distinct services for scalability and maintainability:

User Service

Handles user authentication, registration, and profile management.

Implements OAuth2 and JWT for secure authentication.

Stores user details in PostgreSQL and user preferences in MongoDB.

Transaction Service

Manages money transfers and transaction processing.

Ensures compliance with financial regulations (AML, PCI-DSS).

Uses RabbitMQ for asynchronous processing and Redis for caching transaction history.

Payment Gateway Service

Integrates with external payment providers (e.g., Stripe, PayPal, CurrencyLayer).

Supports real-time currency conversion.

Implements error handling and retries with Resilience4j.

Currency Exchange Service

Retrieves and caches live exchange rates.

Uses external APIs (CurrencyLayer) for accuracy.

Caches results in Redis for faster lookup.

Notification Service

Sends real-time notifications via WebSockets.

Handles email notifications using AWS SES / SendGrid.

Uses Thymeleaf for dynamic email templates.

Audit & Logging Service

Logs all transactions and system activities.

Uses ELK Stack (Elasticsearch, Logstash, Kibana) for log analysis.

Ensures regulatory compliance through audit trails.

=== Frontend

React with TypeScript for building a responsive and interactive UI.

GraphQL APIs to optimize data fetching.

=== Cloud & Deployment

Cloud Provider: AWS/Azure/GCP.

Container Orchestration: Kubernetes (EKS, AKS, GKE).

CI/CD: GitHub Actions for automated builds, testing, and deployments.

Monitoring:

Prometheus + Grafana for performance tracking.

Sentry/New Relic for error monitoring.

=== Account Confirmation & Notifications

JavaMailSender for email-based account confirmations.

AWS SES / SendGrid for scalable email delivery.

Thymeleaf for dynamic email templates.

