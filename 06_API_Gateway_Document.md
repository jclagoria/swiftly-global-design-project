# 1. Overview

Design a robust, secure, and observable API Gateway using Quarkus as the entry point to route and manage traffic for backend microservices. This gateway will handle dynamic service discovery via Eureka, enforce JWT-based authentication, expose health and metrics endpoints, and incorporate resilience, load balancing, and security best practices.

## 2. Core Characteristics

## 2.1 Service Discovery & Dynamic Routing

- Integrate with Eureka to automatically discover downstream service instances.

- Use SmallRye Stork (or the Quarkus Eureka extension) combined with the Quarkus Reactive Routes extension to proxy client requests to the correct service URL at runtime.

## 2.2 Authentication & Authorization

- Enforce validation of a JSON Web Token in the Authorization: Bearer <token> header for all routes.

- Leverage the quarkus-smallrye-jwt extension to verify token signatures, check expiration, and extract claims.

## 2.3 Health & Metrics

- Expose liveness (/health/live) and readiness (/health/ready) probes under a unified /health path using quarkus-smallrye-health.

- Provide Prometheus-compatible metrics under /metrics via quarkus-smallrye-metrics.

## 2.4 Resilience & Fault Tolerance

- Implement circuit breakers, timeouts, and retry policies using quarkus-smallrye-fault-tolerance to prevent cascading failures.

## 2.5 Client-Side Load Balancing
    
- Distribute requests across multiple service instances (e.g., round-robin or weighted) via SmallRye Stork’s load-balancer configuration.

## 2.6 Logging, Tracing & Correlation

- Propagate correlation IDs on each request.

- Integrate with OpenTelemetry (via quarkus-opentelemetry) and export traces to Jaeger or another tracing backend.

- Centralize HTTP access logs with structured format (JSON).

## 2.7 Rate Limiting & Throttling

- Enforce per-client or per-route rate-limit policies to protect downstream services under high load.

## 2.8 Security & CORS

- Terminate TLS at the gateway.

- Define CORS policies (quarkus.http.cors) to control allowed origins, methods, and headers.

- Inject security headers (HSTS, CSP) through HTTP filters.

## 2.9 Request/Response Transformation

- Support header and path rewriting, as well as lightweight JSON/XML payload transformations using Quarkus HTTP filters.

## 2.10 Hexagonal Architecture & SOLID Principles

- Structure the gateway using Hexagonal (Ports & Adapters) to decouple domain logic from infrastructure concerns.

- Apply SOLID principles (Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion) to promote modularity, testability, and maintainability.

3. Standard & Additional Endpoints

Method          Endpoint            Description

GET             /health             Aggregated liveness/readiness checks for the gateway and downstream dependencies.
GET             /status             System-wide status overview, including service registry connectivity.
GET             /rate-limits        Display current rate-limit usage and configuration.
GET             /metrics            Prometheus-compatible metrics (MicroProfile Metrics) for gateway performance.
GET             /routes             List active dynamic routes, target services, and metadata.
GET             /openapi            Serve the aggregated OpenAPI specification for all routed APIs.
GET             /docs               Host Swagger UI or ReDoc for interactive API exploration.
POST            /rate-limits/reset  (Admin-only) Reset all rate-limit counters.

# 4. Configuration & Dependencies
```properties
## HTTP Settings
quarkus.http.port=8080
quarkus.http.auth.proactive=true

## Eureka Service Discovery
quarkus.eureka.enabled=true
quarkus.eureka.client.service-url.defaultZone=http://eureka:8761/eureka

## SmallRye Stork for Dynamic Routing & Load Balancing
stork.gateway-service.service-discovery=eureka
stork.gateway-service.load-balancer=round-robin

## JWT Validation
mp.jwt.verify.publickey.location=classpath:publicKey.pem

## Health & Metrics Paths
quarkus.smallrye-health.root-path=/health
quarkus.smallrye-metrics.path=/metrics
```

## Key Quarkus Extensions

- quarkus-smallrye-reactive-routes
- io.smallrye.stork:smallrye-stork-service-discovery-eureka (or com.github.fmcejudo:quarkus-eureka)
- quarkus-smallrye-jwt
- quarkus-smallrye-health
- quarkus-smallrye-metrics
- quarkus-smallrye-fault-tolerance
- quarkus-opentelemetry

#5. Further Recommendations

Circuit Breaker Fallbacks: Define user-friendly fallback responses for critical routes to degrade gracefully.

Distributed Tracing: Ensure full trace context propagation for end-to-end visibility.

CORS & Security Headers: Regularly review and tighten CORS configurations and HTTP security headers.

Global Error Handling: Centralize exception handling to standardize error payloads and logging.

Admin Dashboard & Alerts: Provide an operational dashboard and alerting for gateway health and anomalies.