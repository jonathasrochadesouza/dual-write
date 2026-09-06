# Spec Delta: observability

## MODIFIED Requirements

### Requirement: Métricas de escrita por cenário
O sistema SHALL instrumentar contadores de escrita/publicação por cenário, cobrindo ao menos: escritas no banco (pedidos), publicações no Kafka, falhas de publish, falhas de escrita no banco, linhas de outbox commitadas e rollbacks de transação, todos identificáveis pelo cenário que os originou. A instrumentação SHALL ser aplicada sem exigir chamadas de métrica no corpo dos cenários.

#### Scenario: Contadores após execução de cenário com falha de publish
- **WHEN** um cenário com falha injetada no publish é executado
- **THEN** as métricas expostas registram a escrita no banco e a falha de publish atribuídas àquele cenário

#### Scenario: Contador de falha de escrita no banco após cenário C4
- **WHEN** o cenário `DUAL_WRITE_KAFKA_ONLY` é executado com falha injetada na escrita do banco após o publish
- **THEN** a métrica de falhas de escrita no banco incrementa a entrada atribuída àquele cenário

#### Scenario: Cenários sem chamadas de métrica no corpo
- **WHEN** o corpo do método de execução de qualquer cenário é inspecionado
- **THEN** ele não contém chamadas de métrica, timers ou tratamento de erro cujo único propósito seja instrumentação

## ADDED Requirements

### Requirement: Duração de cenário incluindo commit da transação
O sistema SHALL medir a duração da execução de cada cenário abrangendo o ciclo de vida completo da transação, incluindo o tempo de commit, e SHALL expor essa duração como métrica de timer identificada pelo cenário. Em execuções de cenário, o timer de publicação no Kafka SHALL ser registrado com a tag do cenário corrente, derivado de um contexto de cenário propagado durante a execução.

#### Scenario: Duração inclui o commit
- **WHEN** um cenário transacional é executado até o commit
- **THEN** a métrica de duração exposta inclui o tempo de commit da transação e carrega a tag do cenário

#### Scenario: Timer de publish sempre tagueado em cenário
- **WHEN** um cenário publica um evento no Kafka durante sua execução
- **THEN** o timer de publicação é registrado com a tag do cenário corrente, sem o cenário precisar propagar seu identificador para o publicador
