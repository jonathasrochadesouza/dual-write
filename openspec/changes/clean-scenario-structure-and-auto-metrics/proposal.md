# Proposal: clean-scenario-structure-and-auto-metrics

## Why

O código dos cenários está contaminado por instrumentação: cada um dos 6 cenários repete o mesmo padrão de `startTransactionTimer`/`try`/`finally`/`stopTransactionTimer` e intercalada chamadas de counters entre as operações de negócio, escondendo o fluxo que é o próprio objeto didático do lab. Além disso, o pacote `scenario` mistura 12 arquivos de papéis diferentes (cenários, contrato, registro, factory, persistência), dificultando a leitura — e o projeto será apresentado em vídeo, onde clareza de estrutura e de nomes é requisito.

## What Changes

- **Métricas injetadas por decorators**: um decorator de instrumentação envolve cada cenário registrado no `ScenarioRegistry`, medindo a duração completa da transação (abertura → commit) sem nenhuma linha de métrica dentro dos cenários.
- **Tag de cenário via ThreadLocal**: um holder (`CurrentScenario`) define o cenário corrente durante a execução; os pontos de instrumentação leem a tag dali, eliminando a propagação manual de `ScenarioId` (e a sobrecarga `publish(event, scenario)` com branch de null no `KafkaEventPublisher`).
- **Counters de sucesso nos decorators das portas**: `OrderRepository`, `EventPublisher` e `OutboxRepository` ganham decorators de instrumentação em `metrics/` que registram `orders.total`, `events.published.total` e `outbox.rows.total`.
- **Counters de falha mapeados por FaultPoint**: a injeção de falha passa a registrar a métrica de falha correspondente ao ponto de falha. **Isso fecha uma brecha atual**: o cenário C4 (`FAIL_DB_AFTER_PUBLISH`) lança a falha mas não incrementa `dualwrite.failures.db.total`.
- **Timer com semântica honesta e renomeado**: `dualwrite.db.write.latency` passa a medir a duração do cenário incluindo o commit da transação e é renomeado (ex.: `dualwrite.scenario.duration`); as queries do dashboard Grafana são atualizadas.
- **Estrutura de pacotes**: `scenario/` contém apenas os 6 cenários; infraestrutura compartilhada (`ScenarioPort`, `ScenarioId`, `ScenarioContext`, `ScenarioRegistry`, `ScenarioPayloadFactory`) migra para `scenario/shared/`.
- **`OrderPersistence` muda de casa e de nome**: é persistência do domínio `order` que delimita a transação antes da injeção de falha — move para `order/` com nome que expresse esse propósito.
- Nomes de classes, métodos e variáveis revisados para clareza em vídeo.

## Capabilities

### New Capabilities

(nenhuma)

### Modified Capabilities

- `observability`: a métrica de latência passa a incluir o commit da transação e é renomeada; falhas de escrita de banco do cenário C4 passam a ser contadas em `dualwrite.failures.db.total`; o timer de publish do Kafka passa a ser sempre registrado com tag de cenário durante execuções de cenário.

## Impact

- **Código**: pacotes `scenario`, `metrics`, `order`, `kafka`, `fault` (backend Spring Boot); nenhum cenário novo ou removido; vereditos e relatórios inalterados.
- **Métricas/Dashboard**: rename de `dualwrite.db_write_latency_seconds` → novo nome (2 queries no JSON do dashboard Grafana em `main/infra/grafana/`); novo incremento de counter para C4.
- **APIs**: nenhuma mudança de endpoint ou contrato externo.
- **Riscos**: ThreadLocal exige execução síncrona (válida hoje: JDBC + `kafkaTemplate.send().get()`); o decorator deve limpar o holder no `finally` para evitar vazamento entre execuções.
