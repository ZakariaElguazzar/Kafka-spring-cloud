# Kafka Spring Cloud Streams - Real-Time Analytics

[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?style=flat&logo=apache-kafka&logoColor=white)](https://kafka.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)

A complete implementation of Apache Kafka with Spring Cloud Streams for real-time data processing and analytics. This project demonstrates event-driven architecture with page visit events, stream processing, and real-time visualization.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [Real-Time Dashboard](#real-time-dashboard)
- [Video Tutorial](#video-tutorial)

## 🎯 Overview

This application processes page visit events in real-time using Kafka Streams. It includes:

- **Producer**: REST endpoint to publish page events
- **Consumer**: Consumes and logs page events
- **Supplier**: Automatically generates random page events
- **Stream Processor**: Filters, aggregates, and counts events in time windows
- **Analytics Dashboard**: Real-time visualization using SmoothieCharts

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Kafka Broker                          │
│  ┌─────────┐      ┌─────────┐      ┌─────────┐             │
│  │ Topic T1│      │ Topic T2│      │ Topic T3│             │
│  └────┬────┘      └────┬────┘      └────┬────┘             │
└───────┼────────────────┼────────────────┼──────────────────┘
        │                │                │
        ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Consumer   │  │    Stream    │  │   Analytics  │
│              │  │  Processor   │  │   Endpoint   │
└──────────────┘  └──────────────┘  └──────────────┘
                         ▲
        ┌────────────────┴────────────────┐
        │                                  │
┌──────────────┐                  ┌──────────────┐
│   Supplier   │                  │REST Producer │
│ (Auto-Gen)   │                  │   /publish   │
└──────────────┘                  └──────────────┘
```

**Data Flow:**
1. **Producer** publishes PageEvents via REST API → **Topic T1**
2. **Consumer** reads from **Topic T1** and logs events
3. **Supplier** auto-generates events → **Topic T2**
4. **Stream Processor** reads **Topic T2**, filters (duration > 100ms), aggregates in 5-second windows → **Topic T3**
5. **Analytics Endpoint** queries the windowed store and streams data to the dashboard

## ✨ Features

- **Event Production**: REST API to publish custom page events
- **Auto-Generation**: Supplier generates random events every 200ms
- **Stream Processing**: 
  - Filters events with duration > 100ms
  - Groups by page name
  - Counts events in 5-second tumbling windows
  - Stores results in queryable state store
- **Real-Time Analytics**: 
  - SSE (Server-Sent Events) endpoint
  - Updates every second
  - Queries last 5 seconds of data
- **Live Dashboard**: Interactive charts using SmoothieCharts

## 📦 Prerequisites

- **Java**: JDK 17 or higher
- **Maven**: 3.6+
- **Docker & Docker Compose**: Latest version
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code (optional)

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/ZakariaElguazzar/Kafka-spring-cloud.git
cd Kafka-spring-cloud
```

### 2. Start Kafka with Docker

```bash
docker-compose up -d
```

This will start:
- **Zookeeper** on port `2181`
- **Kafka Broker** on port `9092`

Verify containers are running:
```bash
docker-compose ps
```

### 3. Build the Application

```bash
mvn clean install
```

## 📁 Project Structure

```
Kafka-spring-cloud/
├── src/
│   └── main/
│       ├── java/
│       │   └── org/example/kafkaspringcloud/
│       │       ├── controllers/
│       │       │   └── PageEventController.java    # REST endpoints
│       │       ├── events/
│       │       │   └── PageEvent.java              # Event record
│       │       └── handlers/
│       │           └── PageEventHandler.java       # Stream functions
│       └── resources/
│           ├── static/
│           │   └── index.html                      # Dashboard
│           └── application.properties              # Configuration
├── docker-compose.yml                              # Docker setup
├── commands.txt                                    # Useful commands
└── pom.xml                                        # Maven dependencies
```

## ⚙️ Configuration

### application.properties

```properties
spring.application.name=Kafka-spring-cloud
server.port=8080

# Consumer binding - reads from Topic T1
spring.cloud.stream.bindings.pageEventConsumer-in-0.destination=T1

# Supplier binding - writes to Topic T2
spring.cloud.stream.bindings.pageEventSupplier-out-0.destination=T2
spring.cloud.stream.bindings.pageEventSupplier-out-0.producer.poller.fixed-delay=200

# Stream Processor bindings - reads from T2, writes to T3
spring.cloud.stream.bindings.kStreamFunction-in-0.destination=T2
spring.cloud.stream.bindings.kStreamFunction-out-0.destination=T3

# Kafka Streams configuration
spring.cloud.stream.kafka.streams.binder.configuration.commit.interval.ms=1000

# Function definitions
spring.cloud.function.definition=pageEventConsumer;pageEventSupplier;kStreamFunction
```

### Key Components

#### PageEvent (Record)
```java
public record PageEvent(String name, String user, Date date, long duration) {}
```

#### PageEventHandler (Stream Functions)

**1. Consumer** - Logs events from Topic T1:
```java
@Bean
public Consumer<PageEvent> pageEventConsumer() {
    return (input) -> System.out.println(input.toString());
}
```

**2. Supplier** - Generates random events to Topic T2:
```java
@Bean
public Supplier<PageEvent> pageEventSupplier() {
    return () -> new PageEvent(
        Math.random() > 0.5 ? "P1" : "P2",
        Math.random() > 0.5 ? "U1" : "U2",
        new Date(),
        10 + new Random().nextInt(10000)
    );
}
```

**3. Stream Processor** - Processes T2 → T3:
```java
@Bean
public Function<KStream<String, PageEvent>, KStream<String, Long>> kStreamFunction() {
    return (input) -> input
        .filter((k, v) -> v.duration() > 100)
        .map((k, v) -> new KeyValue<>(v.name(), v.duration()))
        .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
        .windowedBy(TimeWindows.of(Duration.ofSeconds(5)))
        .count(Materialized.as("count-store"))
        .toStream()
        .map((k, v) -> new KeyValue<>(k.key(), v));
}
```

#### PageEventController

**Publish Endpoint**:
```java
@GetMapping("/publish")
public PageEvent publish(String name, String topic) {
    PageEvent event = new PageEvent(
        name,
        Math.random() > 0.5 ? "U1" : "U2",
        new Date(),
        10 + new Random().nextInt(10000)
    );
    streamBridge.send(topic, event);
    return event;
}
```

**Analytics SSE Endpoint**:
```java
@GetMapping(path = "/analytics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<Map<String, Long>> analytics() {
    return Flux.interval(Duration.ofSeconds(1))
        .map(sequence -> {
            Map<String, Long> stringLongMap = new HashMap<>();
            ReadOnlyWindowStore<String, Long> windowStore = 
                interactiveQueryService.getQueryableStore("count-store", 
                    QueryableStoreTypes.windowStore());
            
            Instant now = Instant.now();
            Instant from = now.minusMillis(5000);
            KeyValueIterator<Windowed<String>, Long> fetchAll = 
                windowStore.fetchAll(from, now);
            
            while (fetchAll.hasNext()) {
                KeyValue<Windowed<String>, Long> next = fetchAll.next();
                stringLongMap.put(next.key.key(), next.value);
            }
            return stringLongMap;
        });
}
```

## 🎮 Running the Application

### Start the Spring Boot Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### What Happens Automatically

1. **Supplier** starts generating random events every 200ms → Topic T2
2. **Consumer** logs all events from Topic T1
3. **Stream Processor** processes T2 events and stores aggregated counts
4. **Analytics endpoint** becomes available at `/analytics`

## 🧪 Testing

### 1. List Kafka Topics

```bash
docker exec --interactive --tty broker kafka-topics --bootstrap-server broker:9092 --list
```

You should see: `T1`, `T2`, `T3`

### 2. Publish Events via REST API

```bash
# Publish to T1
curl "http://localhost:8080/publish?name=P1&topic=T1"
curl "http://localhost:8080/publish?name=P2&topic=T1"

# Publish to T2
curl "http://localhost:8080/publish?name=P1&topic=T2"
```

### 3. Monitor Topics with Console Consumer

**Watch Topic T1:**
```bash
docker exec --interactive --tty broker \
  kafka-console-consumer --bootstrap-server broker:9092 --topic T1
```

**Watch Topic T2:**
```bash
docker exec --interactive --tty broker \
  kafka-console-consumer --bootstrap-server broker:9092 --topic T2
```

**Watch Topic T3 (with keys and values):**
```bash
docker exec --interactive --tty broker \
  kafka-console-consumer --bootstrap-server broker:9092 --topic T3 \
  --property print.key=true \
  --property print.value=true \
  --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
  --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

### 4. Produce Events Manually

```bash
docker exec --interactive --tty broker \
  kafka-console-producer --bootstrap-server broker:9092 --topic T1
```

Type your messages and press Enter.

### 5. Test Analytics Endpoint

```bash
curl http://localhost:8080/analytics
```

You'll receive a stream of JSON data:
```json
{"P1": 45, "P2": 38}
{"P1": 47, "P2": 40}
...
```

## 📊 Real-Time Dashboard

### Access the Dashboard

Open your browser and navigate to:
```
http://localhost:8080/index.html
```

### Dashboard Features

- **Real-Time Charts**: Visualizes event counts for P1 and P2 pages
- **SmoothieCharts**: Smooth, animated line charts
- **Auto-Update**: Refreshes every second via Server-Sent Events
- **5-Second Window**: Shows aggregated counts from the last 5 seconds

### How It Works

1. The dashboard connects to `/analytics` endpoint via `EventSource`
2. Receives SSE updates every second
3. Parses JSON data: `{"P1": count, "P2": count}`
4. Updates two time-series charts in real-time

### Dashboard Code (index.html)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Analytics</title>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/smoothie/1.34.0/smoothie.min.js"></script>
</head>
<body>
    <canvas id="chart2" width="600" height="400"></canvas>
    <script>
        var pages = ["P1", "P2"];
        var colors = [
            { sroke: 'rgba(0, 255, 0, 1)', fill: 'rgba(0, 255, 0, 0.2)' },
            { sroke: 'rgba(255, 0, 0, 1)', fill: 'rgba(255, 0, 0, 0.2)'}
        ];
        
        var courbe = [];
        var smoothieChart = new SmoothieChart({tooltip: true});
        smoothieChart.streamTo(document.getElementById("chart2"), 500);
        
        pages.forEach(function(v, index) {
            courbe[v] = new TimeSeries();
            smoothieChart.addTimeSeries(courbe[v], {
                strokeStyle: colors[index].sroke,
                fillStyle: colors[index].fill,
                lineWidth: 2
            });
        });
        
        var stockEventSource = new EventSource("/analytics");
        stockEventSource.addEventListener("message", function(event) {
            pages.forEach(function(v) {
                val = JSON.parse(event.data)[v];
                courbe[v].append(new Date().getTime(), val);
            });
        });
    </script>
</body>
</html>
```

## 🐳 Docker Commands Reference

### Start/Stop Services

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# Restart services
docker-compose restart
```

### Kafka Commands

**List topics:**
```bash
docker exec --interactive --tty broker \
  kafka-topics --bootstrap-server broker:9092 --list
```

**Create topic:**
```bash
docker exec --interactive --tty broker \
  kafka-topics --bootstrap-server broker:9092 --create \
  --topic test-topic --partitions 3 --replication-factor 1
```

**Describe topic:**
```bash
docker exec --interactive --tty broker \
  kafka-topics --bootstrap-server broker:9092 --describe --topic T2
```

**Delete topic:**
```bash
docker exec --interactive --tty broker \
  kafka-topics --bootstrap-server broker:9092 --delete --topic test-topic
```

## 🔍 Troubleshooting

### Issue: Application won't start

**Solution:**
1. Ensure Docker containers are running: `docker-compose ps`
2. Check Kafka broker is accessible: `telnet localhost 9092`
3. Verify port 8080 is not in use: `lsof -i :8080`

### Issue: No data in dashboard

**Solution:**
1. Check if supplier is generating events (view T2 topic)
2. Verify stream processor is running (view T3 topic)
3. Check browser console for errors
4. Test `/analytics` endpoint directly

### Issue: Consumer not receiving messages

**Solution:**
1. Verify topic exists: `kafka-topics --list`
2. Check consumer group: `kafka-consumer-groups --list`
3. Review application logs for errors

## 📹 Video Tutorial

For a detailed walkthrough of this project, watch the tutorial video:
[https://www.youtube.com/watch?v=8uY7JE_X_Fw](https://www.youtube.com/watch?v=8uY7JE_X_Fw)

## 📚 Additional Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Cloud Stream Documentation](https://spring.io/projects/spring-cloud-stream)
- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [Confluent Docker Quickstart](https://developer.confluent.io/quickstart/kafka-docker/)
- [SmoothieCharts Documentation](http://smoothiecharts.org/)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

## 👤 Author

**Zakaria Elguazzar**
- GitHub: [@ZakariaElguazzar](https://github.com/ZakariaElguazzar)

---

⭐ **If you find this project helpful, please give it a star!**

## 📞 Support

If you have any questions or need help, feel free to:
- Open an issue on GitHub
- Watch the video tutorial
- Check the documentation links above

Happy streaming! 🚀
