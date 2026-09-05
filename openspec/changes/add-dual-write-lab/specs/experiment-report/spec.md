# Spec Delta: experiment-report

## Purpose

Fornece um relatório pós-execução que inspeciona o estado real do banco (pedidos e outbox) e do tópico Kafka, compara o observado com o resultado previsto de cada cenário e emite um veredito classificado, transformando cada experimento em evidência testável do dual-write problem.

## ADDED Requirements

### Requirement: Inspeção do estado observado
O sistema SHALL, para um experimento executado, inspecionar e reportar: a quantidade e o conteúdo das linhas da tabela de pedidos relacionadas ao experimento, as linhas da tabela de outbox relacionadas ao experimento e as mensagens publicadas no tópico Kafka relacionadas ao experimento.

#### Scenario: Consulta do relatório de um experimento
- **WHEN** o cliente consulta o relatório de um experimento existente
- **THEN** o sistema retorna o estado observado de banco, outbox e Kafka para aquele experimento

#### Scenario: Consulta de experimento inexistente
- **WHEN** o cliente consulta o relatório de um experimento que não existe
- **THEN** o sistema retorna erro indicando que o experimento não foi encontrado

### Requirement: Resultado previsto por cenário
O relatório SHALL incluir, para cada cenário, o resultado previsto — o estado esperado de banco, outbox e Kafka conforme a semântica do cenário — de modo que a comparação entre previsto e observado seja explícita. Para os cenários outbox, o Kafka vazio SHALL ser marcado como esperado (message relay fora de escopo), não como falha.

#### Scenario: Relatório de cenário outbox com commit
- **WHEN** o relatório de um experimento `OUTBOX_COMMIT` bem-sucedido é consultado
- **THEN** o resultado previsto exibe pedido gravado, evento pendente no outbox e tópico Kafka vazio como estado esperado

### Requirement: Veredito classificado
O relatório SHALL emitir um veredito classificado a partir da comparação entre previsto e observado, com ao menos estas classificações: `ATOMICO` (desfecho tudo-ou-nada garantido), `INCONSISTENTE` (banco e Kafka divergem — pedido ou evento fantasma) e `OPERACAO_PERDIDA` (nenhum registro em lugar nenhum, operação perdida silenciosamente).

#### Scenario: Veredito de cenário C3
- **WHEN** o relatório de um experimento `DUAL_WRITE_DB_ONLY` é consultado após execução bem-sucedida do cenário
- **THEN** o veredito é `INCONSISTENTE` com indicação de pedido fantasma no banco

#### Scenario: Veredito de cenário C2
- **WHEN** o relatório de um experimento `DUAL_WRITE_NONE` é consultado após execução bem-sucedida do cenário
- **THEN** o veredito é `OPERACAO_PERDIDA`, pois nada foi gravado em banco ou Kafka

#### Scenario: Veredito de cenário C5
- **WHEN** o relatório de um experimento `OUTBOX_COMMIT` é consultado após execução bem-sucedida do cenário
- **THEN** o veredito é `ATOMICO`, pois as duas tabelas foram commitadas na mesma transação

### Requirement: Rastreabilidade do experimento
Cada execução SHALL gerar um registro de experimento com identificador único, cenário executado, marcação temporal e veredito, consultável posteriormente para compor o histórico de execuções.

#### Scenario: Histórico de experimentos
- **WHEN** o cliente consulta o histórico de experimentos
- **THEN** o sistema retorna os experimentos executados com identificador, cenário, data/hora e veredito
