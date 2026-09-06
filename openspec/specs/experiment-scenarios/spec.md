# experiment-scenarios Specification

# Spec Delta: experiment-scenarios

## Purpose

Permite executar os seis cenários experimentais do dual-write problem (quatro variações incorretas de dual-write e duas variações corretas com Transactional Outbox) para observar, na prática, quando o banco e o Kafka divergem e quando a atomicidade é garantida.

## Requirements

### Requirement: Execução de cenário por endpoint único parametrizado
O sistema SHALL expor um único endpoint de execução de experimento que recebe o identificador do cenário como parâmetro e SHALL aceitar exclusivamente os seis cenários suportados: `DUAL_WRITE_BOTH_OK`, `DUAL_WRITE_NONE`, `DUAL_WRITE_DB_ONLY`, `DUAL_WRITE_KAFKA_ONLY`, `OUTBOX_COMMIT`, `OUTBOX_ROLLBACK`.

#### Scenario: Execução com cenário válido
- **WHEN** o cliente invoca o endpoint de execução informando um dos seis identificadores de cenário suportados
- **THEN** o sistema executa o cenário correspondente e retorna o identificador do experimento gerado

#### Scenario: Execução com cenário inválido
- **WHEN** o cliente invoca o endpoint informando um identificador que não corresponde a nenhum dos seis cenários
- **THEN** o sistema rejeita a requisição com erro de validação e não executa nenhuma escrita no banco nem no Kafka

### Requirement: Cenário C1 — dual-write com ambos gravados
No cenário `DUAL_WRITE_BOTH_OK`, o sistema SHALL gravar o pedido na tabela de pedidos e publicar o evento de pedido no tópico Kafka, como duas operações independentes, sem garantia de atomicidade entre elas.

#### Scenario: Execução sem falha
- **WHEN** o cenário `DUAL_WRITE_BOTH_OK` é executado sem nenhuma falha injetada
- **THEN** o pedido existe na tabela de pedidos e o evento existe no tópico Kafka

### Requirement: Cenário C2 — operação perdida
No cenário `DUAL_WRITE_NONE`, o sistema SHALL simular falha de serviço que provoca rollback completo: nenhum pedido gravado no banco e nenhum evento publicado no Kafka.

#### Scenario: Execução com falha antes do commit
- **WHEN** o cenário `DUAL_WRITE_NONE` é executado com a falha injetada antes da efetivação das escritas
- **THEN** nenhuma linha existe na tabela de pedidos e nenhuma mensagem existe no tópico Kafka

### Requirement: Cenário C3 — pedido fantasma no banco
No cenário `DUAL_WRITE_DB_ONLY`, o sistema SHALL simular queda do serviço de publish após o commit do banco, de modo que o pedido fique gravado no banco e o evento não seja publicado no Kafka.

#### Scenario: Execução com queda do publish após o commit
- **WHEN** o cenário `DUAL_WRITE_DB_ONLY` é executado com a falha injetada no publish após a confirmação da transação do banco
- **THEN** o pedido existe na tabela de pedidos e nenhuma mensagem existe no tópico Kafka

### Requirement: Cenário C4 — evento fantasma no Kafka
No cenário `DUAL_WRITE_KAFKA_ONLY`, o sistema SHALL publicar o evento no Kafka antes da gravação no banco e simular falha de serviço na escrita do banco, de modo que o evento exista no Kafka e o pedido não exista no banco.

#### Scenario: Execução com falha na escrita do banco
- **WHEN** o cenário `DUAL_WRITE_KAFKA_ONLY` é executado com a falha injetada na escrita do banco após a publicação do evento
- **THEN** o evento existe no tópico Kafka e nenhuma linha existe na tabela de pedidos

### Requirement: Cenário C5 — outbox com commit atômico
No cenário `OUTBOX_COMMIT`, o sistema SHALL gravar o pedido e o evento de outbox na mesma transação de banco, garantindo que ambas as tabelas sejam commitadas juntas. O evento NÃO SHALL ser publicado no Kafka neste cenário (message relay fora de escopo).

#### Scenario: Execução sem falha
- **WHEN** o cenário `OUTBOX_COMMIT` é executado sem nenhuma falha injetada
- **THEN** o pedido existe na tabela de pedidos e o evento pendente existe na tabela de outbox, ambos gravados na mesma transação, e nenhuma mensagem é publicada no tópico Kafka

### Requirement: Cenário C6 — outbox com rollback atômico
No cenário `OUTBOX_ROLLBACK`, o sistema SHALL simular falha após a gravação do pedido e do evento de outbox, ainda dentro da mesma transação, garantindo rollback atômico de ambas as tabelas.

#### Scenario: Execução com falha dentro da transação
- **WHEN** o cenário `OUTBOX_ROLLBACK` é executado com a falha injetada após ambos os registros e antes do commit
- **THEN** nenhuma linha existe na tabela de pedidos e nenhuma linha existe na tabela de outbox

### Requirement: Falha sempre como ausência por outage
A injeção de falha dos cenários SHALL representar exclusivamente ausência de escrita/publicação por queda de serviço simulada. O sistema NÃO SHALL produzir payloads corrompidos, eventos falsos ou dados deliberadamente errados.

#### Scenario: Falha injetada em qualquer cenário
- **WHEN** um cenário com falha injetada é executado
- **THEN** o efeito observável é apenas a ausência da escrita no recurso afetado (banco ou Kafka), sem alteração do conteúdo dos dados gravados
