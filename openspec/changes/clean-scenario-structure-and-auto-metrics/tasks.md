# Tasks: clean-scenario-structure-and-auto-metrics

## 1. Reestruturação de pacotes (sem mudança de comportamento)

- [x] 1.1 Mover `ScenarioPort`, `ScenarioId`, `ScenarioContext`, `ScenarioRegistry` e `ScenarioPayloadFactory` para `scenario/shared/` e corrigir imports; verificar com `mvn -q compile`
- [x] 1.2 Mover `OrderPersistence` para `order/` com nome que expresse o propósito de delimitar a transação antes da injeção de falha; verificar compilação e testes existentes (`mvn -q test`)
- [x] 1.3 Revisar nomes de classes, métodos e variáveis nos pacotes afetados visando clareza para o vídeo; verificar `mvn -q test` verde após o rename

## 2. Infraestrutura de instrumentação

- [x] 2.1 Criar o holder de contexto de cenário (`CurrentScenario`) em `metrics/` com `set`/`clear`/`current` e um teste unitário que verifica definição, leitura e limpeza na mesma thread
- [x] 2.2 Criar o decorator de instrumentação de cenário em `metrics/` que define o contexto, inicia/para o timer em volta do proxy transacional e limpa no `finally`; testar que dois experimentos consecutivos não vazam contexto entre execuções
- [x] 2.3 Envolver cada `ScenarioPort` no decorator dentro do `ScenarioRegistry`; testar que todos os cenários registrados retornam o cenário instrumentado

## 3. Counters nos decorators das portas

- [x] 3.1 Criar decorator de instrumentação de `OrderRepository` em `metrics/` que registra `dualwrite.orders.total` com a tag do contexto corrente; testar que o counter incrementa após um save com contexto definido
- [x] 3.2 Criar decorator de instrumentação de `EventPublisher` que registra `dualwrite.events.published.total` e o timer de publish do Kafka com a tag do contexto corrente; testar publicação com e sem contexto de cenário
- [x] 3.3 Criar decorator de instrumentação de `OutboxRepository` que registra `dualwrite.outbox.rows.total` com a tag do contexto corrente; testar o counter após save de evento de outbox
- [x] 3.4 Mapear pontos de falha para métricas de falha na instrumentação da injeção de falha (`FAIL_BEFORE_COMMIT`/`FAIL_DB_AFTER_PUBLISH` → falha de banco; `FAIL_PUBLISH_AFTER_COMMIT` → falha de publish); testar que executar o cenário C4 incrementa `dualwrite.failures.db.total` daquele cenário

## 4. Limpeza dos cenários

- [x] 4.1 Remover dos 6 cenários todos os timers, counters e blocos `try/finally`/`catch` cujo único propósito seja métrica, reduzindo o corpo ao fluxo de negócio; verificar por inspeção que nenhum cenário referencia `DualWriteMetrics` e rodar `mvn -q test`
- [x] 4.2 Colapsar a sobrecarga `publish(event, scenario)` do `KafkaEventPublisher` para `publish(event)`, removendo o branch de cenário nulo (o timer passa a ler o contexto corrente); testar que o publicador não referencia `ScenarioId`

## 5. Métrica de duração e dashboard

- [x] 5.1 Renomear a métrica de duração para `dualwrite.scenario.duration` (Micrometer: `dualwrite.scenario.duration`) tagueada por cenário, medindo abertura → commit; testar que a métrica exposta inclui o commit de um cenário transacional
- [x] 5.2 Atualizar as queries do dashboard Grafana em `main/infra/grafana/provisioning/dashboards/json/dual-write-lab.json` que referenciam `dualwrite_db_write_latency_seconds`; verificar que o JSON permanece válido (parser/parse manual) e nenhuma query referencia o nome antigo

## 6. Verificação de ponta a ponta

- [x] 6.1 Subir o ambiente Docker Compose, executar cada cenário (C1–C6) via API e verificar no scrape do `/actuator/prometheus`: counters por cenário, falha de banco registrada para C4, timer de duração com o novo nome e timer de publish tagueado
- [x] 6.2 Rodar `mvn -q test` completo e validar o delta spec com `openspec validate --change clean-scenario-structure-and-auto-metrics`
