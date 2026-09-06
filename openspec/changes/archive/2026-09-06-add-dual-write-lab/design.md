# Design: add-dual-write-lab

## Context

Repositório greenfield (sem código). O laboratório demonstra o dual-write problem e o Transactional Outbox Pattern conforme o artigo "Padrões de Sistemas Distribuídos (#01)". Ver motivation em `proposal.md` e comportamento esperado nas specs `experiment-scenarios`, `experiment-report`, `observability`, `frontend` e `lab-infrastructure`. Estrutura de diretórios definida pelo usuário: `main/src/back/` e `main/src/front/`.

O insight central que orienta o design: os seis cenários são o produto cartesiano de **dois padrões** (dual-write vs outbox) por **pontos de injeção de falha** (nenhuma, antes do commit, no publish após commit, no banco após publish). A ordem das operações dentro de cada cenário *é* a semântica do cenário:

```
                    Kafka ✔              Kafka ✘
  Banco ✔   C1 (por acaso)         C3 (pedido fantasma)
  Banco ✘   C4 (evento fantasma)   C2 (operação perdida)
            ↑ dual-write alcança os 4 quadrantes ↑
            outbox colapsa o espaço para a diagonal (C5 tudo / C6 nada)
```

## Goals / Non-Goals

**Goals:**
- Ambiente 100% reproduzível com um comando (`docker compose up`), sem passos manuais.
- Código pedagógico: cada cenário legível como uma estratégia explícita, não escondido atrás de um motor de caos genérico.
- Observabilidade que *visualize o problema*: a divergência entre séries de métricas banco × Kafka por cenário.
- SOLID, DRY e YAGNI aplicados com julgamento: DRY na infraestrutura (fault injection, ports), duplicação *deliberada* mínima onde a clareza pedagógica exigir.

**Non-Goals:**
- Message Relay (consumir a outbox e publicar no Kafka) — escopo do artigo #02; cenários outbox deixam o Kafka vazio por design.
- Idempotência de consumidor, ordenação, retry com backoff, DLQ, exactly-once.
- Payloads corrompidos/eventos falsos — falha é sempre ausência por outage simulada.
- Autenticação/autorização, multi-tenancy, CI/CD de deploy.

## Decisions

### D1 — Estrutura de diretórios e local do compose
```
main/
├── docker-compose.yml          ← raiz de main/ (ponto único de subida)
├── infra/
│   ├── postgres/init.sql       ← schema no primeiro boot do volume
│   ├── kafka/                  ← setup de tópico
│   ├── prometheus/prometheus.yml
│   └── grafana/provisioning/   ← datasource + dashboard como código
└── src/
    ├── back/                   ← Spring Boot + Java 21 (Maven)
    └── front/                  ← Angular
```
Alternativa: compose na raiz do repo. Rejeitada porque o usuário definiu `main/` como home do projeto; a raiz do repo continua com docs/openspec.

### D2 — Backend: arquitetura em camadas com ports, cenários como estratégias
```
back/
└── java/.../dualwrite/
    ├── api/            OrderController (endpoint único), Advice de erros
    ├── scenario/       ScenarioId (enum), ScenarioPort, uma classe por cenário
    ├── order/          Order (domain), OrderRepository (port + adapter JDBC/JPA)
    ├── outbox/         OutboxEvent, OutboxRepository
    ├── kafka/          EventPublisher (port), KafkaEventPublisher (adapter)
    ├── fault/          FaultInjector com pontos nomeados (ex.: FAIL_PUBLISH_AFTER_COMMIT, FAIL_DB_AFTER_PUBLISH, FAIL_BEFORE_COMMIT)
    ├── report/         ExperimentReport, veredito (enum), ReportService
    └── metrics/        Instrumentação Micrometer
```
Cada cenário implementa `ScenarioPort` (padrão Strategy, DIP): o controller recebe o `scenario` e despacha. O `FaultInjector` é uma abstração mínima de injeção por outage (lança exceção que simula queda) — DRY nos mecanismos de falha, mas o *fluxo* de cada cenário fica explícito na própria classe.
Alternativa: um motor genérico de caos configurável (padrão × falha como dados). Rejeitada: obscurece exatamente o código que o laboratório existe para mostrar; YAGNI.

### D3 — Como cada cenário mapeia para transação e falha
| Cenário | Fluxo | Garantia observável |
|---|---|---|
| C1 `DUAL_WRITE_BOTH_OK` | `save` → `send` (sem `@Transactional` entre ambos; cada recurso independente) | ambos gravam, sem atomicidade |
| C2 `DUAL_WRITE_NONE` | `@Transactional`: `save` → fault antes do commit → rollback | nada em nenhum lugar |
| C3 `DUAL_WRITE_DB_ONLY` | `save` (commit) → fault no publish (outage simulada) | pedido no banco, Kafka vazio |
| C4 `DUAL_WRITE_KAFKA_ONLY` | `send` → fault na escrita do banco | Kafka com evento, banco vazio |
| C5 `OUTBOX_COMMIT` | `@Transactional`: `INSERT order` + `INSERT outbox_event` → commit | duas tabelas atômicas; Kafka vazio (sem relay) |
| C6 `OUTBOX_ROLLBACK` | `@Transactional`: ambos os INSERTs → fault antes do commit → rollback | zero linhas nas duas tabelas |
O ponto crítico do C3: o fault no publish precisa acontecer *após* o commit do banco — o `send` fica fora da transação (ou usa `TransactionSynchronization` afterCommit). Detalhe de implementação a resolver na fase de código, com teste validando o desfecho.

### D4 — Persistência: JDBC/JPA e schema SQL versionado
Tabelas: `orders`, `outbox_events` (id, aggregate_id, event_type, payload JSONB, status, created_at) e `experiments` (id, scenario, executed_at, verdict). `experiments` é gravada *fora* da transação do cenário, após o veredito — o registro do experimento nunca sofre rollback do cenário que observa. Payload do outbox em JSONB.
Alternativa: Flyway. Aceitável; decidir na implementação — para o lab, `init.sql` no volume do Postgres basta (YAGNI), com migração para Flyway se evoluir.

### D5 — Inspeção do Kafka para o relatório
Consumer dedicado (ou `AdminClient` + consumer de leitura) que lê o tópico desde o início com `auto.offset.reset=earliest` filtrando por `experimentId`/aggregate do experimento. Cada evento publicado carrega o `experimentId` no header/payload para rastreio — sem isso o veredito não consegue atribuir mensagens ao experimento.
Alternativa: armazenar offset na publicação. Rejeitada por complexidade; leitura por filtro basta para volume de laboratório.

### D6 — Métricas: taxonomia Micrometer
```
dualwrite.db.writes.total{scenario}
dualwrite.db.rollbacks.total{scenario}
dualwrite.kafka.publishes.total{scenario}
dualwrite.kafka.publish.failures.total{scenario}
dualwrite.outbox.rows.committed.total{scenario}
dualwrite.experiments.total{scenario,verdict}
```
Painel central do Grafana: séries `db.writes` vs `kafka.publishes` por cenário — a divergência visual é o próprio problema. Actuator + `micrometer-registry-prometheus` no `/actuator/prometheus`.

### D7 — Frontend: Angular standalone + nginx
Angular (standalone components, signals, `HttpClient`), build multi-stage (node → nginx). Nginx faz `proxy_pass /api → backend:8080` — zero CORS. Tela única: grid de 6 cards agrupados por padrão (4 dual-write vs 2 outbox), painel de execução com previsto vs observado e veredito, histórico, links para Grafana/Prometheus.
Alternativa: dev-server com `ng serve` no container. Mantida apenas como perfil opcional de dev, não como fluxo principal.

### D8 — Imagens e versões (compatibilidade Java 21 LTS)
- Backend: `maven:3.9-eclipse-temurin-21` (build) → `eclipse-temurin:21-jre` (runtime); Spring Boot 3.x mais recente com suporte a Java 21.
- Kafka: imagem estável `apache/kafka` em modo KRaft (single node) — sem ZooKeeper.
- Postgres: `postgres:17` (ou 18, a mais estável no momento da implementação; Postgres não tem LTS formal).
- Prometheus/Grafana: latest estável. Versões pinadas por tag no compose (nunca `latest` flutuante).

### D9 — Veredito: derivação declarativa
Cada cenário declara seu `ExpectedOutcome` (presença/ausência esperada em orders, outbox, Kafka). O `ReportService` compara observado vs previsto e deriva o veredito: match na diagonal "tudo" → `ATOMICO`; match "nada" → `OPERACAO_PERDIDA`; divergência banco/Kafka → `INCONSISTENTE`. Isso mantém o relatório DRY e extensível a novos cenários.

## Risks / Trade-offs

- [C3 exige falha após o commit] → teste de integração dedicado validando que o pedido sobrevive e o Kafka fica vazio; implementar com afterCommit callback ou send fora da transação.
- [Kafka em KRaft single-node é frágil para restart de volume] → volume nomeado + healthcheck com `kafka-topics --list`; init de tópico idempotente.
- [Leitura do tópico "do início" escala mal] → aceitável para volume de laboratório; documentar limite.
- [6 cenários duplicam parte do fluxo] → duplicação deliberada e pequena (clareza pedagógica); `FaultInjector` e repositories compartilhados seguram o DRY onde importa.
- [Versões "latest estável" podem mudar entre máquinas] → pinar tags exatas no compose quando da implementação.
- [Grafana provisioning pode divergir do dashboard manual] → dashboard em JSON versionado no repo; nenhuma edição manual.

## Migration Plan

Greenfield: sem migração. Rollback = `docker compose down -v` (remove volumes e o lab inteiro). Nenhum dado de produção envolvido.

## Open Questions

- Nome do tópico Kafka (sugestão: `orders.order-created`) — detalhe de implementação, sem impacto de escopo.
- JPA vs JDBC puro para `orders`/`outbox_events` — decidir na implementação; o requisito é que C5/C6 compartilhem a mesma transação.
- Se o histórico de experimentos paginar — YAGNI agora; lista simples.
