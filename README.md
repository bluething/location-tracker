### Introduction
This repo is a solution for [Location Tracking System](https://github.com/bitwyre/backend-interview). Just for my exercise.

### High Level Architecture
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

### Learning Notes
#### Embeded Kafka vs Testcontainer

| Aspect                  | Embedded Kafka (`spring-kafka-test`)                                | Testcontainers (`org.testcontainers:kafka`)                        |
| ----------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------------ |
| **Broker type**         | In-JVM, light mock broker                                           | Full Kafka broker in a Docker container                            |
| **Startup time**        | Very fast (≈100–300 ms)                                             | Slower (≈5–15 s, depending on image pull)                          |
| **Dependencies**        | Only JARs; no external runtime                                      | Requires Docker daemon (and enough disk/CPU to run it)             |
| **Resource usage**      | Minimal (heap only)                                                 | Higher (container CPU / memory overhead)                           |
| **API & networking**    | Direct in-memory client→broker, no real networking                  | Real TCP networking on ephemeral ports                             |
| **Production fidelity** | Lower—doesn’t emulate all broker behaviors (e.g. replica juggling)  | High—runs the same Kafka version you’ll use in prod                |
| **Isolation**           | Shares JVM; one broker instance per test class                      | Fully isolated per test/container, easy parallel runs              |
| **Configuration**       | Annotate with `@EmbeddedKafka`; auto-wires into Spring’s properties | Programmatically start `KafkaContainer`, inject bootstrap URL      |
| **CI friendliness**     | Excellent in pure-JVM CI (no Docker)                                | CI must support Docker (can be disabled with profiles)             |
| **Debuggability**       | Limited introspection (logs in JVM)                                 | Can `docker logs` or `exec` into the container                     |
| **Ideal use case**      | Fast unit-style or lightweight integration tests                    | Full end-to-end integration tests—or anywhere you want prod-parity |


##### When to pick which

Embedded Kafka 
* Ultra-fast startup for CI → runs in milliseconds 
* Zero external dependencies (great for locked-down build servers)
* ⚠️ Doesn’t catch every broker-specific edge case 

Testcontainers
* Runs “real” Kafka—same version, same networking, same configs
* Better for smoke tests or complex flows (topics, ACLs, replication)
* ⚠️ Slower startup, needs Docker available locally/CI

#### About end-to-end (E2E) tests
Embedded Kafka is fantastic for fast, in-JVM integration checks of producer→consumer logic. For anything involving real WebSocket endpoints or multi-process wiring, you’ll need a real Kafka broker—via Docker Compose or Testcontainers.

##### Docker Compose

* Spin up Zookeeper + Kafka via docker-compose.yml.
* Point both services at localhost:9092.
* Run your ingestion service locally (or in a container) and your persistence service locally (or in a container).
* Drive it end-to-end from your test client → ingestion → Kafka → persistence → DB.

If you just need a quick local sandbox, Docker Compose is the simplest:
* docker-compose up -d zookeeper kafka
* Configure your apps to hit localhost:9092
* Run your client tests or manual sanity checks

##### Testcontainers for Everything

* Use Testcontainers’ KafkaContainer to launch Kafka in Docker.
* Optionally use Testcontainers’ GenericContainer or a DockerComposeContainer to bring up your ingestion and persistence services as well.
* This still requires Docker, but you gain programmatic control of the entire stack from within your tests.

#### Kafka serialization
| Feature                    | JSON                                        | Avro                                                  | Protobuf                                              |
| -------------------------- | ------------------------------------------- | ----------------------------------------------------- | ----------------------------------------------------- |
| **Format**                 | Text (UTF-8)                                | Compact binary                                        | Compact binary                                        |
| **Schema**                 | Implicit—your code must agree               | Explicit `.avsc` or Java-generated                    | Explicit `.proto` files                               |
| **Payload size**           | Largest (human-readable)                    | Smaller (no field names)                              | Smallest (varint encoding)                            |
| **Serialization speed**    | Slower                                      | Fast                                                  | Very fast                                             |
| **Deserialization safety** | Can fail at runtime (missing fields, typos) | Strongly enforced via Registry                        | Strongly enforced via Registry                        |
| **Schema evolution**       | Manual compatibility checks                 | Built-in compatibility rules (back/forward)           | Good, but you must manage defaults explicitly         |
| **Tooling & ecosystem**    | Built into Jackson, no extra infra          | Great Java support + Confluent Schema Registry        | Excellent Java + gRPC support; Registry plugins exist |
| **Use-case sweet spot**    | Low-volume, human-debuggable APIs           | High-throughput Kafka pipelines with evolving schemas | Microservices + gRPC + high-perf messaging            |

JSON 

Pros:
* No extra tooling or registry needed.
* Easy to inspect, debug and log.

Cons:
* Bulky payloads blow up your network and Kafka bytes.
* No built-in schema enforcement—mismatches slip through.
* When to pick: prototyping, very low volume, or when you really need human-readable messages.

Avro
Pros:
* Compact binary encoding without field names.
* Schema lived in a central Registry → strong contract, automatic compatibility checks.
* Excellent support in the Kafka ecosystem (Serdes, Confluent tools).

Cons:
* Requires you to maintain Avro schemas and run a Schema Registry.
* You need a build step to generate Java classes (or runtime reflection).
* When to pick: production Kafka pipelines where you expect your event model to grow/evolve and you care about throughput and space.

Protobuf
Pros:
* Even more compact than Avro with varint encoding.
* First-class in gRPC → if you plan RPC + messaging, one IDL for both.
* Strongly typed, easy Java code gen.

Cons:
* Schema evolution rules are a bit trickier (you must handle defaults carefully).
* Less “batteries-included” for Schema Registry, though Confluent and others have plugins.
* When to pick: you’re already using gRPC/Protobuf elsewhere or you need extreme performance on very tight budgets.

#### About multi-module
##### Pros of a Multi-Module vs Mono-repo
* Shared code  
  You can factor out common stuff (your LocationEvent DTO, Kafka serializers/deserializers, shared constants) into a common module and avoid version‐drifting between services.
* One build, one version
  A single parent POM means you manage all your Spring Boot and plugin versions in one place (via <dependencyManagement>).
* Simpler local dev  
  Checkout one repo, run mvn clean install && mvn spring-boot:run -pl ingestion-service (or -pl persistence-service), and both modules pick up the same settings.
* Atomic refactors  
  Refactoring shared code (e.g. renaming a field on your event schema) only needs one PR, one CI run, one merge.

##### Cons & Things to Watch 
* Coupling your release cycles  
  If you ever need to upgrade persistence independently of ingestion, you’ll either need to splinter off or introduce independent versioning inside your multi-module.
* Bigger builds  
  Every mvn install will build both modules (though you can skip modules with -pl/-am).
* Microservice boundaries  
  Ones to watch: if you ever want to run them in separate teams/repos, you’ll need to chunk them apart.

##### Tips for a Smooth Multi-Module Workflow 
1. Common module is your friend  
   Put all cross-service DTOs, serializers, any shared Spring @Configuration.
2. Profiles & application.yml  
   - Keep shared defaults in common/src/main/resources/application.yml  
   - Override per-service in ingestion-service/src/main/resources/….
3. CI/CD  
   - Build with mvn verify at the root  
   - Publish both JARs (or Docker images) with the same version tag.
4. Independent run commands  
   - Use the Spring Boot Maven plugin’s -pl flag to start just one service  
   - Or configure your IDE’s run configurations to point at each module.
5. Consider a “docker-compose.dev.yml”  
   - Define both services, Kafka & DB in one compose file  
   - Developers can spin up the full stack with docker-compose -f … up.

##### When to Split into Separate Repos
* When each service has its own team or drastically different lifecycles.
* If you need to enforce strict isolation (e.g. different compliance requirements).  

But for a two-service PoC and early iterations, a single multi-module repo keeps things DRY, consistent, and easy to manage. Once you have enough confidence in your service boundaries, you can always yank a module into its own repo.

##### Why I don't use @Slf4j and Lombok?  

I left out Lombok on purpose to keep the examples fully “plain Java” and avoid introducing another build-time dependency—so every getter/setter, constructor, and logger declaration is explicit and easy to trace for anyone jumping into the code.
That said, Lombok can absolutely clean up boilerplate:
* @Slf4j replaces your private static final Logger… with a generated log field.
* @Data, @Value, @AllArgsConstructor, etc., can generate your DTO’s getters, setters, constructors, toString(), and more.

Trade-offs of Lombok  
* 📦 Less boilerplate, more concise classes.  
* ⚙️ Another compile-time tool to configure and manage.  
* 🧐 Can sometimes obscure generated code (“where did that method come from?”) if your team isn’t familiar.

##### When to choose Java record vs Lombok  

* Use record when your DTO is a simple, immutable data carrier (no setters, no inheritance). It’s built-in, concise, and communicates intent clearly.
* Stick with Lombok (e.g. @Data, @Builder) if you need mutable beans, builders, or you have complex inheritance hierarchies.

##### SQL vs NoSQL  

###### SQL (e.g. PostgreSQL, TimescaleDB, MySQL)  

Pros  
* Strong consistency & ACID—every write is transactional. 
* Rich query power—joins, window functions, geospatial (PostGIS) or time-series extensions (TimescaleDB). 
* Familiar tooling—migrations (Flyway/Liquibase), ORM support, mature monitoring.

Cons 
* Scaling writes beyond a single node can get complex (sharding). 
* Schema rigidity—adding new fields or event types requires migrations.

When to pick 
* You need to run complex queries: “give me all users who left a geo-fence,” “compute user X’s trip duration,” “aggregate by hour.” 
* Data volumes are moderate (a few million writes/sec across the cluster). 
* You want ACID guarantees for analytic correctness.

###### NoSQL (e.g. Cassandra, MongoDB, DynamoDB) 

Pros 
* Massive write scalability—linear scale-out by adding nodes. 
* Flexible schema—add new fields or event types on the fly. 
* Built-in replication & high availability across data centers.

Cons 
* Eventual consistency (in many systems)—you might read stale data unless you configure strong consistency at a performance cost. 
* Limited query model—typically key-value or wide-column; complex joins or ad-hoc aggregations are hard or expensive. 
* Operational overhead—tuning compactions, repair, partitions.

When to pick 
* You expect to burst to tens or hundreds of millions of points/sec. 
* Your query patterns are simple: “get last N points for user X,” “scan by time range per user,” no cross-user analytics. 
* You favor uptime and horizontal scale above complex analytics.

###### My choice  

For most early-stage location-tracking services, PostgreSQL (optionally with the TimescaleDB extension) hits the sweet spot: 
* You get straightforward SQL + time-series or geo-indexing. 
* A single POC cluster can easily handle 10 000 users pushing updates every few seconds. 
* When you outgrow it, you can: 
  * Shard by user-ID 
  * Introduce read-replicas for analytics 
  * Or migrate hot-path writes to a NoSQL store while keeping SQL for reporting.  

If your queries will mostly be “give me all recent points for this one user,” and you need extreme write scale today, pick Cassandra or DynamoDB. Otherwise, start with PostgreSQL—you’ll move faster, and you can always bolt on a NoSQL tier later if the load demands it.

##### TimescaleDB  
TimescaleDB is an open-source extension that turns PostgreSQL into a purpose-built time-series database. Under the hood it’s still PostgreSQL—so you get all your familiar SQL, ACID transactions, extensions like PostGIS—but with huge performance and manageability improvements for time-series workloads.

###### Why TimescaleDB for Location Tracking? 
1. High ingest rate: hundreds or thousands of points/sec get batched into the “current” chunk. 
2. Fast time-range queries: you’ll often ask “show me all points for user X between T1 and T2” or “trace the last N minutes.” Hypertable chunk pruning makes those queries sub-second, even over years of history. 
3. Built-in analytics: continuous aggregates give you ready-made summaries (e.g. per-hour heatmaps) without extra code. 
4. Data lifecycle: compression and retention policies mean you can keep raw data hot for a week, compress for long-term storage, and auto-drop it when it’s no longer needed.

###### Hypertables & Chunking
* Hypertable: your logical table (e.g. location_events) is “partitioned” automatically by time (and optionally by a second key, like user_id) into many smaller tables called chunks. 
* Chunking: when you insert a row, TimescaleDB routes it to the right chunk based on its timestamp (and user-ID if you choose multi-dimension partitioning). Queries that target a time window only hit the relevant chunks, which makes large scans lightning-fast.

###### Indexing & Performance 
* You still create indexes on hypertables (e.g. a composite index on (user_id, timestamp)). 
* Because each chunk is smaller, index lookups and VACUUM/ANALYZE operations are much cheaper. 
* Bulk inserts at high throughput are efficiently batched into the current chunk.

###### Compression 
* Native, columnar compression for historical chunks: after data “ages out” (say older than 7 days), you can enable compression on those chunks. 
* Compressed chunks use up to 10× less disk and still support fast scans for analytics. 

###### Enable Compression in TimescaleDB

Enable Compression on a Hypertable 
```sql
ALTER TABLE location_events SET (timescaledb.compress, timescaledb.compress_segmentby = 'user_id');
```
Add Compression Policy (e.g., compress data older than 7 days)
```sql
SELECT add_compression_policy('location_events', INTERVAL '7 days');
```
This tells TimescaleDB to automatically compress chunks where `max(timestamp) < now() - INTERVAL '7 days'`  

(Optional) Manually Compress a Chunk 
```sql
SELECT compress_chunk('_timescaledb_internal._hyper_1_10_chunk');
```

Verify Compression Settings  
```sql
SELECT * FROM timescaledb_information.compressed_hypertables;
```
And to list compressed chunks: 
```sql
SELECT chunk_name, is_compressed
FROM timescaledb_information.chunks
WHERE hypertable_name = 'location_events';
```

Implement in Liquibase, put this in a separate changelog with `runInTransaction: false`. 
```sql
-- Enable compression and segment by user_id
ALTER TABLE location_events SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'user_id'
);

-- Add compression policy (older than 7 days)
SELECT add_compression_policy('location_events', INTERVAL '7 days');
```

###### Scalability & Clustering 
* On a single node, you can handle millions of inserts/sec and terabytes of data. 
* A multi-node (enterprise) option lets you shard writes across servers while still querying them as one logical hypertable.