# Swiftly Global Design Project

## Gateway Service

The gateway service acts as the entry point for all requests to the system, providing:

- Service discovery and routing
- Rate limiting
- Health checking
- Comprehensive metrics system

### Metrics Implementation

The metrics system includes:

1. **Gateway Metrics**
   - Route request tracking
   - Response time monitoring
   - Response size tracking
   - Status code distribution
   - Error tracking
   - Circuit breaker state monitoring

2. **Circuit Breaker Metrics**
   - State transitions
   - Failure rates
   - Success rates
   - Latency tracking
   - Capacity monitoring
   - Health status

3. **Infrastructure Metrics**
   - JVM memory usage
   - CPU usage
   - Thread pool metrics
   - Connection pool metrics
   - Garbage collection metrics

### Monitoring Configuration

The service is configured with Prometheus and Grafana for:

- Real-time metric collection
- Interactive dashboards
- Automated alerting
- Historical data analysis

### Getting Started

1. Clone the repository
2. Build with Maven:
   ```bash
   ./mvnw clean install
   ```
3. Run the application:
   ```bash
   ./mvnw quarkus:dev
   ```

### Monitoring Endpoints

- Metrics: `/metrics`
- Health: `/health`
- Routes: `/routes`
- Rate Limits: `/rate-limits`