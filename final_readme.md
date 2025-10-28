# Standing Order Processor - Setup Guide

## Quick Start

### Step 1: Run the Setup Script

```bash
# Download and run the setup script
chmod +x setup-project.sh
./setup-project.sh
```

This will create the complete project structure with all files.

### Step 2: Start Infrastructure

```bash
cd standing-order-processor

# Start all services (Temporal, PostgreSQL, Kafka)
docker-compose up -d

# Verify all services are running
docker-compose ps

# Check logs if needed
docker-compose logs -f
```

Wait for all services to be healthy (30-60 seconds).

### Step 3: Build the Project

#### Option A: Using Bazel (Recommended)

```bash
# Build all projects
bazel build //...

# Run tests
bazel test //...

# Run only unit tests (exclude integration tests)
bazel test --test_tag_filters=-integration //...

# Run integration tests
bazel test --test_tag_filters=integration //...

# Build the application binary
bazel build //activities-project:standing_order_app
```

#### Option B: Using Maven

```bash
# Build workflow project first
cd workflow-project
mvn clean install

# Build activities project
cd ../activities-project
mvn clean install

# Run tests
mvn test

# Package application
mvn package
```

### Step 4: Run the Application

#### Using Bazel:

```bash
# Run from project root
bazel run //activities-project:standing_order_app
```

#### Using Maven:

```bash
cd activities-project
mvn spring-boot:run
```

#### Using JAR:

```bash
cd activities-project
java -jar target/standing-order-activities-1.0.0-SNAPSHOT.jar
```

### Step 5: Verify Setup

1. **Check Application Logs:**
   ```bash
   # You should see:
   # - "Started StandingOrderApplication"
   # - "WorkerFactory started"
   # - Temporal worker registration messages
   ```

2. **Access Temporal UI:**
   - Open browser: http://localhost:8080
   - Navigate to Workflows
   - You should see the worker registered

3. **Verify Database:**
   ```bash
   docker exec -it standing-order-db psql -U postgres -d standing_orders
   
   # Run queries
   SELECT * FROM standing_order;
   \q
   ```

4. **Check Kafka:**
   ```bash
   # List topics
   docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
   
   # Should see: payment-orders-topic
   ```

## Manual Workflow Trigger

To manually trigger a workflow execution:

```bash
# Using Temporal CLI (if installed)
temporal workflow start \
  --task-queue standing-order-task-queue \
  --type StandingOrderWorkflow \
  --workflow-id manual-test-$(date +%s) \
  --input '{"processingDate":"2025-01-15","status":"ACTIVE"}'
```

Or create a test REST endpoint in your application.

## Testing

### Run All Tests

```bash
# Bazel
bazel test //...

# Maven
cd workflow-project && mvn test
cd ../activities-project && mvn test
```

### Run Specific Test Suites

```bash
# Workflow tests only
bazel test //workflow-project:workflow_test

# Activities tests only
bazel test //activities-project:activities_test

# Integration tests only
bazel test //activities-project:integration_test
```

### Test Coverage

The project includes tests for:
- ✅ Successful workflow execution
- ✅ Workflow idempotency (duplicate detection)
- ✅ Empty eligible orders list
- ✅ Partial failures (some orders succeed, some fail)
- ✅ Failures at different stages (getPayload, publishToKafka, updateNextExecutionDate)
- ✅ Retry logic with exponential backoff
- ✅ Activity-level error handling
- ✅ End-to-end integration tests

## Project Structure

```
standing-order-processor/
├── WORKSPACE                     # Bazel workspace
├── .bazelrc                      # Bazel configuration
├── docker-compose.yml            # Infrastructure services
├── init-scripts/
│   └── init.sql                  # Database initialization
├── workflow-project/             # Temporal Workflow Definitions
│   ├── BUILD.bazel
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/payment/workflow/
│       │   ├── StandingOrderWorkflow.java
│       │   ├── StandingOrderWorkflowImpl.java
│       │   └── model/
│       │       ├── StandingOrderFilterCriteria.java
│       │       ├── StandingOrder.java
│       │       ├── PaymentPayload.java
│       │       ├── WorkflowExecutionResult.java
│       │       └── StandingOrderProcessingResult.java
│       └── test/java/
└── activities-project/           # Temporal Activities Implementation
    ├── BUILD.bazel
    ├── pom.xml
    └── src/
        ├── main/java/com/payment/
        │   ├── StandingOrderApplication.java
        │   ├── activities/
        │   │   ├── StandingOrderActivities.java
        │   │   ├── StandingOrderActivitiesImpl.java
        │   │   ├── repository/
        │   │   │   └── StandingOrderRepository.java
        │   │   └── service/
        │   │       ├── WorkflowRegistryService.java
        │   │       ├── StandingOrderService.java
        │   │       └── KafkaPublisherService.java
        │   ├── config/
        │   │   ├── TemporalConfig.java
        │   │   └── KafkaConfig.java
        │   └── scheduler/
        │       └── StandingOrderScheduler.java
        ├── resources/
        │   ├── application.yml
        │   └── db/migration/
        │       └── V1__create_standing_order_table.sql
        └── test/java/
```

## Configuration

### Temporal Configuration

Edit `activities-project/src/main/resources/application.yml`:

```yaml
temporal:
  server:
    host: localhost
    port: 7233
  namespace: default
  task-queue: standing-order-task-queue
```

### Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/standing_orders
    username: postgres
    password: postgres
```

### Kafka Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

kafka:
  topic:
    payment-orders: payment-orders-topic
```

### Scheduler Configuration

```yaml
standing-order:
  schedule:
    cron: "0 0 2 * * ?"  # Daily at 2 AM
```

Change the cron expression to adjust execution schedule:
- Every minute: `"0 * * * * ?"`
- Every 5 minutes: `"0 */5 * * * ?"`
- Hourly: `"0 0 * * * ?"`
- Daily at 3 AM: `"0 0 3 * * ?"`

## Workflow Execution Flow

```
1. Scheduler triggers workflow (or manual trigger)
   ↓
2. Check if workflow already running (idempotency)
   ↓
3. Get eligible standing orders from database
   ↓
4. For each standing order:
   a. Get payment payload
   b. Publish to Kafka
   c. Update next execution date
   ↓
5. Return execution result with success/failure counts
```

## Monitoring

### Temporal UI

Access: http://localhost:8080

Features:
- View all workflow executions
- Inspect workflow history
- Debug failed workflows
- View activity execution details
- Replay workflows

### Application Logs

```bash
# View application logs
tail -f activities-project/logs/application.log

# View specific workflow execution
grep "workflow-id-123" activities-project/logs/application.log
```

### Database Queries

```sql
-- View all standing orders
SELECT * FROM standing_order;

-- View active orders due for processing
SELECT * FROM standing_order 
WHERE status = 'ACTIVE' 
  AND next_execution_date <= CURRENT_DATE;

-- View orders by entity
SELECT entity_code, COUNT(*) 
FROM standing_order 
GROUP BY entity_code;
```

### Kafka Monitoring

```bash
# List topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Consume messages
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-orders-topic \
  --from-beginning

# Check consumer groups
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

## Troubleshooting

### Issue: "Connection refused" to Temporal Server

**Solution:**
```bash
# Check Temporal is running
docker-compose ps temporal

# Restart Temporal
docker-compose restart temporal

# View logs
docker-compose logs temporal
```

### Issue: Database connection failed

**Solution:**
```bash
# Check PostgreSQL is running
docker-compose ps postgresql

# Connect to database manually
docker exec -it standing-order-db psql -U postgres -d standing_orders

# Check tables exist
\dt
```

### Issue: Kafka publishing fails

**Solution:**
```bash
# Check Kafka is running
docker-compose ps kafka

# Verify topic exists
docker exec -it kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092

# Create topic manually if needed
docker exec -it kafka kafka-topics \
  --create \
  --topic payment-orders-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

### Issue: Bazel build fails

**Solution:**
```bash
# Clean Bazel cache
bazel clean --expunge

# Rebuild
bazel build //...

# Check Java version
java -version  # Should be 17+
```

### Issue: Maven build fails with "Cannot resolve dependency"

**Solution:**
```bash
# Build workflow project first
cd workflow-project
mvn clean install -U

# Then build activities project
cd ../activities-project
mvn clean install -U
```

### Issue: Worker not registering with Temporal

**Solution:**
1. Check application.yml has correct Temporal server settings
2. Verify task queue name matches in config and workflow
3. Check logs for registration errors
4. Ensure Temporal server is reachable

## Advanced Usage

### Custom Workflow Parameters

Modify the scheduler to pass custom parameters:

```java
StandingOrderFilterCriteria criteria = new StandingOrderFilterCriteria();
criteria.setProcessingDate(LocalDate.now());
criteria.setEntityCode("ENTITY001");  // Filter by specific entity
criteria.setStatus("ACTIVE");
criteria.setIsBackDated(false);
```

### Adding New Activities

1. Define activity interface in `StandingOrderActivities.java`
2. Implement in `StandingOrderActivitiesImpl.java`
3. Call from workflow in `StandingOrderWorkflowImpl.java`
4. Add tests

### Changing Retry Policy

Edit `StandingOrderWorkflowImpl.java`:

```java
ActivityOptions options = ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofMinutes(10))  // Increase timeout
    .setRetryOptions(RetryOptions.newBuilder()
        .setInitialInterval(Duration.ofSeconds(2))    // Slower initial retry
        .setMaximumInterval(Duration.ofSeconds(30))   // Higher max interval
        .setBackoffCoefficient(3.0)                   // Faster backoff
        .setMaximumAttempts(5)                        // More attempts
        .build())
    .build();
```

## Production Deployment

### Checklist

- [ ] Update database credentials
- [ ] Configure production Temporal server
- [ ] Set up production Kafka cluster
- [ ] Configure proper logging (ELK, Splunk, etc.)
- [ ] Set up monitoring (Prometheus, Grafana)
- [ ] Configure alerts for failures
- [ ] Set up database backups
- [ ] Review and adjust retry policies
- [ ] Load test the system
- [ ] Set up CI/CD pipeline
- [ ] Configure proper authentication/authorization

### Environment Variables

```bash
export TEMPORAL_HOST=temporal.production.com
export TEMPORAL_PORT=7233
export DB_URL=jdbc:postgresql://prod-db:5432/standing_orders
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
export KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092
```

### Docker Deployment

```bash
# Build Docker image
docker build -t standing-order-processor:latest .

# Run container
docker run -d \
  --name standing-order-processor \
  -e TEMPORAL_HOST=temporal.prod.com \
  -e DB_URL=jdbc:postgresql://prod-db:5432/standing_orders \
  -p 8081:8081 \
  standing-order-processor:latest
```

## Performance Tuning

### Database Indexing

Ensure indexes exist on frequently queried columns:

```sql
CREATE INDEX idx_execution_date_status 
ON standing_order(next_execution_date, status);

CREATE INDEX idx_entity_code 
ON standing_order(entity_code);
```

### Kafka Partitioning

Increase partitions for better throughput:

```bash
docker exec -it kafka kafka-topics \
  --alter \
  --topic payment-orders-topic \
  --partitions 10 \
  --bootstrap-server localhost:9092
```

### Temporal Worker Tuning

Adjust worker concurrency in `TemporalConfig.java`:

```java
Worker worker = workerFactory.newWorker(taskQueue, 
    WorkerOptions.newBuilder()
        .setMaxConcurrentActivityExecutionSize(100)
        .setMaxConcurrentWorkflowTaskExecutionSize(50)
        .build()
);
```

## Support

For issues or questions:
- Check the Troubleshooting section above
- Review Temporal documentation: https://docs.temporal.io
- Check application logs for detailed error messages
- Open an issue in the project repository

## License

MIT License - see LICENSE file for details