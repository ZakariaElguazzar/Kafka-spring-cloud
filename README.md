# Kafka & Spring Cloud Streams - Real-Time Data Analytics

[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?style=flat&logo=apache-kafka&logoColor=white)](https://kafka.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)

This project demonstrates a complete implementation of Apache Kafka with Spring Cloud Streams for real-time data processing and analytics. It includes multiple microservices for producing, consuming, and analyzing streaming data.

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Part 1: Kafka Installation & Setup](#part-1-kafka-installation--setup)
- [Part 2: Docker Setup](#part-2-docker-setup)
- [Part 3: Spring Cloud Streams Services](#part-3-spring-cloud-streams-services)
- [Getting Started](#getting-started)
- [Testing](#testing)
- [Video Tutorial](#video-tutorial)

## 🎯 Project Overview

This project implements a distributed streaming platform using Apache Kafka and Spring Cloud Streams. It includes:

- **Producer Service**: REST API for publishing messages to Kafka topics
- **Consumer Service**: Service for consuming and processing Kafka messages
- **Supplier Service**: Automated data generation service
- **Stream Analytics Service**: Real-time data processing with Kafka Streams
- **Web Dashboard**: Real-time visualization of analytics results

## 🏗️ Architecture

```
┌─────────────────┐
│  REST Client    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐       ┌──────────────┐
│  Producer API   │──────▶│    Kafka     │
└─────────────────┘       │   Broker     │
                          └──────┬───────┘
┌─────────────────┐              │
│    Supplier     │──────────────┤
└─────────────────┘              │
                                 │
         ┌───────────────────────┼───────────────────┐
         ▼                       ▼                   ▼
┌─────────────────┐    ┌─────────────────┐  ┌──────────────┐
│    Consumer     │    │ Stream Analytics │  │  Dashboard   │
└─────────────────┘    └─────────────────┘  └──────────────┘
```

## 📦 Prerequisites

- **Java**: JDK 17 or higher
- **Maven**: 3.6+
- **Docker & Docker Compose**: Latest version
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code

## 🚀 Part 1: Kafka Installation & Setup

### Step 1: Download Kafka

```bash
# Download Kafka from Apache website
wget https://downloads.apache.org/kafka/3.6.0/kafka_2.13-3.6.0.tgz

# Extract the archive
tar -xzf kafka_2.13-3.6.0.tgz
cd kafka_2.13-3.6.0
```

### Step 2: Start Zookeeper

```bash
# Start Zookeeper server
bin/zookeeper-server-start.sh config/zookeeper.properties
```

### Step 3: Start Kafka Server

```bash
# In a new terminal, start Kafka broker
bin/kafka-server-start.sh config/server.properties
```

### Step 4: Test with Console Tools

**Create a topic:**
```bash
bin/kafka-topics.sh --create --topic test-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

**Start a console producer:**
```bash
bin/kafka-console-producer.sh --topic test-topic --bootstrap-server localhost:9092
```

**Start a console consumer:**
```bash
bin/kafka-console-consumer.sh --topic test-topic --from-beginning --bootstrap-server localhost:9092
```

## 🐳 Part 2: Docker Setup

### Step 1: Create `docker-compose.yml`

```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    hostname: zookeeper
    container_name: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  broker:
    image: confluentinc/cp-kafka:7.5.0
    hostname: broker
    container_name: broker
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "9101:9101"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_JMX_PORT: 9101
      KAFKA_JMX_HOSTNAME: localhost
```

### Step 2: Start Docker Containers

```bash
# Start services
docker-compose up -d

# Check running containers
docker-compose ps

# View logs
docker-compose logs -f
```

### Step 3: Test with Docker

```bash
# Start producer
docker exec -it broker kafka-console-producer --topic test-topic --bootstrap-server localhost:9092

# Start consumer (in another terminal)
docker exec -it broker kafka-console-consumer --topic test-topic --from-beginning --bootstrap-server localhost:9092
```

## ⚙️ Part 3: Spring Cloud Streams Services

### Service 1: Producer (REST Controller)

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-kafka</artifactId>
</dependency>
```

**Controller Example:**
```java
@RestController
@RequestMapping("/api/messages")
public class ProducerController {
    
    @Autowired
    private StreamBridge streamBridge;
    
    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody Message message) {
        streamBridge.send("producer-out-0", message);
        return ResponseEntity.ok("Message published successfully");
    }
}
```

**Configuration (`application.yml`):**
```yaml
spring:
  cloud:
    stream:
      bindings:
        producer-out-0:
          destination: input-topic
      kafka:
        binder:
          brokers: localhost:9092
```

### Service 2: Consumer

**Consumer Function:**
```java
@Configuration
public class ConsumerService {
    
    @Bean
    public Consumer<Message> consumer() {
        return message -> {
            System.out.println("Received: " + message);
            // Process message
        };
    }
}
```

**Configuration:**
```yaml
spring:
  cloud:
    stream:
      bindings:
        consumer-in-0:
          destination: input-topic
          group: consumer-group
```

### Service 3: Supplier (Auto-Generation)

**Supplier Function:**
```java
@Configuration
public class SupplierService {
    
    @Bean
    public Supplier<Message> supplier() {
        return () -> {
            Message message = new Message();
            message.setTimestamp(System.currentTimeMillis());
            message.setData(generateRandomData());
            return message;
        };
    }
}
```

**Configuration:**
```yaml
spring:
  cloud:
    stream:
      bindings:
        supplier-out-0:
          destination: input-topic
      function:
        definition: supplier
    poller:
      fixed-delay: 1000
```

### Service 4: Stream Analytics (Kafka Streams)

**Stream Processor:**
```java
@Configuration
public class StreamAnalyticsService {
    
    @Bean
    public Function<KStream<String, Message>, KStream<String, Analytics>> process() {
        return input -> input
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
            .aggregate(
                Analytics::new,
                (key, value, aggregate) -> aggregate.update(value),
                Materialized.with(Serdes.String(), analyticsSerde)
            )
            .toStream()
            .map((windowedKey, value) -> KeyValue.pair(windowedKey.key(), value));
    }
}
```

**Configuration:**
```yaml
spring:
  cloud:
    stream:
      bindings:
        process-in-0:
          destination: input-topic
        process-out-0:
          destination: analytics-topic
      kafka:
        streams:
          binder:
            configuration:
              default.key.serde: org.apache.kafka.common.serialization.Serdes$StringSerde
              default.value.serde: org.apache.kafka.common.serialization.Serdes$StringSerde
```

### Service 5: Web Dashboard (Real-Time Visualization)

**WebSocket Configuration:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}
```

**Frontend (index.html with SockJS):**
```html
<!DOCTYPE html>
<html>
<head>
    <title>Real-Time Analytics Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <h1>Real-Time Data Analytics</h1>
    <canvas id="analyticsChart"></canvas>
    
    <script>
        const socket = new SockJS('/ws');
        const stompClient = Stomp.over(socket);
        
        stompClient.connect({}, function(frame) {
            stompClient.subscribe('/topic/analytics', function(message) {
                updateChart(JSON.parse(message.body));
            });
        });
    </script>
</body>
</html>
```

## 🎬 Getting Started

### Clone the Repository

```bash
git clone https://github.com/ZakariaElguazzar/Kafka-spring-cloud.git
cd Kafka-spring-cloud
```

### Start Kafka with Docker

```bash
docker-compose up -d
```

### Build and Run Services

```bash
# Build all services
mvn clean install

# Run Producer Service
cd producer-service
mvn spring-boot:run

# Run Consumer Service (in new terminal)
cd consumer-service
mvn spring-boot:run

# Run Supplier Service (in new terminal)
cd supplier-service
mvn spring-boot:run

# Run Analytics Service (in new terminal)
cd analytics-service
mvn spring-boot:run

# Run Dashboard Service (in new terminal)
cd dashboard-service
mvn spring-boot:run
```

### Access the Dashboard

Open your browser and navigate to: `http://localhost:8085`

## 🧪 Testing

### Test Producer API

```bash
curl -X POST http://localhost:8081/api/messages/publish \
  -H "Content-Type: application/json" \
  -d '{"id":"1","data":"Test message","timestamp":1234567890}'
```

### View Kafka Topics

```bash
docker exec -it broker kafka-topics --list --bootstrap-server localhost:9092
```

### Monitor Consumer Groups

```bash
docker exec -it broker kafka-consumer-groups --bootstrap-server localhost:9092 --list
docker exec -it broker kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group consumer-group
```

## 📹 Video Tutorial

For a detailed walkthrough of this project, watch the tutorial video: [YouTube Link](https://www.youtube.com/watch?v=8uY7JE_X_Fw)

## 📚 Additional Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Cloud Stream Documentation](https://spring.io/projects/spring-cloud-stream)
- [Confluent Kafka Docker Quickstart](https://developer.confluent.io/quickstart/kafka-docker/)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👤 Author

**Zakaria Elguazzar**
- GitHub: [@ZakariaElguazzar](https://github.com/ZakariaElguazzar)

---

⭐ If you find this project helpful, please give it a star!
