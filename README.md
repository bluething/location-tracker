### Architecture
```text
+------------+        WebSocket        +-------------------+
|   Clients  |  ─────────────────────> | Ingestion Service |
| (mobile,   |                        | (Spring Boot)     |
|  IoT, …)   |                        +-------------------+
+------------+                                   |
                                                 | Kafka Producer
                                                 ▼
                                         +-------------------+
                                         |   Kafka Cluster   |
                                         +-------------------+
                                                 |
                                  Kafka Consumer |
                                                 ▼
                                         +----------------------+              
                                         |  Persistence Service | 
                                         |  (Spring Boot)       |              
                                         |  • Validates + saves |
                                         |  • CLI (Spring Shell)|
                                         +----------------------+
                                                  |
                                                  | JDBC / driver
                                                  ▼
                                         +-------------------------------+
                                         |  PostgreSQL with TimeScaleDB  |
                                         +-------------------------------+

```