# Design: clean-scenario-structure-and-auto-metrics

## Context

Hoje os 6 cenários em `scenario/` repetem o mesmo bloco de instrumentação (`startTransactionTimer`/`try`/`finally`/`stopTransactionTimer`) e intercalam chamadas de counters (`recordDbWrite`, `recordKafkaPublish`, etc.) no fluxo de negócio. O pacote `scenario` também mistura 12 arquivos de papéis diferentes. Ver `proposal.md` para a motivação; ver `specs/observability/spec.md` (delta) para os requisitos de comportamento.

Fatos do código atual que fundamentam as decisões:

- O `ScenarioRegistry` recebe do Spring os beans `ScenarioPort` já proxyfados (`@Transactional` vive no proxy).
- Os counters de falha (`recordDbRollback`, `recordKafkaPublishFailure`) só são chamados em `catch` que re-lança, e a exceção que os dispara é sempre a `SimulatedOutageException` do `FaultInjector`, que carrega o `FaultPoint`.
- Todo o fluxo é síncrono na mesma thread (JDBC + `kafkaTemplate.send().get()`), o que torna ThreadLocal seguro.
- O timer atual para antes do commit (roda dentro do método transacional), então o commit nunca foi medido.

## Goals / Non-Goals

**Goals**

- Corpo de cada cenário reduzido ao fluxo de negócio puro (create → save → publish), sem try/finally/catch cujo único propósito seja métrica.
- Duração medida abrangendo o commit da transação.
- Tag de cenário derivada de contexto propagado, sem o cenário propagar identificadores aos colaboradores.
- Estrutura de pacotes com `scenario/` contendo apenas cenários e `scenario/shared/` a infraestrutura compartilhada.

**Non-Goals**

- Não mudar endpoints, vereditos, relatórios nem o comportamento dos cenários (C1–C6).
- Não adotar AOP (`@Aspect`), Micrometer `@Timed` ou a API `Observation` — decorators explícitos foram escolhidos.
- Não renomear `ScenarioPort` (decisão explícita: manter).
- Não propagar contexto entre threads ou suportar execução assíncrona.

## Decisions

### D1 — Decorator de instrumentação no ponto de registro

`ScenarioRegistry` envolve cada `ScenarioPort` (o proxy transacional) num decorator de instrumentação no momento do registro. O timer inicia antes da chamada ao proxy e termina após o retorno — como o commit acontece dentro do proxy, antes do retorno, a duração measure o ciclo de vida completo da transação (abertura → commit), atendendo ao requisito de duração sem nenhuma linha de métrica no cenário.

*Alternativas*: aspect Spring com `@Order` interno ao interceptor de transação (funciona, mas é "mágica" difícil de narrar); timer fora do `ExperimentRunner` (medição perderia a fronteira do proxy em favor de código ainda mais distante do domínio).

### D2 — Contexto de cenário via ThreadLocal

Um holder estático (`CurrentScenario`, em `metrics/`) guarda o `ScenarioId` corrente. O decorator define o valor antes de delegar e o limpa no `finally`. Os pontos de instrumentação (decorators das portas e da injeção de falha) leem a tag dali. Isso permite colapsar a sobrecarga `publish(event, scenario)` do `KafkaEventPublisher` para `publish(event)`, eliminando o branch de cenário nulo.

*Alternativas*: `Observation`/context-propagation do Micrometer (correto, porém pesado demais para o lab); aceitar counters sem tag de cenário (quebraria os painéis por cenário do dashboard).

### D3 — Counters de sucesso em decorators das portas, contando só o que commitou

`OrderRepository`, `EventPublisher` e `OutboxRepository` são envolvidos por decorators de instrumentação que registram `dualwrite.orders.total`, `dualwrite.events.published.total` e `dualwrite.outbox.rows.total`, tagueando pelo contexto corrente. Os decorators ficam em `metrics/` com nomes que expressam o papel (instrumentação de métrica daquela porta), mantendo os adaptadores JDBC/Kafka limpos.

**Contagem apenas de escritas commitadas** (decisão durante a implementação): contar no momento do `save()` faria cenários de rollback (C2, C6) aparecerem no painel de lacuna de consistência com escritas fantasma. Os decorators de escrita registram o counter num callback `afterCommit` quando há transação ativa (`TransactionSynchronizationManager`) e gravam imediatamente quando não há (autocommit, como em C1 e C3). O counter de publicação no Kafka permanece imediato — o publish é síncrono com ack do broker, sem semântica de transação. Os counters de falha também são imediatos: a falha antecede o commit e o rollback é certo.

*Alternativas*: injetar `DualWriteMetrics` direto nos adaptadores Jdbc/Kafka (menos classes, mas reintroduz linhas de métrica em código de infraestrutura); manter counters nos cenários (o problema que se quer eliminar); counting ingênuo no `save()` (métrica fantasma em C2/C6).

### D4 — Counters de falha mapeados por FaultPoint

A injeção de falha passa a registrar a métrica de falha correspondente ao ponto: `FAIL_BEFORE_COMMIT` → `dualwrite.failures.db.total`; `FAIL_PUBLISH_AFTER_COMMIT` → `dualwrite.failures.kafka.total`; `FAIL_DB_AFTER_PUBLISH` → `dualwrite.failures.db.total`. Como os counters atuais só reagem a `SimulatedOutageException`, o mapeamento é uma tradução direta — e fecha a brecha em que C4 (`FAIL_DB_AFTER_PUBLISH`) não incrementava contador de falha.

*Alternativas*: decorator genérico que captura exceção e infere a falha (não distingue qual escrita falhou; o `FaultPoint` já sabe).

### D5 — Estrutura de pacotes

```
scenario/            ← apenas os 6 cenários
scenario/shared/     ← ScenarioPort, ScenarioId, ScenarioContext, ScenarioRegistry, ScenarioPayloadFactory
order/               ← persistência transacional do domínio order (ex-OrderPersistence, renomeada)
metrics/             ← DualWriteMetrics, CurrentScenario, decorators de instrumentação
fault/  kafka/  outbox/  report/  experiment/  api/  (inalterados, exceto wiring)
```

### D6 — Rename da métrica de duração

`dualwrite.db.write.latency` mede o cenário inteiro (banco + publish + commit); o nome mente. Renomear para `dualwrite.scenario.duration` e atualizar as 2 queries do dashboard Grafana que referenciam `dualwrite_db_write_latency_seconds`.

## Risks / Trade-offs

- [ThreadLocal vaza entre execuções se não for limpo] → decorator limpa no `finally`; teste de regressão executando dois experimentos consecutivos na mesma thread.
- [Decorator no registry esconde o bean real ao depurar] → nomes explícitos e um teste que afirma que a instrumentação envolve todos os cenários registrados.
- [Quebra do dashboard pelo rename da métrica] → atualizar o JSON do dashboard na mesma change; validar com scrape manual do `/actuator/prometheus`.
- [Instrumentação via decorator duplica a semântica dos counters existentes] → `DualWriteMetrics` permanece a única fachada de métrica; decorators apenas orquestram chamadas a ela.

## Migration Plan

Refatoração interna sem mudança de contrato de API. Ordem: (1) mover/reorganizar pacotes sem mudança de comportamento e testar; (2) introduzir decorators e ThreadLocal, removendo as chamadas de métrica dos cenários; (3) renomear a métrica de duração e atualizar o dashboard; (4) rodar a suíte de testes e validar o scrape do Prometheus entre cada etapa. Rollback: revert do commit; nenhuma migração de dados envolvida.
