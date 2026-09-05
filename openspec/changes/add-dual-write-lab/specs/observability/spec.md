# Spec Delta: observability

## Purpose

Expõe métricas por cenário (Micrometer/Prometheus) e dashboards Grafana provisionados como código, tornando a divergência banco × Kafka — o próprio dual-write problem — visível em tempo real.

## ADDED Requirements

### Requirement: Métricas de escrita por cenário
O sistema SHALL instrumentar contadores de escrita/publicação por cenário, cobrindo ao menos: escritas no banco (pedidos), publicações no Kafka, falhas de publish, linhas de outbox commitadas e rollbacks de transação, todos identificáveis pelo cenário que os originou.

#### Scenario: Contadores após execução de cenário com falha de publish
- **WHEN** um cenário com falha injetada no publish é executado
- **THEN** as métricas expostas registram a escrita no banco e a falha de publish atribuídas àquele cenário

### Requirement: Métrica de veredito de experimento
O sistema SHALL expor contadores de experimentos executados por cenário e por veredito, permitindo observar a proporção de desfechos atômicos, inconsistentes e perdidos.

#### Scenario: Contador de veredito após experimento inconsistente
- **WHEN** um experimento com veredito `INCONSISTENTE` é concluído
- **THEN** o contador de experimentos por veredito incrementa a entrada correspondente ao cenário e ao veredito

### Requirement: Exposição de métricas no formato Prometheus
O sistema SHALL expor as métricas em um endpoint no formato Prometheus, passível de scrape pelo Prometheus configurado no ambiente.

#### Scenario: Scrape das métricas
- **WHEN** o Prometheus executa o scrape do endpoint de métricas do backend
- **THEN** as métricas do laboratório estão presentes na resposta no formato Prometheus

### Requirement: Dashboard Grafana provisionado como código
O ambiente SHALL provisionar automaticamente, sem configuração manual, ao menos um dashboard Grafana dedicado ao laboratório, contendo: painel de lacuna de consistência (escritas no banco vs publicações no Kafka por cenário), contadores de experimentos por veredito e latência das transações de banco.

#### Scenario: Dashboard disponível após subida do ambiente
- **WHEN** o ambiente Docker Compose sobe completamente
- **THEN** o dashboard do laboratório está disponível no Grafana sem nenhuma etapa manual de importação

#### Scenario: Lacuna de consistência visível após cenário C3
- **WHEN** o cenário `DUAL_WRITE_DB_ONLY` é executado e o painel de lacuna de consistência é observado
- **THEN** o painel exibe divergência entre a série de escritas no banco e a série de publicações no Kafka para aquele cenário
