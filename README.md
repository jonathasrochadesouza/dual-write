# Dual-Write Lab

A hands-on laboratory to observe, in real time, the **Dual-Write Problem** in distributed systems and how the **Transactional Outbox Pattern** eliminates it.

Based on the article series *"Padrões de Sistemas Distribuídos"*.

```
                    Kafka ✓              Kafka ✘
  DB ✓     C1 (by luck)          C3 (phantom order)
  DB ✘     C4 (phantom event)    C2 (lost operation)
           ↑ dual-write reaches all 4 quadrants ↑
           outbox collapses the state space to the diagonal (C5 all / C6 nothing)
```

## Stack

| Layer | Technology | Version |
|---|---|---|
| Backend | Spring Boot + Java 21 LTS | 3.4.5 / 21 |
| Database | PostgreSQL | 17.4 |
| Messaging | Apache Kafka (KRaft, no ZooKeeper) | 3.8.1 |
| Frontend | Angular (standalone, signals) | 19.x |
| Observability | Prometheus + Grafana + Micrometer | latest stable |

## Quick Start

```bash
cd main
docker compose up -d --build
```

First build takes a few minutes (Maven dependencies + npm install). All services start with healthchecks and wait for dependencies.

## Access

| Service | URL | Credentials |
|---|---|---|
| Frontend | http://localhost:4200 | — |
| Backend API | http://localhost:8080/api/experiments | — |
| Actuator | http://localhost:8080/actuator/health | — |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |

## API

### Run an experiment

```bash
curl -X POST http://localhost:8080/api/experiments/run \
  -H 'Content-Type: application/json' \
  -d '{"scenario":"DUAL_WRITE_BOTH_OK"}'
```

**Supported scenarios:**

| Scenario | Description | Verdict |
|---|---|---|
| `DUAL_WRITE_BOTH_OK` | DB and Kafka both succeed (no atomicity) | ATOMICO |
| `DUAL_WRITE_NONE` | Failure before commit → rollback everywhere | OPERACAO_PERDIDA |
| `DUAL_WRITE_DB_ONLY` | DB commits, Kafka publish fails (phantom order) | INCONSISTENTE |
| `DUAL_WRITE_KAFKA_ONLY` | Kafka publishes, DB write fails (phantom event) | INCONSISTENTE |
| `OUTBOX_COMMIT` | order + outbox_event committed atomically | ATOMICO |
| `OUTBOX_ROLLBACK` | Atomic rollback of both tables | ATOMICO |

### Get experiment report

```bash
curl http://localhost:8080/api/experiments/{id}/report
```

### Experiment history

```bash
curl http://localhost:8080/api/experiments
```

### Invalid scenario

```bash
curl -X POST http://localhost:8080/api/experiments/run \
  -H 'Content-Type: application/json' \
  -d '{"scenario":"INVALID"}'
# → 400 { "error": "VALIDATION_ERROR", "message": "Invalid or unsupported scenario" }
```

## Architecture

```
main/
├── docker-compose.yml
├── infra/
│   ├── postgres/init.sql          # orders, outbox_events, experiments
│   ├── kafka/create-topic.sh      # idempotent topic creation
│   ├── prometheus/prometheus.yml  # scrape backend actuator
│   └── grafana/provisioning/      # datasource + dashboard as code
└── src/
    ├── back/                      # Spring Boot 3.4.5 / Java 21
    │   └── java/com/dualwrite/lab/
    │       ├── api/               # REST controller (single endpoint)
    │       ├── scenario/          # 6 strategies (ScenarioPort)
    │       ├── order/             # Order domain + JDBC adapter
    │       ├── outbox/            # OutboxEvent domain + JDBC adapter
    │       ├── kafka/             # KafkaEventPublisher + KafkaMessageInspector
    │       ├── fault/             # FaultInjector (3 named fault points)
    │       ├── report/            # VerdictDeriver + ExpectedOutcome
    │       ├── metrics/           # Micrometer counters
    │       └── experiment/        # ExperimentRunner + persistence
    └── front/                     # Angular 19 (standalone + signals)
        └── app/
            ├── models.ts          # ScenarioId, Verdict, SCENARIOS
            ├── experiment.service.ts
            └── app.component.*    # UI: scenario cards, result panel, history
```

### Design Decisions

- **Strategy pattern** — each scenario is an explicit class implementing `ScenarioPort` (pedagogically clear, deliberately not abstracted away).
- **Fault injection** — `FaultInjector` throws `SimulatedOutageException` at named points; failure always means *absence* (never corrupted data).
- **Experiments table** — written outside the scenario transaction via `ExperimentPersistence` (REQUIRES_NEW), so it survives rollbacks.
- **Kafka inspection** — `KafkaMessageInspector` reads from the beginning with a short timeout, filtering by `experimentId` in the payload.
- **Verdict derivation** — `ExpectedOutcome` declares what each scenario predicts; `VerdictDeriver` compares predicted vs observed.

## Observability

### Prometheus Metrics

Exposed at `/actuator/prometheus`:

| Metric | Tags | Description |
|---|---|---|
| `dualwrite_orders_total` | scenario | Orders saved to DB |
| `dualwrite_events_published_total` | scenario | Events published to Kafka |
| `dualwrite_outbox_rows_total` | scenario | PENDING rows in outbox_events |
| `dualwrite_failures_db_total` | scenario | DB rollbacks (simulated outages) |
| `dualwrite_failures_kafka_total` | scenario | Failed Kafka publishes (simulated outages) |
| `dualwrite_experiments_total` | scenario, verdict | Experiments by verdict |
| `dualwrite_db_write_latency` | scenario | DB write latency |
| `dualwrite_kafka_publish_latency` | scenario | Kafka publish latency |

### Grafana Dashboard

Auto-provisioned at `http://localhost:3000` (uid: `dual-write-lab`):

**Top row — key counters:**

| Panel | What it shows |
|---|---|
| **Pedidos no banco** | Total orders persisted in the DB |
| **Eventos no Kafka** | Total events published to the topic |
| **Linhas no Outbox** | Total PENDING rows in outbox_events |
| **Falhas de publish no Kafka** | Failed Kafka publishes (red if > 0) |
| **Rollbacks no banco** | DB rollbacks (red if > 0) |
| **Experimentos executados** | Total experiments run |

**Detail panels:**

| Panel | What it shows |
|---|---|
| **Lacuna de consistencia (banco vs Kafka)** | DB writes vs Kafka publishes per scenario — the dual-write gap |
| **Latencia: gravacao no banco** | Average DB write time per scenario |
| **Latencia: publicacao no Kafka** | Average Kafka publish time per scenario |
| **Vereditos dos experimentos** | Bar chart: ATOMICO (green), INCONSISTENTE (red), OPERACAO_PERDIDA (orange) |
| **Experimentos por cenario** | Experiments per scenario + verdict over time |

## Useful Commands

```bash
# Start everything
cd main && docker compose up -d --build

# Stop and clean volumes
docker compose down -v

# Check service health
docker compose ps

# Backend logs
docker compose logs -f backend

# Run all 6 scenarios
for s in DUAL_WRITE_BOTH_OK DUAL_WRITE_NONE DUAL_WRITE_DB_ONLY DUAL_WRITE_KAFKA_ONLY OUTBOX_COMMIT OUTBOX_ROLLBACK; do
  curl -s -X POST http://localhost:8080/api/experiments/run \
    -H 'Content-Type: application/json' \
    -d "{\"scenario\":\"$s\"}" | python3 -m json.tool
  echo "---"
done

# Query Prometheus
curl -s 'http://localhost:9090/api/v1/query?query=dualwrite_experiments_total'

# Check DB experiments
docker exec dualwrite-postgres psql -U dualwrite -d dualwrite \
  -c "SELECT scenario, verdict, count(*) FROM experiments GROUP BY scenario, verdict ORDER BY 1,2;"
```

## Running Tests

```bash
# Backend unit tests
cd main/src/back && mvn test

# Full build
mvn clean package
```

## Troubleshooting

| Problem | Solution |
|---|---|
| Port 8080 in use | Stop the conflicting process, or the `colima` port forward may differ from Docker Desktop |
| Backend unhealthy | Check logs: `docker compose logs backend` |
| Kafka topic missing | Init runs automatically on first boot; for clean restart: `docker compose down -v && docker compose up -d` |
| Grafana dashboard empty | Metrics appear after running at least one experiment |
