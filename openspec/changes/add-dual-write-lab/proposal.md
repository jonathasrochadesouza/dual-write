# Proposal: add-dual-write-lab

## Why

O dual-write problem (gravar no banco e publicar no Kafka como duas operações independentes) não pode ser demonstrado apenas em teoria: é preciso observar, em tempo real, os estados inconsistentes que ele produz e como o Transactional Outbox Pattern os elimina. Este change cria um laboratório executável — baseado no artigo "Padrões de Sistemas Distribuídos (#01): Dual-Write Problem" — que permite rodar seis cenários experimentais, comparar o resultado observado com o resultado previsto e visualizar a inconsistência em métricas (Grafana/Prometheus) e em uma interface Angular.

## What Changes

- **Novo backend** Spring Boot (Java 21 LTS) em `main/src/back/`, com Maven, arquitetura em camadas com ports/adapters, seguindo SOLID, DRY e YAGNI.
- **Seis cenários experimentais** executáveis por um único endpoint parametrizado:
  - C1 `DUAL_WRITE_BOTH_OK` — dual-write sem falha: banco e Kafka gravam (correto apenas por acaso, sem atomicidade).
  - C2 `DUAL_WRITE_NONE` — falha antes do commit: rollback total, nada gravado em lugar nenhum (operação perdida).
  - C3 `DUAL_WRITE_DB_ONLY` — falha no publish após o commit do banco: pedido no banco, evento ausente no Kafka ("pedido fantasma", outage simulada).
  - C4 `DUAL_WRITE_KAFKA_ONLY` — publish antes da gravação e falha no banco: evento no Kafka, pedido ausente no banco ("evento fantasma").
  - C5 `OUTBOX_COMMIT` — outbox: `orders` + `outbox_events` commitadas atomicamente na mesma transação.
  - C6 `OUTBOX_ROLLBACK` — outbox: falha após ambos os INSERTs, dentro da transação → rollback atômico das duas tabelas.
- **Fault injection** mínima e nomeada (falha = ausência por queda de serviço simulada; sem payloads corrompidos).
- **Relatório de experimento** com veredito: compara estado previsto vs observado (tabela `orders`, tabela `outbox_events`, mensagens do tópico Kafka) e classifica o resultado (atômico / inconsistente / operação perdida).
- **Message Relay NÃO incluído** (escopo do artigo #02): nos cenários outbox, o Kafka permanece propositalmente vazio e o relatório trata isso como resultado esperado.
- **Observabilidade**: Micrometer + Spring Actuator, Prometheus scrape e Grafana provisionado como código com dashboard "Dual-Write Lab" (incluindo painel de lacuna de consistência banco × Kafka por cenário).
- **Frontend** Angular em `main/src/front/`, servido por nginx no Docker Compose (proxy para o backend, sem CORS), com tela de hipótese vs observação (cards dos 6 cenários, execução, veredito e histórico).
- **Infraestrutura Docker Compose**: PostgreSQL (latest stable, 17/18), Apache Kafka modo KRaft (sem ZooKeeper, versão LTS/estável compatível), backend, frontend, Prometheus e Grafana.

## Capabilities

### New Capabilities
- `experiment-scenarios`: Execução dos seis cenários experimentais (dual-write vs outbox) via endpoint único parametrizado, com injeção de falha por outage simulada e garantias de desfecho atômico ou inconsistente conforme o cenário.
- `experiment-report`: Relatório pós-execução que inspeciona banco, outbox e Kafka, compara o observado com o previsto por cenário e emite veredito classificado.
- `observability`: Métricas Micrometer por cenário, scrape Prometheus e dashboards Grafana provisionados como código.
- `frontend`: Interface Angular para selecionar cenários, executar experimentos, exibir vereditos e histórico, com UX/UI aplicadas.
- `lab-infrastructure`: Ambiente Docker Compose completo (Postgres, Kafka KRaft, backend, frontend, Prometheus, Grafana) com healthchecks e versões compatíveis com Java 21 LTS.

### Modified Capabilities
(nenhuma — repositório greenfield, sem specs existentes)

## Impact

- **Novos diretórios**: `main/src/back/` (Spring Boot), `main/src/front/` (Angular), infra de compose e provisioning na raiz de `main/` (ou raiz do repo, a definir em design).
- **APIs novas**: endpoint de execução de experimento (POST parametrizado) e endpoint de relatório/veredito.
- **Banco**: tabelas `orders` e `outbox_events` (Postgres).
- **Kafka**: tópico de eventos de pedido.
- **Portas/serviços**: Postgres 5432, Kafka 9092, backend 8080, frontend 4200, Prometheus 9090, Grafana 3000.
- Sem impacto em código existente (repositório vazio além de docs/openspec).
